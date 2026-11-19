package com.example.ui.viewmodel

import android.app.Application
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BuiltInServers
import com.example.data.local.MegaVpnDatabase
import com.example.data.local.ServerRepository
import com.example.data.model.ConnectionState
import com.example.data.model.CountryTemplate
import com.example.data.model.DnsPreset
import com.example.data.model.InstalledAppInfo
import com.example.data.model.LogLevel
import com.example.data.model.ServerRegion
import com.example.data.model.UdpProtocolMode
import com.example.data.model.UdpSettings
import com.example.data.model.VpnServer
import com.example.data.model.VpnTelemetry
import com.example.data.repository.SettingsRepository
import com.example.domain.ServerGenerator
import com.example.vpn.MegaUdpVpnService
import com.example.vpn.NetworkDiagnostics
import com.example.vpn.VpnStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ServersUiState(
    val servers: List<VpnServer> = BuiltInServers.defaultServers,
    val selectedRegion: ServerRegion = ServerRegion.ALL,
    val searchQuery: String = "",
    val isPingingAll: Boolean = false
)

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val serverRepo = ServerRepository(
        MegaVpnDatabase.getInstance(application).customServerDao()
    )

    val telemetry: StateFlow<VpnTelemetry> = VpnStateHolder.telemetry

    val settings: StateFlow<UdpSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UdpSettings())

    private val _measuredPings = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _selectedRegion = MutableStateFlow(ServerRegion.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _isPingingAll = MutableStateFlow(false)

    val allServers: StateFlow<List<VpnServer>> = combine(
        serverRepo.generatedServers,
        _measuredPings
    ) { customList, pingMap ->
        val combined = customList + BuiltInServers.defaultServers
        combined.map { server ->
            val updatedPing = pingMap[server.id]
            if (updatedPing != null) server.copy(pingMs = updatedPing) else server
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BuiltInServers.defaultServers)

    val serversUiState: StateFlow<ServersUiState> = combine(
        allServers,
        _selectedRegion,
        _searchQuery,
        _isPingingAll
    ) { list, region, query, pinging ->
        val filtered = list.filter { server ->
            val matchesRegion = when (region) {
                ServerRegion.ALL -> true
                ServerRegion.GENERATED -> server.isCustomGenerated
                else -> server.region == region
            }
            val q = query.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                server.countryName.lowercase().contains(q) ||
                server.city.lowercase().contains(q) ||
                server.name.lowercase().contains(q) ||
                server.host.lowercase().contains(q)
            matchesRegion && matchesQuery
        }
        ServersUiState(
            servers = filtered,
            selectedRegion = region,
            searchQuery = query,
            isPingingAll = pinging
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServersUiState())

    val selectedServer: StateFlow<VpnServer> = combine(
        allServers,
        settings
    ) { list, prefs ->
        list.firstOrNull { it.id == prefs.selectedServerId } ?: list.first()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BuiltInServers.defaultServers.first()
    )

    private val _lastGeneratedServer = MutableStateFlow<VpnServer?>(null)
    val lastGeneratedServer: StateFlow<VpnServer?> = _lastGeneratedServer.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _appsLoading = MutableStateFlow(false)
    val appsLoading: StateFlow<Boolean> = _appsLoading.asStateFlow()

    init {
        if (VpnStateHolder.telemetry.value.logs.isEmpty()) {
            VpnStateHolder.addLog(
                LogLevel.INFO,
                "DJ TRUST MEGA UDP Engine ready. Select a country node or generate a custom UDP server."
            )
        }
    }

    fun prepareVpnIntent(): android.content.Intent? =
        VpnService.prepare(getApplication())

    fun connectSelectedServer() {
        val server = selectedServer.value
        MegaUdpVpnService.startTunnel(getApplication(), server.id)
    }

    fun disconnectTunnel() {
        MegaUdpVpnService.stopTunnel(getApplication())
    }

    fun selectServer(server: VpnServer, reconnectIfActive: Boolean = true) {
        viewModelScope.launch {
            settingsRepo.setSelectedServerId(server.id)
            VpnStateHolder.addLog(
                LogLevel.INFO,
                "Selected ${server.flagEmoji} ${server.countryName} (${server.protocolMode.shortCode}:${server.port})"
            )
            val state = telemetry.value.connectionState
            if (reconnectIfActive && (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING)) {
                MegaUdpVpnService.startTunnel(getApplication(), server.id)
            }
        }
    }

    fun setRegionFilter(region: ServerRegion) {
        _selectedRegion.value = region
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun pingAllServers() {
        if (_isPingingAll.value) return
        viewModelScope.launch {
            _isPingingAll.value = true
            VpnStateHolder.addLog(LogLevel.INFO, "Probing UDP node latencies across ${allServers.value.size} endpoints...")
            val results = NetworkDiagnostics.measureServerLatencies(allServers.value)
            _measuredPings.value = results
            _isPingingAll.value = false
            VpnStateHolder.addLog(LogLevel.SUCCESS, "Latency scan complete (${results.size} nodes updated).")
        }
    }

    fun generateAndSaveServer(
        country: CountryTemplate,
        protocol: UdpProtocolMode,
        customPort: Int?,
        mtu: Int,
        customName: String?,
        sniOrBugHost: String
    ): VpnServer {
        val generated = ServerGenerator.generateUdpServer(
            country = country,
            protocol = protocol,
            customPort = customPort,
            mtu = mtu,
            customName = customName,
            sniOrBugHost = sniOrBugHost
        )
        _lastGeneratedServer.value = generated
        viewModelScope.launch {
            serverRepo.saveGeneratedServer(generated)
            settingsRepo.setSelectedServerId(generated.id)
            VpnStateHolder.addLog(
                LogLevel.SUCCESS,
                "Generated UDP server: ${generated.flagEmoji} ${generated.host}:${generated.port} (${generated.protocolMode.shortCode})"
            )
        }
        return generated
    }

    fun deleteGeneratedServer(serverId: String) {
        viewModelScope.launch {
            serverRepo.deleteGeneratedServer(serverId)
            if (settings.value.selectedServerId == serverId) {
                settingsRepo.setSelectedServerId(BuiltInServers.defaultServers.first().id)
            }
            if (_lastGeneratedServer.value?.id == serverId) {
                _lastGeneratedServer.value = null
            }
        }
    }

    fun updateSettings(transform: (UdpSettings) -> UdpSettings) {
        viewModelScope.launch {
            settingsRepo.updateSettings(transform)
        }
    }

    fun setDnsPreset(preset: DnsPreset) {
        updateSettings { it.copy(dnsPreset = preset) }
    }

    fun loadInstalledAppsIfNeeded() {
        if (_installedApps.value.isNotEmpty() || _appsLoading.value) return
        viewModelScope.launch {
            _appsLoading.value = true
            _installedApps.value = NetworkDiagnostics.loadLaunchableApps(getApplication())
            _appsLoading.value = false
        }
    }

    fun toggleSplitTunnelApp(packageName: String) {
        updateSettings { current ->
            val next = current.splitTunnelDisallowedPackages.toMutableSet()
            if (next.contains(packageName)) next.remove(packageName) else next.add(packageName)
            current.copy(splitTunnelDisallowedPackages = next)
        }
    }

    fun clearLogs() {
        VpnStateHolder.clearLogs()
    }
}

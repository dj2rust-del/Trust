package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DnsPreset
import com.example.data.model.UdpProtocolMode
import com.example.data.model.UdpSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.vpnSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dj_trust_mega_udp_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SELECTED_SERVER_ID = stringPreferencesKey("selected_server_id")
        val PROTOCOL_OVERRIDE = stringPreferencesKey("protocol_override")
        val CUSTOM_UDP_PORT = intPreferencesKey("custom_udp_port")
        val MTU_SIZE = intPreferencesKey("mtu_size")
        val UDP_BUFFER_KB = intPreferencesKey("udp_buffer_kb")
        val KEEP_ALIVE_SECONDS = intPreferencesKey("keep_alive_seconds")
        val BADVPN_ENABLED = booleanPreferencesKey("badvpn_enabled")
        val BADVPN_PORT = intPreferencesKey("badvpn_port")
        val UDP_OBFUSCATION_ENABLED = booleanPreferencesKey("udp_obfuscation_enabled")
        val OBFUSCATION_PAYLOAD = stringPreferencesKey("obfuscation_payload")
        val KILL_SWITCH_ENABLED = booleanPreferencesKey("kill_switch_enabled")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val DNS_LEAK_PROTECTION = booleanPreferencesKey("dns_leak_protection")
        val BLOCK_IPV6_LEAKS = booleanPreferencesKey("block_ipv6_leaks")
        val LAN_BYPASS_ENABLED = booleanPreferencesKey("lan_bypass_enabled")
        val FULL_DEVICE_TUNNEL = booleanPreferencesKey("full_device_tunnel")
        val DNS_PRESET = stringPreferencesKey("dns_preset")
        val CUSTOM_DNS_PRIMARY = stringPreferencesKey("custom_dns_primary")
        val CUSTOM_DNS_SECONDARY = stringPreferencesKey("custom_dns_secondary")
        val SPLIT_TUNNELING_ENABLED = booleanPreferencesKey("split_tunneling_enabled")
        val SPLIT_TUNNEL_PACKAGES = stringSetPreferencesKey("split_tunnel_packages")
        val USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
    }

    val settingsFlow: Flow<UdpSettings> = context.vpnSettingsDataStore.data.map { prefs ->
        val default = UdpSettings()
        val protocolStr = prefs[Keys.PROTOCOL_OVERRIDE]
        val protocolOverride = if (protocolStr.isNullOrBlank()) null else {
            runCatching { UdpProtocolMode.valueOf(protocolStr) }.getOrNull()
        }
        val dnsStr = prefs[Keys.DNS_PRESET]
        val dnsPreset = if (dnsStr.isNullOrBlank()) default.dnsPreset else {
            runCatching { DnsPreset.valueOf(dnsStr) }.getOrDefault(default.dnsPreset)
        }

        UdpSettings(
            selectedServerId = prefs[Keys.SELECTED_SERVER_ID] ?: default.selectedServerId,
            protocolOverride = protocolOverride,
            customUdpPort = prefs[Keys.CUSTOM_UDP_PORT] ?: default.customUdpPort,
            mtuSize = prefs[Keys.MTU_SIZE] ?: default.mtuSize,
            udpBufferKb = prefs[Keys.UDP_BUFFER_KB] ?: default.udpBufferKb,
            keepAliveSeconds = prefs[Keys.KEEP_ALIVE_SECONDS] ?: default.keepAliveSeconds,
            badVpnEnabled = prefs[Keys.BADVPN_ENABLED] ?: default.badVpnEnabled,
            badVpnPort = prefs[Keys.BADVPN_PORT] ?: default.badVpnPort,
            udpObfuscationEnabled = prefs[Keys.UDP_OBFUSCATION_ENABLED] ?: default.udpObfuscationEnabled,
            obfuscationPayload = prefs[Keys.OBFUSCATION_PAYLOAD] ?: default.obfuscationPayload,
            killSwitchEnabled = prefs[Keys.KILL_SWITCH_ENABLED] ?: default.killSwitchEnabled,
            autoReconnect = prefs[Keys.AUTO_RECONNECT] ?: default.autoReconnect,
            dnsLeakProtection = prefs[Keys.DNS_LEAK_PROTECTION] ?: default.dnsLeakProtection,
            blockIpv6Leaks = prefs[Keys.BLOCK_IPV6_LEAKS] ?: default.blockIpv6Leaks,
            lanBypassEnabled = prefs[Keys.LAN_BYPASS_ENABLED] ?: default.lanBypassEnabled,
            fullDeviceTunnel = prefs[Keys.FULL_DEVICE_TUNNEL] ?: default.fullDeviceTunnel,
            dnsPreset = dnsPreset,
            customDnsPrimary = prefs[Keys.CUSTOM_DNS_PRIMARY] ?: default.customDnsPrimary,
            customDnsSecondary = prefs[Keys.CUSTOM_DNS_SECONDARY] ?: default.customDnsSecondary,
            splitTunnelingEnabled = prefs[Keys.SPLIT_TUNNELING_ENABLED] ?: default.splitTunnelingEnabled,
            splitTunnelDisallowedPackages = prefs[Keys.SPLIT_TUNNEL_PACKAGES] ?: default.splitTunnelDisallowedPackages,
            useDarkTheme = prefs[Keys.USE_DARK_THEME] ?: default.useDarkTheme
        )
    }

    suspend fun getSnapshot(): UdpSettings = settingsFlow.first()

    suspend fun setSelectedServerId(id: String) {
        context.vpnSettingsDataStore.edit { it[Keys.SELECTED_SERVER_ID] = id }
    }

    suspend fun updateSettings(transform: (UdpSettings) -> UdpSettings) {
        context.vpnSettingsDataStore.edit { prefs ->
            val current = settingsFlow.first()
            val updated = transform(current)
            prefs[Keys.SELECTED_SERVER_ID] = updated.selectedServerId
            prefs[Keys.PROTOCOL_OVERRIDE] = updated.protocolOverride?.name ?: ""
            prefs[Keys.CUSTOM_UDP_PORT] = updated.customUdpPort
            prefs[Keys.MTU_SIZE] = updated.mtuSize
            prefs[Keys.UDP_BUFFER_KB] = updated.udpBufferKb
            prefs[Keys.KEEP_ALIVE_SECONDS] = updated.keepAliveSeconds
            prefs[Keys.BADVPN_ENABLED] = updated.badVpnEnabled
            prefs[Keys.BADVPN_PORT] = updated.badVpnPort
            prefs[Keys.UDP_OBFUSCATION_ENABLED] = updated.udpObfuscationEnabled
            prefs[Keys.OBFUSCATION_PAYLOAD] = updated.obfuscationPayload
            prefs[Keys.KILL_SWITCH_ENABLED] = updated.killSwitchEnabled
            prefs[Keys.AUTO_RECONNECT] = updated.autoReconnect
            prefs[Keys.DNS_LEAK_PROTECTION] = updated.dnsLeakProtection
            prefs[Keys.BLOCK_IPV6_LEAKS] = updated.blockIpv6Leaks
            prefs[Keys.LAN_BYPASS_ENABLED] = updated.lanBypassEnabled
            prefs[Keys.FULL_DEVICE_TUNNEL] = updated.fullDeviceTunnel
            prefs[Keys.DNS_PRESET] = updated.dnsPreset.name
            prefs[Keys.CUSTOM_DNS_PRIMARY] = updated.customDnsPrimary
            prefs[Keys.CUSTOM_DNS_SECONDARY] = updated.customDnsSecondary
            prefs[Keys.SPLIT_TUNNELING_ENABLED] = updated.splitTunnelingEnabled
            prefs[Keys.SPLIT_TUNNEL_PACKAGES] = updated.splitTunnelDisallowedPackages
            prefs[Keys.USE_DARK_THEME] = updated.useDarkTheme
        }
    }
}

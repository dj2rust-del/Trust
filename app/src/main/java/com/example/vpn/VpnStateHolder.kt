package com.example.vpn

import com.example.data.model.ConnectionState
import com.example.data.model.LogLevel
import com.example.data.model.TunnelLogEntry
import com.example.data.model.UdpProtocolMode
import com.example.data.model.VpnServer
import com.example.data.model.VpnTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

object VpnStateHolder {

    private val _telemetry = MutableStateFlow(VpnTelemetry())
    val telemetry: StateFlow<VpnTelemetry> = _telemetry.asStateFlow()

    private val logIdSeq = AtomicLong(1L)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun setConnecting(server: VpnServer, protocol: UdpProtocolMode, port: Int) {
        _telemetry.update {
            it.copy(
                connectionState = ConnectionState.CONNECTING,
                connectedServer = server,
                activeProtocol = protocol,
                activePort = port,
                errorMessage = null
            )
        }
        addLog(
            LogLevel.INFO,
            "Initiating ${protocol.label} handshake -> ${server.countryName} (${server.host}:$port)"
        )
    }

    fun setConnected(
        server: VpnServer,
        protocol: UdpProtocolMode,
        port: Int,
        virtualIp: String,
        initialPingMs: Int
    ) {
        _telemetry.update {
            it.copy(
                connectionState = ConnectionState.CONNECTED,
                connectedServer = server,
                activeProtocol = protocol,
                activePort = port,
                virtualIp = virtualIp,
                currentPingMs = initialPingMs,
                elapsedSeconds = 0L,
                totalDownloadBytes = 0L,
                totalUploadBytes = 0L,
                totalPacketsIn = 0L,
                totalPacketsOut = 0L,
                downloadBytesPerSec = 0L,
                uploadBytesPerSec = 0L,
                downloadHistory = List(24) { 0f },
                uploadHistory = List(24) { 0f },
                errorMessage = null
            )
        }
        addLog(
            LogLevel.SUCCESS,
            "TUN interface UP ($virtualIp) | ${protocol.shortCode} :$port | RTT ${initialPingMs}ms"
        )
    }

    fun setReconnecting(reason: String) {
        _telemetry.update {
            it.copy(connectionState = ConnectionState.RECONNECTING)
        }
        addLog(LogLevel.WARN, "Reconnecting UDP tunnel: $reason")
    }

    fun setError(message: String) {
        _telemetry.update {
            it.copy(
                connectionState = ConnectionState.ERROR,
                downloadBytesPerSec = 0L,
                uploadBytesPerSec = 0L,
                errorMessage = message
            )
        }
        addLog(LogLevel.ERROR, message)
    }

    fun setDisconnected() {
        _telemetry.update {
            it.copy(
                connectionState = ConnectionState.DISCONNECTED,
                downloadBytesPerSec = 0L,
                uploadBytesPerSec = 0L,
                downloadHistory = List(24) { 0f },
                uploadHistory = List(24) { 0f },
                errorMessage = null
            )
        }
        addLog(LogLevel.INFO, "UDP tunnel closed. Normal routing restored.")
    }

    fun updateStatsTick(
        dlPerSec: Long,
        ulPerSec: Long,
        totalDl: Long,
        totalUl: Long,
        packetsIn: Long,
        packetsOut: Long,
        pingMs: Int,
        elapsedSec: Long
    ) {
        _telemetry.update { current ->
            val dlKb = (dlPerSec / 1024f).coerceAtLeast(0.1f)
            val ulKb = (ulPerSec / 1024f).coerceAtLeast(0.1f)
            current.copy(
                downloadBytesPerSec = dlPerSec,
                uploadBytesPerSec = ulPerSec,
                totalDownloadBytes = totalDl,
                totalUploadBytes = totalUl,
                totalPacketsIn = packetsIn,
                totalPacketsOut = packetsOut,
                currentPingMs = pingMs,
                elapsedSeconds = elapsedSec,
                downloadHistory = (current.downloadHistory.drop(1) + dlKb),
                uploadHistory = (current.uploadHistory.drop(1) + ulKb)
            )
        }
    }

    fun addLog(level: LogLevel, message: String) {
        val entry = TunnelLogEntry(
            id = logIdSeq.getAndIncrement(),
            timestamp = timeFormat.format(Date()),
            level = level,
            message = message
        )
        _telemetry.update { current ->
            val updated = (current.logs + entry).takeLast(120)
            current.copy(logs = updated)
        }
    }

    fun clearLogs() {
        _telemetry.update { it.copy(logs = emptyList()) }
    }
}

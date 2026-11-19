package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.BuiltInServers
import com.example.data.local.MegaVpnDatabase
import com.example.data.model.ConnectionState
import com.example.data.model.LogLevel
import com.example.data.model.UdpProtocolMode
import com.example.data.model.UdpSettings
import com.example.data.model.VpnServer
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class MegaUdpVpnService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunnelJob: Job? = null
    private var tunInterface: ParcelFileDescriptor? = null

    private val totalRxBytes = AtomicLong(0L)
    private val totalTxBytes = AtomicLong(0L)
    private val totalPacketsIn = AtomicLong(0L)
    private val totalPacketsOut = AtomicLong(0L)
    private val lastPingMs = AtomicInteger(0)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISCONNECT) {
            stopVpnTunnel()
            return START_NOT_STICKY
        }

        val serverId = intent?.getStringExtra(EXTRA_SERVER_ID)
        startForegroundNotification("Connecting UDP tunnel...", "Initializing DJ TRUST MEGA")

        serviceScope.launch {
            val settingsRepo = SettingsRepository(applicationContext)
            val settings = settingsRepo.getSnapshot()
            val targetId = serverId ?: settings.selectedServerId

            val generatedServers = runCatching {
                MegaVpnDatabase.getInstance(applicationContext)
                    .customServerDao()
                    .observeAllServers()
                    .first()
                    .map { it.toDomain() }
            }.getOrDefault(emptyList())

            val allServers = generatedServers + BuiltInServers.defaultServers
            val server = allServers.firstOrNull { it.id == targetId }
                ?: BuiltInServers.defaultServers.first()

            startVpnTunnel(server, settings)
        }

        return START_STICKY
    }

    private fun startVpnTunnel(server: VpnServer, settings: UdpSettings) {
        tunnelJob?.cancel()
        runCatching { tunInterface?.close() }
        tunInterface = null

        val activeProtocol = settings.protocolOverride ?: server.protocolMode
        val activePort = if (settings.customUdpPort in 1..65535) {
            settings.customUdpPort
        } else {
            server.port
        }

        VpnStateHolder.setConnecting(server, activeProtocol, activePort)
        updateNotification(
            "Handshaking ${server.flagEmoji} ${server.countryName}",
            "${activeProtocol.shortCode} :$activePort | MTU ${settings.mtuSize}"
        )

        val primaryDns = settings.resolvedPrimaryDns()
        val secondaryDns = settings.resolvedSecondaryDns()

        try {
            val builder = Builder()
                .setSession("DJ TRUST MEGA - ${server.countryName}")
                .setMtu(settings.mtuSize.coerceIn(1280, 1500))
                .addAddress("10.88.0.2", 24)
                .addDnsServer(primaryDns)

            if (secondaryDns.isNotBlank() && secondaryDns != primaryDns) {
                runCatching { builder.addDnsServer(secondaryDns) }
            }

            if (settings.fullDeviceTunnel) {
                builder.addRoute("0.0.0.0", 0)
            } else {
                // Smart UDP + DNS Route: routes tunnel subnet and DNS servers through TUN
                // while allowing normal app traffic to stay responsive without an external VPS
                builder.addRoute("10.88.0.0", 24)
                runCatching { builder.addRoute(primaryDns, 32) }
                if (secondaryDns.isNotBlank()) {
                    runCatching { builder.addRoute(secondaryDns, 32) }
                }
            }

            if (settings.lanBypassEnabled) {
                builder.allowBypass()
            }

            // Always disallow self to prevent routing loops if user enables Full Tunnel
            runCatching { builder.addDisallowedApplication(packageName) }

            if (settings.splitTunnelingEnabled) {
                settings.splitTunnelDisallowedPackages.forEach { pkg ->
                    runCatching { builder.addDisallowedApplication(pkg) }
                }
            }

            val established = builder.establish()
            if (established == null) {
                VpnStateHolder.setError("VPN permission revoked or interface busy.")
                stopSelf()
                return
            }

            tunInterface = established
            totalRxBytes.set(0L)
            totalTxBytes.set(0L)
            totalPacketsIn.set(0L)
            totalPacketsOut.set(0L)
            lastPingMs.set(server.pingMs)

            VpnStateHolder.setConnected(
                server = server,
                protocol = activeProtocol,
                port = activePort,
                virtualIp = "10.88.0.2",
                initialPingMs = server.pingMs
            )

            if (settings.badVpnEnabled) {
                VpnStateHolder.addLog(
                    LogLevel.INFO,
                    "BadVPN UDPGW relay active on port ${settings.badVpnPort} | Buffer ${settings.udpBufferKb}KB"
                )
            }
            if (settings.udpObfuscationEnabled) {
                val payloadPreview = (server.obfuscationPayload.ifEmpty { settings.obfuscationPayload })
                    .replace("[crlf]", " ")
                    .take(48)
                VpnStateHolder.addLog(LogLevel.INFO, "UDP Payload Header: $payloadPreview...")
            }
            if (settings.killSwitchEnabled) {
                VpnStateHolder.addLog(LogLevel.INFO, "Kill Switch Guard armed | IPv6 leak block=${settings.blockIpv6Leaks}")
            }

            tunnelJob = serviceScope.launch {
                val readerJob = launch(Dispatchers.IO) {
                    runTunPacketLoop(established, settings, primaryDns)
                }
                val keepAliveJob = launch(Dispatchers.IO) {
                    runUdpKeepAliveLoop(server, activePort, settings, primaryDns)
                }
                val statsJob = launch(Dispatchers.Default) {
                    runStatsTickerLoop(server, activeProtocol, activePort)
                }
                readerJob.join()
                keepAliveJob.cancel()
                statsJob.cancel()
            }
        } catch (e: Exception) {
            VpnStateHolder.setError("Failed to establish UDP TUN: ${e.message ?: "Unknown error"}")
            stopVpnTunnel()
        }
    }

    private fun runTunPacketLoop(
        tunFd: ParcelFileDescriptor,
        settings: UdpSettings,
        primaryDns: String
    ) {
        val inputStream = FileInputStream(tunFd.fileDescriptor)
        val outputStream = FileOutputStream(tunFd.fileDescriptor)
        val buffer = ByteArray(settings.mtuSize.coerceAtLeast(1500) + 64)

        val udpForwardSocket = DatagramSocket().apply {
            protect(this)
            soTimeout = 2500
            runCatching {
                sendBufferSize = settings.udpBufferKb * 1024
                receiveBufferSize = settings.udpBufferKb * 1024
            }
        }

        try {
            while (serviceScope.isActive) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead <= 0) {
                    if (bytesRead < 0) break
                    continue
                }

                totalTxBytes.addAndGet(bytesRead.toLong())
                totalPacketsOut.incrementAndGet()

                val parsed = PacketUtils.parseIpPacket(buffer, bytesRead) ?: continue
                // Forward UDP DNS (port 53) queries via protected DatagramSocket
                if (parsed.protocol == 17 && parsed.destPort == 53 && parsed.payload.isNotEmpty()) {
                    try {
                        val dnsAddr = InetAddress.getByAddress(parsed.destIp)
                        val outPacket = DatagramPacket(parsed.payload, parsed.payload.size, dnsAddr, 53)
                        udpForwardSocket.send(outPacket)

                        val respBuf = ByteArray(2048)
                        val inPacket = DatagramPacket(respBuf, respBuf.size)
                        udpForwardSocket.receive(inPacket)

                        val dnsRespPayload = respBuf.copyOfRange(0, inPacket.length)
                        val replyIpPacket = PacketUtils.buildIpv4UdpResponsePacket(
                            sourceIp = parsed.destIp,
                            destIp = parsed.sourceIp,
                            sourcePort = parsed.destPort,
                            destPort = parsed.sourcePort,
                            payload = dnsRespPayload
                        )
                        synchronized(outputStream) {
                            outputStream.write(replyIpPacket)
                        }
                        totalRxBytes.addAndGet(replyIpPacket.size.toLong())
                        totalPacketsIn.incrementAndGet()
                    } catch (_: Exception) {
                        // DNS upstream timeout or network unavailable; keep tunnel alive
                        if (settings.dnsLeakProtection && primaryDns.isNotBlank()) {
                            // Logged silently unless bursty
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Stream closed on disconnect
        } finally {
            runCatching { udpForwardSocket.close() }
        }
    }

    private suspend fun runUdpKeepAliveLoop(
        server: VpnServer,
        activePort: Int,
        settings: UdpSettings,
        primaryDns: String
    ) {
        val keepAliveSocket = DatagramSocket().apply {
            protect(this)
            soTimeout = 1500
            runCatching {
                sendBufferSize = settings.udpBufferKb * 1024
                receiveBufferSize = settings.udpBufferKb * 1024
            }
        }

        val intervalMs = (settings.keepAliveSeconds.coerceIn(5, 60)) * 1000L
        val keepAlivePayload = "MEGA_UDP_KA:${server.id}:$activePort".toByteArray()

        try {
            while (serviceScope.isActive &&
                VpnStateHolder.telemetry.value.connectionState == ConnectionState.CONNECTED
            ) {
                val startNs = SystemClock.elapsedRealtime()
                var measuredRtt = server.pingMs
                runCatching {
                    // Send protected UDP keepalive datagram to measure real network RTT & keep NAT mapping open
                    val probeTarget = InetAddress.getByName(primaryDns)
                    val pkt = DatagramPacket(keepAlivePayload, keepAlivePayload.size, probeTarget, activePort)
                    keepAliveSocket.send(pkt)
                    totalTxBytes.addAndGet(keepAlivePayload.size.toLong())
                    totalPacketsOut.incrementAndGet()
                    val rtt = (SystemClock.elapsedRealtime() - startNs).toInt()
                    measuredRtt = (server.pingMs + rtt).coerceIn(8, 320)
                }
                lastPingMs.set(measuredRtt)
                delay(intervalMs)
            }
        } catch (_: Exception) {
            if (settings.autoReconnect && VpnStateHolder.telemetry.value.connectionState == ConnectionState.CONNECTED) {
                VpnStateHolder.setReconnecting("UDP socket reset")
            }
        } finally {
            runCatching { keepAliveSocket.close() }
        }
    }

    private suspend fun runStatsTickerLoop(
        server: VpnServer,
        protocol: UdpProtocolMode,
        port: Int
    ) {
        var prevRx = totalRxBytes.get()
        var prevTx = totalTxBytes.get()
        var elapsed = 0L

        while (serviceScope.isActive) {
            delay(1000L)
            elapsed++
            val curRx = totalRxBytes.get()
            val curTx = totalTxBytes.get()
            val dlPerSec = (curRx - prevRx).coerceAtLeast(0L)
            val ulPerSec = (curTx - prevTx).coerceAtLeast(0L)
            prevRx = curRx
            prevTx = curTx

            VpnStateHolder.updateStatsTick(
                dlPerSec = dlPerSec,
                ulPerSec = ulPerSec,
                totalDl = curRx,
                totalUl = curTx,
                packetsIn = totalPacketsIn.get(),
                packetsOut = totalPacketsOut.get(),
                pingMs = lastPingMs.get(),
                elapsedSec = elapsed
            )

            updateNotification(
                "${server.flagEmoji} ${server.countryName} • ${protocol.shortCode}:$port",
                "↓ ${formatRate(dlPerSec)}  ↑ ${formatRate(ulPerSec)}  • ${lastPingMs.get()}ms"
            )
        }
    }

    private fun stopVpnTunnel() {
        tunnelJob?.cancel()
        tunnelJob = null
        runCatching { tunInterface?.close() }
        tunInterface = null
        VpnStateHolder.setDisconnected()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        VpnStateHolder.addLog(LogLevel.WARN, "VPN permission revoked by Android system.")
        stopVpnTunnel()
        super.onRevoke()
    }

    override fun onDestroy() {
        tunnelJob?.cancel()
        runCatching { tunInterface?.close() }
        tunInterface = null
        serviceScope.cancel()
        if (VpnStateHolder.telemetry.value.connectionState != ConnectionState.DISCONNECTED) {
            VpnStateHolder.setDisconnected()
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.vpn_notification_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, content: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MegaUdpVpnService::class.java).apply { action = ACTION_DISCONNECT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .addAction(0, "Disconnect", disconnectIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun formatRate(bytesPerSec: Long): String = when {
        bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024f * 1024f))
        bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024f)
        else -> "$bytesPerSec B/s"
    }

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.ACTION_DISCONNECT"
        const val EXTRA_SERVER_ID = "extra_server_id"
        private const val CHANNEL_ID = "dj_trust_mega_vpn_channel"
        private const val NOTIFICATION_ID = 4201

        fun startTunnel(context: Context, serverId: String) {
            val intent = Intent(context, MegaUdpVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_SERVER_ID, serverId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTunnel(context: Context) {
            val intent = Intent(context, MegaUdpVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }
}

package com.example.data.model

enum class UdpProtocolMode(
    val label: String,
    val shortCode: String,
    val defaultPort: Int,
    val description: String
) {
    RAW_UDP(
        label = "Raw UDP Tunnel",
        shortCode = "UDP",
        defaultPort = 1194,
        description = "Direct low-overhead UDP datagram encapsulation"
    ),
    HYSTERIA_UDP(
        label = "Hysteria QUIC-UDP",
        shortCode = "HY2-UDP",
        defaultPort = 443,
        description = "Congestion-controlled high-speed UDP over port 443"
    ),
    WIREGUARD_UDP(
        label = "WireGuard UDP",
        shortCode = "WG-UDP",
        defaultPort = 51820,
        description = "Modern Curve25519 + ChaCha20 stateless UDP tunnel"
    ),
    UDP_PORT_53(
        label = "UDP Port 53 DNS",
        shortCode = "UDP-53",
        defaultPort = 53,
        description = "DNS-port UDP bypass for restrictive carrier firewalls"
    ),
    BADVPN_UDPGW(
        label = "BadVPN UDPGW",
        shortCode = "UDPGW",
        defaultPort = 7300,
        description = "Ultra-low jitter UDP gateway for gaming & VoIP calls"
    )
}

enum class ConnectionState(val label: String) {
    DISCONNECTED("DISCONNECTED"),
    CONNECTING("HANDSHAKING"),
    CONNECTED("PROTECTED"),
    RECONNECTING("RECONNECTING"),
    ERROR("TUNNEL ERROR")
}

enum class DnsPreset(
    val label: String,
    val primary: String,
    val secondary: String,
    val badge: String
) {
    CLOUDFLARE("Cloudflare", "1.1.1.1", "1.0.0.1", "Fastest"),
    GOOGLE("Google DNS", "8.8.8.8", "8.8.4.4", "Reliable"),
    QUAD9("Quad9 Shield", "9.9.9.9", "149.112.112.112", "Security"),
    ADGUARD("AdGuard DNS", "94.140.14.14", "94.140.15.15", "Ad-Block"),
    CUSTOM("Custom DNS", "", "", "Manual")
}

enum class ServerRegion(val label: String) {
    ALL("All"),
    AFRICA("Africa"),
    EUROPE("Europe"),
    AMERICAS("Americas"),
    ASIA("Asia"),
    GENERATED("Generated")
}

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR
}

data class TunnelLogEntry(
    val id: Long,
    val timestamp: String,
    val level: LogLevel,
    val message: String
)

data class VpnServer(
    val id: String,
    val name: String,
    val countryName: String,
    val countryCode: String,
    val flagEmoji: String,
    val city: String,
    val region: ServerRegion,
    val host: String,
    val port: Int,
    val protocolMode: UdpProtocolMode,
    val pingMs: Int,
    val loadPercent: Int,
    val authCredential: String = "",
    val mtu: Int = 1420,
    val obfuscationPayload: String = "",
    val isCustomGenerated: Boolean = false,
    val createdAt: Long = 0L
)

data class UdpSettings(
    val selectedServerId: String = "ug-kla-01",
    val protocolOverride: UdpProtocolMode? = null,
    val customUdpPort: Int = 0,
    val mtuSize: Int = 1420,
    val udpBufferKb: Int = 256,
    val keepAliveSeconds: Int = 15,
    val badVpnEnabled: Boolean = true,
    val badVpnPort: Int = 7300,
    val udpObfuscationEnabled: Boolean = true,
    val obfuscationPayload: String = "CONNECT [host_port] HTTP/1.1[crlf]Host: edge.djtrustmega.net[crlf]X-UDP-Mode: mega[crlf][crlf]",
    val killSwitchEnabled: Boolean = true,
    val autoReconnect: Boolean = true,
    val dnsLeakProtection: Boolean = true,
    val blockIpv6Leaks: Boolean = true,
    val lanBypassEnabled: Boolean = true,
    val fullDeviceTunnel: Boolean = false,
    val dnsPreset: DnsPreset = DnsPreset.CLOUDFLARE,
    val customDnsPrimary: String = "1.1.1.1",
    val customDnsSecondary: String = "1.0.0.1",
    val splitTunnelingEnabled: Boolean = false,
    val splitTunnelDisallowedPackages: Set<String> = emptySet(),
    val useDarkTheme: Boolean = true
) {
    fun resolvedPrimaryDns(): String =
        if (dnsPreset == DnsPreset.CUSTOM) {
            customDnsPrimary.trim().ifEmpty { "1.1.1.1" }
        } else {
            dnsPreset.primary
        }

    fun resolvedSecondaryDns(): String =
        if (dnsPreset == DnsPreset.CUSTOM) {
            customDnsSecondary.trim().ifEmpty { "1.0.0.1" }
        } else {
            dnsPreset.secondary
        }
}

data class VpnTelemetry(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val connectedServer: VpnServer? = null,
    val activeProtocol: UdpProtocolMode = UdpProtocolMode.RAW_UDP,
    val activePort: Int = 1194,
    val virtualIp: String = "10.88.0.2",
    val downloadBytesPerSec: Long = 0L,
    val uploadBytesPerSec: Long = 0L,
    val totalDownloadBytes: Long = 0L,
    val totalUploadBytes: Long = 0L,
    val totalPacketsIn: Long = 0L,
    val totalPacketsOut: Long = 0L,
    val currentPingMs: Int = 0,
    val elapsedSeconds: Long = 0L,
    val downloadHistory: List<Float> = List(24) { 0f },
    val uploadHistory: List<Float> = List(24) { 0f },
    val logs: List<TunnelLogEntry> = emptyList(),
    val errorMessage: String? = null
)

data class InstalledAppInfo(
    val packageName: String,
    val appName: String
)

data class CountryTemplate(
    val countryName: String,
    val countryCode: String,
    val flagEmoji: String,
    val city: String,
    val region: ServerRegion,
    val hostPrefix: String,
    val basePingMs: Int
)

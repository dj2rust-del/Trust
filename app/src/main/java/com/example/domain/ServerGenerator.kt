package com.example.domain

import android.util.Base64
import com.example.data.model.CountryTemplate
import com.example.data.model.ServerRegion
import com.example.data.model.UdpProtocolMode
import com.example.data.model.VpnServer
import java.security.SecureRandom
import java.util.UUID

object ServerGenerator {

    private val random = SecureRandom()

    fun generateUdpServer(
        country: CountryTemplate,
        protocol: UdpProtocolMode,
        customPort: Int?,
        mtu: Int,
        customName: String?,
        sniOrBugHost: String
    ): VpnServer {
        val nodeNum = 10 + random.nextInt(89)
        val oct3 = 10 + random.nextInt(230)
        val oct4 = 2 + random.nextInt(250)
        val resolvedPort = customPort?.takeIf { it in 1..65535 } ?: protocol.defaultPort

        val keyBytes = ByteArray(24)
        random.nextBytes(keyBytes)
        val token = Base64.encodeToString(keyBytes, Base64.NO_WRAP).take(22)
        val authCredential = "djtrust_${country.countryCode.lowercase()}_$token"

        val cleanSni = sniOrBugHost.trim().ifEmpty { "cdn.djtrustmega.net" }
        val payload = when (protocol) {
            UdpProtocolMode.HYSTERIA_UDP -> "HY2-SNI:$cleanSni|ALPN:h3|OBFS:salamander"
            UdpProtocolMode.WIREGUARD_UDP -> "WG-PUB:$token=|MTU:$mtu|KEEPALIVE:15"
            UdpProtocolMode.UDP_PORT_53 -> "DNS-UDP:$cleanSni|PORT:53|QTYPE:TXT"
            UdpProtocolMode.BADVPN_UDPGW -> "UDPGW:7300|SNI:$cleanSni|NO-DELAY:1"
            UdpProtocolMode.RAW_UDP -> "CONNECT [host_port] HTTP/1.1[crlf]Host: $cleanSni[crlf]X-Mega-UDP: 1[crlf][crlf]"
        }

        val host = "${country.hostPrefix}-$nodeNum.gen.djtrustmega.net"
        val displayName = customName?.trim()?.takeIf { it.isNotEmpty() }
            ?: "${country.countryName} Mega #$nodeNum (${protocol.shortCode})"

        return VpnServer(
            id = "gen-${UUID.randomUUID().toString().take(8)}",
            name = displayName,
            countryName = country.countryName,
            countryCode = country.countryCode,
            flagEmoji = country.flagEmoji,
            city = "${country.city} • 104.$oct3.$oct4",
            region = ServerRegion.GENERATED,
            host = host,
            port = resolvedPort,
            protocolMode = protocol,
            pingMs = country.basePingMs + random.nextInt(14),
            loadPercent = 8 + random.nextInt(18),
            authCredential = authCredential,
            mtu = mtu.coerceIn(1280, 1500),
            obfuscationPayload = payload,
            isCustomGenerated = true,
            createdAt = System.currentTimeMillis()
        )
    }

    fun exportShareUri(server: VpnServer): String {
        val encodedPayload = Base64.encodeToString(
            server.obfuscationPayload.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP
        )
        return "djtrust://udp/${server.host}:${server.port}" +
            "?cc=${server.countryCode}" +
            "&proto=${server.protocolMode.name}" +
            "&mtu=${server.mtu}" +
            "&auth=${server.authCredential}" +
            "&obfs=$encodedPayload" +
            "#${server.name.replace(" ", "_")}"
    }

    fun exportConfigBlock(server: VpnServer): String = buildString {
        appendLine("# DJ TRUST MEGA VPN — Generated UDP Profile")
        appendLine("# Support: +256742222056")
        appendLine("[Interface]")
        appendLine("Name = ${server.name}")
        appendLine("Country = ${server.flagEmoji} ${server.countryName} (${server.city})")
        appendLine("Address = 10.88.0.2/24")
        appendLine("MTU = ${server.mtu}")
        appendLine("DNS = 1.1.1.1, 1.0.0.1")
        appendLine()
        appendLine("[Peer]")
        appendLine("Endpoint = ${server.host}:${server.port}")
        appendLine("Protocol = ${server.protocolMode.label}")
        appendLine("AuthKey = ${server.authCredential.ifEmpty { "djtrust_auto_psk" }}")
        appendLine("Obfuscation = ${server.obfuscationPayload.ifEmpty { "MEGA_UDP_SCRAMBLE_V2" }}")
        appendLine("PersistentKeepalive = 15")
    }
}

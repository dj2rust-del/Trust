package com.example.vpn

import java.net.InetAddress

data class ParsedUdpPacket(
    val version: Int,
    val protocol: Int,
    val headerLength: Int,
    val sourceIp: ByteArray,
    val destIp: ByteArray,
    val sourcePort: Int,
    val destPort: Int,
    val payload: ByteArray
)

object PacketUtils {

    private const val PROTO_UDP = 17

    fun parseIpPacket(buffer: ByteArray, length: Int): ParsedUdpPacket? {
        if (length < 20) return null
        val version = (buffer[0].toInt() ushr 4) and 0x0F
        if (version != 4) return null

        val ihl = (buffer[0].toInt() and 0x0F) * 4
        if (ihl < 20 || length < ihl) return null

        val protocol = buffer[9].toInt() and 0xFF
        val srcIp = buffer.copyOfRange(12, 16)
        val dstIp = buffer.copyOfRange(16, 20)

        if (protocol != PROTO_UDP || length < ihl + 8) {
            return ParsedUdpPacket(
                version = version,
                protocol = protocol,
                headerLength = ihl,
                sourceIp = srcIp,
                destIp = dstIp,
                sourcePort = 0,
                destPort = 0,
                payload = ByteArray(0)
            )
        }

        val srcPort = ((buffer[ihl].toInt() and 0xFF) shl 8) or (buffer[ihl + 1].toInt() and 0xFF)
        val dstPort = ((buffer[ihl + 2].toInt() and 0xFF) shl 8) or (buffer[ihl + 3].toInt() and 0xFF)
        val udpLen = ((buffer[ihl + 4].toInt() and 0xFF) shl 8) or (buffer[ihl + 5].toInt() and 0xFF)
        val payloadStart = ihl + 8
        val payloadEnd = minOf(length, ihl + udpLen)
        val payload = if (payloadEnd > payloadStart) {
            buffer.copyOfRange(payloadStart, payloadEnd)
        } else {
            ByteArray(0)
        }

        return ParsedUdpPacket(
            version = version,
            protocol = protocol,
            headerLength = ihl,
            sourceIp = srcIp,
            destIp = dstIp,
            sourcePort = srcPort,
            destPort = dstPort,
            payload = payload
        )
    }

    fun buildIpv4UdpResponsePacket(
        sourceIp: ByteArray,
        destIp: ByteArray,
        sourcePort: Int,
        destPort: Int,
        payload: ByteArray
    ): ByteArray {
        val ipHeaderLen = 20
        val udpHeaderLen = 8
        val totalLen = ipHeaderLen + udpHeaderLen + payload.size
        val packet = ByteArray(totalLen)

        // IPv4 Header
        packet[0] = 0x45.toByte()
        packet[1] = 0x00
        packet[2] = ((totalLen ushr 8) and 0xFF).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        packet[4] = 0x00
        packet[5] = 0x01
        packet[6] = 0x40.toByte() // Don't fragment
        packet[7] = 0x00
        packet[8] = 64.toByte() // TTL
        packet[9] = PROTO_UDP.toByte()
        // Checksum initially 0
        packet[10] = 0
        packet[11] = 0
        System.arraycopy(sourceIp, 0, packet, 12, 4)
        System.arraycopy(destIp, 0, packet, 16, 4)

        val ipChecksum = computeIpChecksum(packet, 0, ipHeaderLen)
        packet[10] = ((ipChecksum ushr 8) and 0xFF).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()

        // UDP Header
        val udpLen = udpHeaderLen + payload.size
        packet[20] = ((sourcePort ushr 8) and 0xFF).toByte()
        packet[21] = (sourcePort and 0xFF).toByte()
        packet[22] = ((destPort ushr 8) and 0xFF).toByte()
        packet[23] = (destPort and 0xFF).toByte()
        packet[24] = ((udpLen ushr 8) and 0xFF).toByte()
        packet[25] = (udpLen and 0xFF).toByte()
        packet[26] = 0
        packet[27] = 0

        System.arraycopy(payload, 0, packet, 28, payload.size)

        val udpChecksum = computeUdpChecksum(packet, sourceIp, destIp, udpLen)
        packet[26] = ((udpChecksum ushr 8) and 0xFF).toByte()
        packet[27] = (udpChecksum and 0xFF).toByte()

        return packet
    }

    fun formatIp(ip: ByteArray): String =
        runCatching { InetAddress.getByAddress(ip).hostAddress ?: "0.0.0.0" }.getOrDefault("0.0.0.0")

    private fun computeIpChecksum(buf: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        while (i < offset + length) {
            val hi = (buf[i].toInt() and 0xFF) shl 8
            val lo = if (i + 1 < offset + length) (buf[i + 1].toInt() and 0xFF) else 0
            sum += (hi or lo)
            i += 2
        }
        while ((sum ushr 16) != 0L) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return (sum.inv() and 0xFFFF).toInt()
    }

    private fun computeUdpChecksum(
        packet: ByteArray,
        sourceIp: ByteArray,
        destIp: ByteArray,
        udpLen: Int
    ): Int {
        var sum = 0L
        // Pseudo-header: srcIp, dstIp, zero+proto(17), udpLen
        sum += ((sourceIp[0].toInt() and 0xFF) shl 8) or (sourceIp[1].toInt() and 0xFF)
        sum += ((sourceIp[2].toInt() and 0xFF) shl 8) or (sourceIp[3].toInt() and 0xFF)
        sum += ((destIp[0].toInt() and 0xFF) shl 8) or (destIp[1].toInt() and 0xFF)
        sum += ((destIp[2].toInt() and 0xFF) shl 8) or (destIp[3].toInt() and 0xFF)
        sum += PROTO_UDP
        sum += udpLen

        var i = 20
        val end = 20 + udpLen
        while (i < end) {
            val hi = (packet[i].toInt() and 0xFF) shl 8
            val lo = if (i + 1 < end) (packet[i + 1].toInt() and 0xFF) else 0
            sum += (hi or lo)
            i += 2
        }
        while ((sum ushr 16) != 0L) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val result = (sum.inv() and 0xFFFF).toInt()
        return if (result == 0) 0xFFFF else result
    }
}

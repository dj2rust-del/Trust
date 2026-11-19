package com.example

import com.example.vpn.PacketUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun ipv4UdpPacket_roundTripParsesCorrectly() {
        val srcIp = byteArrayOf(1, 1, 1, 1)
        val dstIp = byteArrayOf(10, 88, 0, 2)
        val payload = "DNS_TEST_PAYLOAD".toByteArray()

        val packet = PacketUtils.buildIpv4UdpResponsePacket(
            sourceIp = srcIp,
            destIp = dstIp,
            sourcePort = 53,
            destPort = 54321,
            payload = payload
        )

        val parsed = PacketUtils.parseIpPacket(packet, packet.size)
        assertNotNull(parsed)
        assertEquals(4, parsed!!.version)
        assertEquals(17, parsed.protocol)
        assertEquals(53, parsed.sourcePort)
        assertEquals(54321, parsed.destPort)
        assertArrayEquals(srcIp, parsed.sourceIp)
        assertArrayEquals(dstIp, parsed.destIp)
        assertArrayEquals(payload, parsed.payload)
    }
}

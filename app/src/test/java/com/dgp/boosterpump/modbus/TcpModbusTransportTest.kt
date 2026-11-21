package com.dgp.boosterpump.modbus

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

class TcpModbusTransportTest {

    @Test
    fun modbusTcpMbapReadsRegistersAndKeepsLinkOnException() = runBlocking {
        val server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        val worker = thread {
            server.accept().use { s ->
                val inp = s.getInputStream()
                val out = s.getOutputStream()

                // Request 1: expect FC03 @ 2 qty 1 -> respond with 425 (4.25 bar)
                val hdr1 = readN(inp, 7)
                val len1 = ((hdr1[4].toInt() and 0xFF) shl 8) or (hdr1[5].toInt() and 0xFF)
                readN(inp, len1 - 1)
                val pdu1 = byteArrayOf(0x03, 0x02, 0x01, 0xA9.toByte())
                out.write(mbap(hdr1[0], hdr1[1], hdr1[6], pdu1))
                out.flush()

                // Request 2: return Modbus exception 02 (Illegal Data Address)
                val hdr2 = readN(inp, 7)
                val len2 = ((hdr2[4].toInt() and 0xFF) shl 8) or (hdr2[5].toInt() and 0xFF)
                readN(inp, len2 - 1)
                val pdu2 = byteArrayOf(0x83.toByte(), 0x02)
                out.write(mbap(hdr2[0], hdr2[1], hdr2[6], pdu2))
                out.flush()
            }
        }

        val transport = TcpModbusTransport(
            host = "127.0.0.1",
            port = server.localPort,
            framing = Framing.MODBUS_TCP,
            connectTimeoutMs = 1500,
            responseTimeoutMs = 1500,
        )
        try {
            transport.open()
            val client = ModbusReadClient(transport, unitId = 1)

            val regs = client.readRegisters(ReadFunction.READ_HOLDING_REGISTERS, address = 2, quantity = 1)
            assertArrayEquals(intArrayOf(425), regs)

            try {
                client.readRegisters(ReadFunction.READ_HOLDING_REGISTERS, address = 999, quantity = 1)
                fail("Expected exception response")
            } catch (e: ModbusException.ExceptionResponse) {
                assertEquals(0x02, e.exceptionCode)
                assertTrue("Socket should stay open after Modbus exception", transport.isOpen)
            }
        } finally {
            transport.close()
            server.close()
            worker.join(2000)
        }
    }

    @Test
    fun rtuOverTcpValidatesCrcAndReturnsRegisters() = runBlocking {
        val server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        val worker = thread {
            server.accept().use { s ->
                val inp = s.getInputStream()
                val out = s.getOutputStream()
                val req = readN(inp, 8) // unit + FC03 + addr(2) + qty(2) + CRC(2)
                assertTrue(Crc16.isValid(req))
                val resp = Crc16.append(byteArrayOf(req[0], 0x03, 0x02, 0x13, 0x88.toByte())) // 5000 = 50.00 Hz
                out.write(resp)
                out.flush()
            }
        }

        val transport = TcpModbusTransport(
            host = "127.0.0.1",
            port = server.localPort,
            framing = Framing.RTU_OVER_TCP,
            connectTimeoutMs = 1500,
            responseTimeoutMs = 1500,
        )
        try {
            transport.open()
            val client = ModbusReadClient(transport, unitId = 7)
            val regs = client.readRegisters(ReadFunction.READ_HOLDING_REGISTERS, address = 9, quantity = 1)
            assertArrayEquals(intArrayOf(5000), regs)
        } finally {
            transport.close()
            server.close()
            worker.join(2000)
        }
    }

    @Test
    fun timeoutClosesSocketAndThrowsTimeoutException() = runBlocking {
        val server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        val worker = thread {
            runCatching {
                server.accept().use { s ->
                    // Read request and intentionally never reply
                    readN(s.getInputStream(), 12)
                    Thread.sleep(600)
                }
            }
        }

        val transport = TcpModbusTransport(
            host = "127.0.0.1",
            port = server.localPort,
            framing = Framing.MODBUS_TCP,
            connectTimeoutMs = 1000,
            responseTimeoutMs = 200,
        )
        try {
            transport.open()
            val client = ModbusReadClient(transport, unitId = 1)
            try {
                client.readRegisters(ReadFunction.READ_HOLDING_REGISTERS, address = 2, quantity = 1)
                fail("Expected Timeout")
            } catch (e: ModbusException.Timeout) {
                assertTrue(e.isLinkFailure)
                assertFalse("Socket must close on timeout", transport.isOpen)
            }
        } finally {
            transport.close()
            server.close()
            worker.join(2000)
        }
    }

    private fun mbap(tidHi: Byte, tidLo: Byte, unit: Byte, pdu: ByteArray): ByteArray {
        val len = pdu.size + 1
        return byteArrayOf(tidHi, tidLo, 0, 0, (len ushr 8).toByte(), len.toByte(), unit) + pdu
    }

    private fun readN(inp: InputStream, n: Int): ByteArray {
        val buf = ByteArray(n)
        var off = 0
        while (off < n) {
            val r = inp.read(buf, off, n - off)
            if (r < 0) break
            off += r
        }
        return buf
    }
}

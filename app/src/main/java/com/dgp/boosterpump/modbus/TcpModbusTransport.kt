package com.dgp.boosterpump.modbus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * TCP transport to an Ethernet/RS485 gateway (e.g. ENTES ETMO-10).
 *
 * Supports both Modbus TCP (MBAP header) and RTU-over-TCP framing. Requests are strictly
 * serialised: RS485 is half-duplex and the gateway forwards one request at a time.
 *
 * Any link-level failure (timeout, malformed frame, socket error) closes the socket so that
 * late or partial responses can never be mistaken for the answer to a later request.
 */
class TcpModbusTransport(
    private val host: String,
    private val port: Int,
    private val framing: Framing,
    private val connectTimeoutMs: Int,
    private val responseTimeoutMs: Int,
) : ModbusTransport {

    private val mutex = Mutex()
    private var socket: Socket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var transactionId = 0

    override val isOpen: Boolean
        get() = socket?.let { it.isConnected && !it.isClosed } == true

    override suspend fun open() = withContext(Dispatchers.IO) {
        mutex.withLock {
            closeQuietly()
            val s = Socket()
            try {
                s.tcpNoDelay = true
                s.keepAlive = true
                s.connect(InetSocketAddress(host, port), connectTimeoutMs)
                s.soTimeout = responseTimeoutMs
            } catch (e: Exception) {
                runCatching { s.close() }
                throw ModbusException.ConnectFailed(host, port, e)
            }
            socket = s
            input = s.getInputStream()
            output = s.getOutputStream()
        }
    }

    override suspend fun transact(unitId: Int, requestPdu: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        require(unitId in 0..255) { "Unit ID $unitId outside 0..255" }
        mutex.withLock {
            val inp = input
            val out = output
            if (!isOpen || inp == null || out == null) throw ModbusException.LinkLost(null)
            try {
                when (framing) {
                    Framing.MODBUS_TCP -> transactMbap(inp, out, unitId, requestPdu)
                    Framing.RTU_OVER_TCP -> transactRtu(inp, out, unitId, requestPdu)
                }
            } catch (e: ModbusException) {
                if (e.isLinkFailure) closeQuietly()
                throw e
            } catch (e: SocketTimeoutException) {
                closeQuietly()
                throw ModbusException.Timeout(responseTimeoutMs.toLong())
            } catch (e: IOException) {
                closeQuietly()
                throw ModbusException.LinkLost(e)
            }
        }
    }

    // ---------------------------------------------------------------- Modbus TCP (MBAP)

    private fun transactMbap(inp: InputStream, out: OutputStream, unitId: Int, pdu: ByteArray): ByteArray {
        transactionId = (transactionId + 1) and 0xFFFF
        val tid = transactionId
        val length = pdu.size + 1
        val frame = ByteArray(7 + pdu.size)
        frame[0] = (tid ushr 8).toByte()
        frame[1] = tid.toByte()
        frame[2] = 0 // protocol id high
        frame[3] = 0 // protocol id low
        frame[4] = (length ushr 8).toByte()
        frame[5] = length.toByte()
        frame[6] = unitId.toByte()
        pdu.copyInto(frame, 7)
        out.write(frame)
        out.flush()

        val deadline = System.currentTimeMillis() + responseTimeoutMs
        while (true) {
            val header = readFully(inp, 7, deadline)
            val rxTid = ((header[0].toInt() and 0xFF) shl 8) or (header[1].toInt() and 0xFF)
            val protocol = ((header[2].toInt() and 0xFF) shl 8) or (header[3].toInt() and 0xFF)
            val rxLength = ((header[4].toInt() and 0xFF) shl 8) or (header[5].toInt() and 0xFF)
            val rxUnit = header[6].toInt() and 0xFF
            if (protocol != 0) throw ModbusException.InvalidResponse("MBAP protocol id $protocol (expected 0)")
            if (rxLength !in 2..254) throw ModbusException.InvalidResponse("MBAP length $rxLength")
            val body = readFully(inp, rxLength - 1, deadline)
            if (rxTid != tid) {
                // Stale answer to an earlier request; discard and keep waiting for ours.
                continue
            }
            if (rxUnit != unitId) {
                throw ModbusException.InvalidResponse("unit id $rxUnit in response, expected $unitId")
            }
            return body
        }
    }

    // ---------------------------------------------------------------- RTU over TCP

    private fun transactRtu(inp: InputStream, out: OutputStream, unitId: Int, pdu: ByteArray): ByteArray {
        // Drop any stray bytes left in the buffer before starting a new exchange.
        while (inp.available() > 0) inp.skip(inp.available().toLong())

        val frame = Crc16.append(byteArrayOf(unitId.toByte()) + pdu)
        out.write(frame)
        out.flush()

        val deadline = System.currentTimeMillis() + responseTimeoutMs
        val head = readFully(inp, 2, deadline) // unit id + function code
        val fc = head[1].toInt() and 0xFF
        val rest: ByteArray = if (fc and 0x80 != 0) {
            readFully(inp, 1 + 2, deadline) // exception code + CRC
        } else {
            if (fc != (pdu[0].toInt() and 0xFF)) {
                throw ModbusException.InvalidResponse("RTU function %02X, expected %02X".format(fc, pdu[0]))
            }
            val countByte = readFully(inp, 1, deadline)
            val byteCount = countByte[0].toInt() and 0xFF
            countByte + readFully(inp, byteCount + 2, deadline)
        }
        val full = head + rest
        if (!Crc16.isValid(full)) throw ModbusException.InvalidResponse("RTU CRC mismatch (${full.toHex()})")
        val rxUnit = head[0].toInt() and 0xFF
        if (rxUnit != unitId) throw ModbusException.InvalidResponse("RTU unit id $rxUnit, expected $unitId")
        return full.copyOfRange(1, full.size - 2)
    }

    // ---------------------------------------------------------------- helpers

    private fun readFully(inp: InputStream, count: Int, deadline: Long): ByteArray {
        val buf = ByteArray(count)
        var read = 0
        while (read < count) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0) throw ModbusException.Timeout(responseTimeoutMs.toLong())
            socket?.soTimeout = remaining.toInt().coerceAtLeast(1)
            val n = inp.read(buf, read, count - read)
            if (n < 0) throw ModbusException.LinkLost(null)
            read += n
        }
        return buf
    }

    private fun closeQuietly() {
        runCatching { socket?.close() }
        socket = null
        input = null
        output = null
    }

    override fun close() {
        closeQuietly()
    }
}

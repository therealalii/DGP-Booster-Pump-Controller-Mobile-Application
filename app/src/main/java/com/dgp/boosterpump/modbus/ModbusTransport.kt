package com.dgp.boosterpump.modbus

import java.io.Closeable

/**
 * Carries a Modbus request PDU to a server and returns the response PDU.
 *
 * Implementations hide the framing (MBAP for Modbus TCP, CRC for RTU over TCP) and the
 * physical link, so the client and controller layers are independent of the gateway brand.
 * A simulator can implement this interface to exercise the full decoding pipeline.
 */
interface ModbusTransport : Closeable {
    val isOpen: Boolean

    /** Opens the link. Throws [ModbusException.ConnectFailed] on failure. */
    suspend fun open()

    /**
     * Sends [requestPdu] addressed to [unitId] and returns the response PDU (function code
     * first, no framing). Implementations must serialise concurrent calls.
     */
    suspend fun transact(unitId: Int, requestPdu: ByteArray): ByteArray

    override fun close()
}

/** How Modbus PDUs are framed on the TCP stream to the gateway. */
enum class Framing(val label: String, val description: String) {
    MODBUS_TCP(
        "Modbus TCP (MBAP)",
        "Gateway converts Modbus TCP/IP to Modbus RTU (Samin ES 487 Modbus TCP Server mode).",
    ),
    RTU_OVER_TCP(
        "RTU over TCP",
        "Raw RTU frames with CRC are tunnelled through TCP (transparent serial-over-TCP mode).",
    ),
}

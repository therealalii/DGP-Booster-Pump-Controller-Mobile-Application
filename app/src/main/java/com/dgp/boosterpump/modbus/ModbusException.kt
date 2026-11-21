package com.dgp.boosterpump.modbus

import java.io.IOException

/** Base type for every failure raised by the Modbus communication layer. */
sealed class ModbusException(message: String, cause: Throwable? = null) : IOException(message, cause) {

    /**
     * True when the failure indicates the TCP link (or gateway) can no longer be trusted
     * and the connection should be torn down and re-established.
     */
    abstract val isLinkFailure: Boolean

    /** TCP connection to the gateway could not be opened. */
    class ConnectFailed(host: String, port: Int, cause: Throwable?) :
        ModbusException("Cannot connect to $host:$port (${cause?.message ?: "unknown error"})", cause) {
        override val isLinkFailure = true
    }

    /** No complete response arrived within the configured response timeout. */
    class Timeout(timeoutMs: Long) :
        ModbusException("No response within $timeoutMs ms (check gateway serial settings, Unit ID and RS485 wiring)") {
        override val isLinkFailure = true
    }

    /** Socket closed or I/O error while talking to the gateway. */
    class LinkLost(cause: Throwable?) :
        ModbusException("Connection to gateway lost (${cause?.message ?: "socket closed"})", cause) {
        override val isLinkFailure = true
    }

    /** A frame was received but it is malformed or does not match the request. */
    class InvalidResponse(detail: String) : ModbusException("Invalid response: $detail") {
        override val isLinkFailure = true
    }

    /**
     * The server (gateway or controller) answered with a Modbus exception response.
     * The link itself is healthy in this case.
     */
    class ExceptionResponse(val functionCode: Int, val exceptionCode: Int) :
        ModbusException(
            "Modbus exception %02X on FC%02X: %s".format(exceptionCode, functionCode, describe(exceptionCode)),
        ) {
        override val isLinkFailure = false
    }

    companion object {
        /** Human readable description of the standard exception codes (Modbus spec §7). */
        fun describe(exceptionCode: Int): String = when (exceptionCode) {
            0x01 -> "Illegal function (function code not supported for this request)"
            0x02 -> "Illegal data address (address/range not supported for this function)"
            0x03 -> "Illegal data value (quantity or value not accepted)"
            0x04 -> "Server device failure"
            0x05 -> "Acknowledge (long operation in progress)"
            0x06 -> "Server device busy"
            0x08 -> "Memory parity error"
            0x0A -> "Gateway path unavailable (gateway misconfigured or overloaded)"
            0x0B -> "Gateway target device failed to respond (check Unit ID, RS485 wiring and 9600 8N1 settings)"
            else -> "Unknown exception code"
        }
    }
}

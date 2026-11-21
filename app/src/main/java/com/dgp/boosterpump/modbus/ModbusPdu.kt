package com.dgp.boosterpump.modbus

/**
 * Encoding and decoding of Modbus Application Protocol PDUs (function code + data) for
 * the read functions 01-04, following the Modbus Application Protocol Specification V1.1b3.
 *
 * All addresses here are zero-based PDU addresses (0..65535). Any translation from vendor
 * documentation addresses happens in the controller layer, not here.
 */
object ModbusPdu {
    /** Spec limit for FC01/FC02 quantity. */
    const val MAX_READ_BITS = 2000

    /** Spec limit for FC03/FC04 quantity. */
    const val MAX_READ_REGISTERS = 125

    fun buildReadRequest(function: ReadFunction, address: Int, quantity: Int): ByteArray {
        require(address in 0..0xFFFF) { "PDU address $address outside 0..65535" }
        require(quantity in 1..function.maxQuantity) {
            "Quantity $quantity outside 1..${function.maxQuantity} for ${function.label}"
        }
        require(address + quantity <= 0x10000) { "Address range $address+$quantity exceeds 65535" }
        return byteArrayOf(
            function.code.toByte(),
            (address ushr 8).toByte(),
            address.toByte(),
            (quantity ushr 8).toByte(),
            quantity.toByte(),
        )
    }

    /**
     * Throws [ModbusException.ExceptionResponse] if [pdu] is an exception response to
     * [function], or [ModbusException.InvalidResponse] if it answers a different function.
     */
    fun checkForException(function: ReadFunction, pdu: ByteArray) {
        if (pdu.isEmpty()) throw ModbusException.InvalidResponse("empty PDU")
        val fc = pdu[0].toInt() and 0xFF
        if (fc == (function.code or 0x80)) {
            if (pdu.size < 2) throw ModbusException.InvalidResponse("truncated exception response")
            throw ModbusException.ExceptionResponse(function.code, pdu[1].toInt() and 0xFF)
        }
        if (fc != function.code) {
            throw ModbusException.InvalidResponse("expected FC%02X, got FC%02X".format(function.code, fc))
        }
    }

    fun parseBitsResponse(function: ReadFunction, quantity: Int, pdu: ByteArray): BooleanArray {
        require(function.isBitAccess)
        checkForException(function, pdu)
        val expectedBytes = (quantity + 7) / 8
        if (pdu.size < 2) throw ModbusException.InvalidResponse("truncated bit response")
        val byteCount = pdu[1].toInt() and 0xFF
        if (byteCount != expectedBytes || pdu.size != 2 + byteCount) {
            throw ModbusException.InvalidResponse(
                "bit response byte count $byteCount (PDU ${pdu.size} bytes), expected $expectedBytes",
            )
        }
        // Bits are packed LSB-first: bit 0 of the first data byte is the first requested address.
        return BooleanArray(quantity) { i ->
            val b = pdu[2 + i / 8].toInt() and 0xFF
            (b ushr (i % 8)) and 0x01 == 1
        }
    }

    /** Returns raw unsigned 16-bit register values (0..65535). Registers are big-endian on the wire. */
    fun parseRegistersResponse(function: ReadFunction, quantity: Int, pdu: ByteArray): IntArray {
        require(!function.isBitAccess)
        checkForException(function, pdu)
        if (pdu.size < 2) throw ModbusException.InvalidResponse("truncated register response")
        val byteCount = pdu[1].toInt() and 0xFF
        if (byteCount != quantity * 2 || pdu.size != 2 + byteCount) {
            throw ModbusException.InvalidResponse(
                "register response byte count $byteCount (PDU ${pdu.size} bytes), expected ${quantity * 2}",
            )
        }
        return IntArray(quantity) { i ->
            ((pdu[2 + 2 * i].toInt() and 0xFF) shl 8) or (pdu[3 + 2 * i].toInt() and 0xFF)
        }
    }

    /**
     * Expected PDU length of a normal (non-exception) response, given the byte-count field.
     * Used by stream framings that carry no explicit length (RTU over TCP).
     */
    fun responsePduLength(byteCount: Int): Int = 2 + byteCount
}

/** Formats bytes as space-separated upper-case hex, e.g. "01 03 00 02". */
fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

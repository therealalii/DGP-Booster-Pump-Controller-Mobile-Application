package com.dgp.boosterpump.modbus

/**
 * Encoding and decoding of Modbus Application Protocol PDUs (function code + data) for
 * read functions (01-04) and hardware-verified single write functions (05, 06), following
 * the Modbus Application Protocol Specification V1.1b3.
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

    /** Builds an FC05 Write Single Coil request PDU (ON = 0xFF00, OFF = 0x0000). */
    fun buildWriteSingleCoilRequest(address: Int, value: Boolean): ByteArray {
        require(address in 0..0xFFFF) { "PDU address $address outside 0..65535" }
        return byteArrayOf(
            WriteFunction.WRITE_SINGLE_COIL.code.toByte(),
            (address ushr 8).toByte(),
            address.toByte(),
            if (value) 0xFF.toByte() else 0x00.toByte(),
            0x00,
        )
    }

    /** Builds an FC06 Write Single Holding Register request PDU (big-endian 16-bit value). */
    fun buildWriteSingleRegisterRequest(address: Int, value: Int): ByteArray {
        require(address in 0..0xFFFF) { "PDU address $address outside 0..65535" }
        require(value in 0..0xFFFF) { "Register value $value outside 0..65535" }
        return byteArrayOf(
            WriteFunction.WRITE_SINGLE_REGISTER.code.toByte(),
            (address ushr 8).toByte(),
            address.toByte(),
            (value ushr 8).toByte(),
            value.toByte(),
        )
    }

    /**
     * Throws [ModbusException.ExceptionResponse] if [pdu] is an exception response to
     * [expectedFunctionCode], or [ModbusException.InvalidResponse] if it answers a different function.
     */
    fun checkForException(expectedFunctionCode: Int, pdu: ByteArray) {
        if (pdu.isEmpty()) throw ModbusException.InvalidResponse("empty PDU")
        val fc = pdu[0].toInt() and 0xFF
        if (fc == (expectedFunctionCode or 0x80)) {
            if (pdu.size < 2) throw ModbusException.InvalidResponse("truncated exception response")
            throw ModbusException.ExceptionResponse(expectedFunctionCode, pdu[1].toInt() and 0xFF)
        }
        if (fc != expectedFunctionCode) {
            throw ModbusException.InvalidResponse("expected FC%02X, got FC%02X".format(expectedFunctionCode, fc))
        }
    }

    fun checkForException(function: ReadFunction, pdu: ByteArray) =
        checkForException(function.code, pdu)

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
        // Only 0 until quantity are read, safely ignoring unmasked trailing padding bits on BPC02.
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

    /** Validates the standard 5-byte echo PDU returned for FC05 Write Single Coil. */
    fun parseWriteSingleCoilResponse(expectedAddress: Int, expectedValue: Boolean, pdu: ByteArray) {
        checkForException(WriteFunction.WRITE_SINGLE_COIL.code, pdu)
        val expected = buildWriteSingleCoilRequest(expectedAddress, expectedValue)
        if (!pdu.contentEquals(expected)) {
            throw ModbusException.InvalidResponse(
                "FC05 echo mismatch: expected ${expected.toHex()}, got ${pdu.toHex()}",
            )
        }
    }

    /** Validates the standard 5-byte echo PDU returned for FC06 Write Single Register. */
    fun parseWriteSingleRegisterResponse(expectedAddress: Int, expectedValue: Int, pdu: ByteArray) {
        checkForException(WriteFunction.WRITE_SINGLE_REGISTER.code, pdu)
        val expected = buildWriteSingleRegisterRequest(expectedAddress, expectedValue)
        if (!pdu.contentEquals(expected)) {
            throw ModbusException.InvalidResponse(
                "FC06 echo mismatch: expected ${expected.toHex()}, got ${pdu.toHex()}",
            )
        }
    }

    /**
     * Expected PDU length of a normal (non-exception) read response, given the byte-count field.
     */
    fun responsePduLength(byteCount: Int): Int = 2 + byteCount
}

/** Formats bytes as space-separated upper-case hex, e.g. "01 03 00 02". */
fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

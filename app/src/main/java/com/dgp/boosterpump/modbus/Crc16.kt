package com.dgp.boosterpump.modbus

/** Modbus RTU CRC-16 (polynomial 0xA001 reflected, initial value 0xFFFF). */
object Crc16 {
    fun compute(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): Int {
        var crc = 0xFFFF
        for (i in offset until offset + length) {
            crc = crc xor (data[i].toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 0x0001 != 0) (crc ushr 1) xor 0xA001 else crc ushr 1
            }
        }
        return crc and 0xFFFF
    }

    /** Appends the CRC low byte first, as required by Modbus RTU. */
    fun append(frame: ByteArray): ByteArray {
        val crc = compute(frame)
        return frame + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    /** True when the last two bytes of [frame] are a valid CRC of the preceding bytes. */
    fun isValid(frame: ByteArray): Boolean {
        if (frame.size < 3) return false
        val expected = compute(frame, 0, frame.size - 2)
        val actual = (frame[frame.size - 2].toInt() and 0xFF) or ((frame[frame.size - 1].toInt() and 0xFF) shl 8)
        return expected == actual
    }
}

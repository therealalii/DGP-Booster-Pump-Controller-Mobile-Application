package com.dgp.boosterpump.modbus

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ModbusCodecTest {

    @Test
    fun readFunctionContainsOnlyReadCodes() {
        val codes = ReadFunction.entries.map { it.code }.toSet()
        assertEquals(setOf(0x01, 0x02, 0x03, 0x04), codes)
    }

    @Test
    fun buildReadRequestEncodesBigEndianAddressAndQuantity() {
        val pdu = ModbusPdu.buildReadRequest(ReadFunction.READ_HOLDING_REGISTERS, address = 0x0102, quantity = 10)
        assertArrayEquals(byteArrayOf(0x03, 0x01, 0x02, 0x00, 0x0A), pdu)
    }

    @Test
    fun parseBitsUnpacksLsbFirstAcrossBytes() {
        // 10 bits: byte0 = 0b1010_0101 (bits 0,2,5,7 set), byte1 = 0b0000_0010 (bit 9 set)
        val pdu = byteArrayOf(0x01, 0x02, 0xA5.toByte(), 0x02)
        val bits = ModbusPdu.parseBitsResponse(ReadFunction.READ_COILS, quantity = 10, pdu = pdu)
        assertArrayEquals(
            booleanArrayOf(true, false, true, false, false, true, false, true, false, true),
            bits,
        )
    }

    @Test
    fun parseRegistersDecodesUnsignedBigEndian16BitWords() {
        // 0x01A4 = 420 (4.20 bar), 0xFFFF = 65535 (unsigned, not -1)
        val pdu = byteArrayOf(0x03, 0x04, 0x01, 0xA4.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val regs = ModbusPdu.parseRegistersResponse(ReadFunction.READ_HOLDING_REGISTERS, quantity = 2, pdu = pdu)
        assertArrayEquals(intArrayOf(420, 65535), regs)
    }

    @Test
    fun exceptionResponseThrowsTypedExceptionAndIsNotLinkFailure() {
        val pdu = byteArrayOf(0x83.toByte(), 0x02) // FC03 exception 02 Illegal Data Address
        try {
            ModbusPdu.parseRegistersResponse(ReadFunction.READ_HOLDING_REGISTERS, quantity = 1, pdu = pdu)
            fail("Expected ModbusException.ExceptionResponse")
        } catch (e: ModbusException.ExceptionResponse) {
            assertEquals(0x03, e.functionCode)
            assertEquals(0x02, e.exceptionCode)
            assertFalse(e.isLinkFailure)
        }
    }

    @Test
    fun crc16MatchesStandardModbusRtuVector() {
        // Canonical CRC-16/MODBUS check value: ASCII "123456789" -> 0x4B37
        assertEquals(0x4B37, Crc16.compute("123456789".toByteArray(Charsets.US_ASCII)))

        // Unit 1, FC03, address 0x0002, quantity 0x0001 -> CRC = 0xCA25 (wire: 25 CA)
        val frame = byteArrayOf(0x01, 0x03, 0x00, 0x02, 0x00, 0x01)
        val withCrc = Crc16.append(frame)
        assertEquals("01 03 00 02 00 01 25 CA", withCrc.toHex())
        assertTrue(Crc16.isValid(withCrc))

        val corrupted = withCrc.copyOf().also { it[3] = 0x03 }
        assertFalse(Crc16.isValid(corrupted))
    }
}

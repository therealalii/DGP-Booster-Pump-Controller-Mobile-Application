package com.dgp.boosterpump.modbus

/**
 * Hardware-verified Modbus write function codes supported by DGP BPC02.
 *
 * Verified in BPC02_WRITE_TEST_RESULTS.txt (COM4, 9600 8N1, Slave ID 1):
 *  - FC05 (Write Single Coil) verified on Coil 4101 (1 -> 0 -> 1).
 *  - FC06 (Write Single Register) verified on Holding Register 4175 (40 -> 41 -> 40).
 *  - FC15 (0x0F) and FC16 (0x10) are deliberately omitted: BPC02 firmware rejects both
 *    with 4-byte frames (01 0F 40 24 and 01 10 01 EC).
 */
enum class WriteFunction(val code: Int, val label: String) {
    WRITE_SINGLE_COIL(0x05, "FC05 Write Single Coil"),
    WRITE_SINGLE_REGISTER(0x06, "FC06 Write Single Register"),
}

package com.dgp.boosterpump.modbus

/**
 * Separate write-only Modbus client implementing ONLY the hardware-verified BPC02 single
 * write operations: FC05 (Write Single Coil) and FC06 (Write Single Holding Register).
 *
 * Callers must enforce register-map permissions, range checks, and explicit user confirmation
 * before invoking these methods.
 */
class ModbusWriteClient(
    private val transport: ModbusTransport,
    private val unitId: Int,
    private val clock: () -> Long = System::currentTimeMillis,
    private val onTransaction: (TransactionRecord) -> Unit = {},
) {
    init {
        require(unitId in 0..255) { "Unit ID $unitId outside 0..255" }
    }

    suspend fun writeSingleCoil(address: Int, value: Boolean) {
        val request = ModbusPdu.buildWriteSingleCoilRequest(address, value)
        val response = exchange(WriteFunction.WRITE_SINGLE_COIL, address, request)
        ModbusPdu.parseWriteSingleCoilResponse(address, value, response)
    }

    suspend fun writeSingleRegister(address: Int, value: Int) {
        val request = ModbusPdu.buildWriteSingleRegisterRequest(address, value)
        val response = exchange(WriteFunction.WRITE_SINGLE_REGISTER, address, request)
        ModbusPdu.parseWriteSingleRegisterResponse(address, value, response)
    }

    private suspend fun exchange(function: WriteFunction, address: Int, request: ByteArray): ByteArray {
        val start = clock()
        var response: ByteArray? = null
        try {
            response = transport.transact(unitId, request)
            ModbusPdu.checkForException(function.code, response)
            onTransaction(record(start, function.code, address, request, response, null))
            return response
        } catch (e: ModbusException) {
            onTransaction(record(start, function.code, address, request, response, e.message))
            throw e
        }
    }

    private fun record(
        start: Long,
        fc: Int,
        address: Int,
        request: ByteArray,
        response: ByteArray?,
        error: String?,
    ) = TransactionRecord(
        timestampMillis = start,
        unitId = unitId,
        functionCode = fc,
        isWrite = true,
        pduAddress = address,
        quantity = 1,
        requestHex = request.toHex(),
        responseHex = response?.toHex(),
        durationMs = clock() - start,
        error = error,
    )
}

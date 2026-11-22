package com.dgp.boosterpump.modbus

/** One request/response exchange (read or verified write), kept for the diagnostics terminal log. */
data class TransactionRecord(
    val timestampMillis: Long,
    val unitId: Int,
    val functionCode: Int,
    val isWrite: Boolean,
    val pduAddress: Int,
    val quantity: Int,
    val requestHex: String,
    val responseHex: String?,
    val durationMs: Long,
    val error: String?,
)

/**
 * Read-only Modbus client (FC01-FC04). Kept strictly separate from [ModbusWriteClient] so
 * polling and diagnostic reads can never construct a write frame.
 */
class ModbusReadClient(
    private val transport: ModbusTransport,
    private val unitId: Int,
    private val clock: () -> Long = System::currentTimeMillis,
    private val onTransaction: (TransactionRecord) -> Unit = {},
) {
    init {
        require(unitId in 0..255) { "Unit ID $unitId outside 0..255" }
    }

    suspend fun readBits(function: ReadFunction, address: Int, quantity: Int): BooleanArray {
        require(function.isBitAccess) { "${function.label} is not a bit read" }
        val response = exchange(function, address, quantity)
        return ModbusPdu.parseBitsResponse(function, quantity, response)
    }

    suspend fun readRegisters(function: ReadFunction, address: Int, quantity: Int): IntArray {
        require(!function.isBitAccess) { "${function.label} is not a register read" }
        val response = exchange(function, address, quantity)
        return ModbusPdu.parseRegistersResponse(function, quantity, response)
    }

    private suspend fun exchange(function: ReadFunction, address: Int, quantity: Int): ByteArray {
        val request = ModbusPdu.buildReadRequest(function, address, quantity)
        val start = clock()
        var response: ByteArray? = null
        try {
            response = transport.transact(unitId, request)
            ModbusPdu.checkForException(function, response)
            onTransaction(record(start, function.code, address, quantity, request, response, null))
            return response
        } catch (e: ModbusException) {
            onTransaction(record(start, function.code, address, quantity, request, response, e.message))
            throw e
        }
    }

    private fun record(
        start: Long,
        fc: Int,
        address: Int,
        quantity: Int,
        request: ByteArray,
        response: ByteArray?,
        error: String?,
    ) = TransactionRecord(
        timestampMillis = start,
        unitId = unitId,
        functionCode = fc,
        isWrite = false,
        pduAddress = address,
        quantity = quantity,
        requestHex = request.toHex(),
        responseHex = response?.toHex(),
        durationMs = clock() - start,
        error = error,
    )
}

package com.dgp.boosterpump.modbus

/** One request/response exchange, kept for the diagnostics screen and hardware test records. */
data class TransactionRecord(
    val timestampMillis: Long,
    val unitId: Int,
    val function: ReadFunction,
    val pduAddress: Int,
    val quantity: Int,
    val requestHex: String,
    val responseHex: String?,
    val durationMs: Long,
    val error: String?,
)

/**
 * Read-only Modbus client. Exposes FC01-FC04 only; there is no write method by design.
 *
 * @param onTransaction invoked after every exchange (success or failure) with the raw PDUs,
 * so field engineers can record exact requests/responses during hardware verification.
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
            onTransaction(record(start, function, address, quantity, request, response, null))
            return response
        } catch (e: ModbusException) {
            onTransaction(record(start, function, address, quantity, request, response, e.message))
            throw e
        }
    }

    private fun record(
        start: Long,
        function: ReadFunction,
        address: Int,
        quantity: Int,
        request: ByteArray,
        response: ByteArray?,
        error: String?,
    ) = TransactionRecord(
        timestampMillis = start,
        unitId = unitId,
        function = function,
        pduAddress = address,
        quantity = quantity,
        requestHex = request.toHex(),
        responseHex = response?.toHex(),
        durationMs = clock() - start,
        error = error,
    )
}

package com.dgp.boosterpump.controller

import com.dgp.boosterpump.controller.register.ReadBlock
import com.dgp.boosterpump.controller.register.RegisterDefinition
import com.dgp.boosterpump.controller.register.VendorTable
import com.dgp.boosterpump.gateway.ProtocolMapping
import com.dgp.boosterpump.modbus.ModbusException
import com.dgp.boosterpump.modbus.ModbusReadClient
import kotlinx.coroutines.delay

/**
 * Translates documented vendor addresses into Modbus reads using the configured
 * [ProtocolMapping]. Read-only by construction ([ModbusReadClient] has no write methods).
 *
 * Modbus exception responses for a block are recorded and the remaining blocks are still
 * read; link failures propagate so the caller can reconnect.
 */
class ControllerReader(
    private val client: ModbusReadClient,
    private val mapping: ProtocolMapping,
) {
    suspend fun readBlocks(blocks: List<ReadBlock>, interBlockDelayMs: Long = 0L): RawReadings {
        val bits = mutableMapOf<Int, Boolean>()
        val words = mutableMapOf<Int, Int>()
        val errors = mutableListOf<BlockError>()
        for ((index, block) in blocks.withIndex()) {
            if (index > 0 && interBlockDelayMs > 0L) {
                delay(interBlockDelayMs)
            }
            try {
                readBlock(block, bits, words)
            } catch (e: ModbusException) {
                if (e.isLinkFailure) throw e
                errors += BlockError(block, e.message ?: e.toString())
            }
        }
        return RawReadings(bits, words, errors)
    }

    /** Reads each definition individually (used for on-demand configuration readout). */
    suspend fun readEach(definitions: List<RegisterDefinition>): RawReadings =
        readBlocks(definitions.map { ReadBlock(it.table, it.documentedAddress, 1) })

    private suspend fun readBlock(block: ReadBlock, bits: MutableMap<Int, Boolean>, words: MutableMap<Int, Int>) {
        val function = mapping.functionFor(block.table)
        val pduStart = mapping.pduAddress(block.startAddress)
        if (pduStart < 0) {
            throw ModbusException.ExceptionResponse(function.code, 0x02)
        }
        when (block.table) {
            VendorTable.BIT -> {
                val values = client.readBits(function, pduStart, block.count)
                values.forEachIndexed { i, v -> bits[block.startAddress + i] = v }
            }
            VendorTable.WORD -> {
                val values = client.readRegisters(function, pduStart, block.count)
                values.forEachIndexed { i, v -> words[block.startAddress + i] = v }
            }
        }
    }
}

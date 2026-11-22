package com.dgp.boosterpump.simulation

import com.dgp.boosterpump.controller.SystemErrorBit
import com.dgp.boosterpump.controller.register.AccessPolicy
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.VendorTable
import com.dgp.boosterpump.gateway.ProtocolMapping
import com.dgp.boosterpump.modbus.ModbusException
import com.dgp.boosterpump.modbus.ModbusTransport
import com.dgp.boosterpump.modbus.ReadFunction
import com.dgp.boosterpump.modbus.WriteFunction
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.math.sin

/** Selectable scenarios for testing UI states and alarm handling without hardware. */
enum class SimulationScenario(val label: String, val description: String) {
    NORMAL_DUTY(
        "Normal duty (1-2 pumps)",
        "Steady ~4.2 bar, Pump 1 running on drive, Pump 2 standby, healthy 3-phase supply, tank OK.",
    ),
    PEAK_DEMAND(
        "Peak demand (3 pumps)",
        "Higher flow at ~3.8 bar with Pumps 1-3 running and higher drive frequency.",
    ),
    FLOATER_LOW_ALARM(
        "Low-tank floater fault",
        "Floater L & H disconnected, System Error bit 3 active, all pumps stopped.",
    ),
    PUMP_AND_DRIVE_FAULT(
        "Pump 2 & drive fault",
        "Drive fail (bit 25), Pump 2 fail (bit 27), max voltage & max pressure errors active.",
    ),
    LINK_TIMEOUT(
        "Simulated timeout",
        "Simulates a non-responsive RS485 bus to test timeout handling and error UI.",
    ),
}

/**
 * In-memory [ModbusTransport] simulating a DGP BPC02 across FC01, FC03, and verified
 * hardware configuration writes (FC05, FC06). Also reproduces observed BPC02 exceptions
 * for unverified manual-control addresses (50-59 / 99).
 */
class SimulatedBpc02Transport(
    private val mapping: ProtocolMapping,
    private val scenarioProvider: () -> SimulationScenario,
    private val responseDelayMs: Long = 35L,
) : ModbusTransport {

    @Volatile
    private var open = false
    private var startMillis = 0L

    /** Mutable simulated hardware configuration mirrors real controller EEPROM state. */
    private val cfgBits = ConcurrentHashMap<Int, Boolean>().apply {
        put(4096, true)
        put(4097, true)
        put(4098, true)
        put(4099, true)
        put(4100, true)
        put(4101, true)
        put(4106, false) // Matches BPC02_FULL_READ_RESULTS.txt (0xD0)
        put(4107, false)
        put(4108, false)
        put(4109, false)
        put(4110, true) // NC
    }

    private val cfgWords = ConcurrentHashMap<Int, Int>().apply {
        put(4173, 0) // Control status
        put(4175, 40) // 0.40 bar sleep gap
        put(4177, 159) // 159 h
        put(4178, 33) // 3.3 s
        put(4179, 11) // 11 min
        put(4180, 0) // MFI1..4 Disable
        put(4181, 0)
        put(4182, 0)
        put(4183, 0)
        put(4184, 0) // MFO1..3 Disable
        put(4185, 0)
        put(4186, 0)
        put(4195, 1) // Modbus Slave ID
    }

    private val writableBitAddrs = Bpc02RegisterMap.configuration
        .filter { it.table == VendorTable.BIT && it.access == AccessPolicy.READ_WRITE_VERIFIED }
        .map { it.documentedAddress }.toSet()

    private val writableWordAddrs = Bpc02RegisterMap.configuration
        .filter { it.table == VendorTable.WORD && it.access == AccessPolicy.READ_WRITE_VERIFIED }
        .associateBy { it.documentedAddress }

    override val isOpen: Boolean get() = open

    override suspend fun open() {
        delay(80)
        startMillis = System.currentTimeMillis()
        open = true
    }

    override fun close() {
        open = false
    }

    override suspend fun transact(unitId: Int, requestPdu: ByteArray): ByteArray {
        if (!open) throw ModbusException.LinkLost(null)
        val scenario = scenarioProvider()
        if (scenario == SimulationScenario.LINK_TIMEOUT) {
            delay(180)
            throw ModbusException.Timeout(1500)
        }
        if (responseDelayMs > 0) delay(responseDelayMs)

        if (requestPdu.size < 5) throw ModbusException.InvalidResponse("short request")
        val fcCode = requestPdu[0].toInt() and 0xFF
        val pduAddr = ((requestPdu[1].toInt() and 0xFF) shl 8) or (requestPdu[2].toInt() and 0xFF)
        val param = ((requestPdu[3].toInt() and 0xFF) shl 8) or (requestPdu[4].toInt() and 0xFF)
        val docAddr = pduAddr - mapping.addressOffset

        // Handle verified FC05 / FC06 writes
        if (fcCode == WriteFunction.WRITE_SINGLE_COIL.code) {
            if (param != 0xFF00 && param != 0x0000) return byteArrayOf(0x85.toByte(), 0x03)
            if (docAddr !in writableBitAddrs) return byteArrayOf(0x85.toByte(), 0x02)
            cfgBits[docAddr] = (param == 0xFF00)
            return requestPdu.copyOf()
        }
        if (fcCode == WriteFunction.WRITE_SINGLE_REGISTER.code) {
            if (docAddr == 99) {
                return if (param == 1234) requestPdu.copyOf() else byteArrayOf(0x86.toByte(), 0x03)
            }
            val def = writableWordAddrs[docAddr] ?: return byteArrayOf(0x86.toByte(), 0x02)
            val min = def.minValue ?: 0
            val max = def.maxValue ?: 0xFFFF
            if (param !in min..max) return byteArrayOf(0x86.toByte(), 0x03)
            cfgWords[docAddr] = param
            return requestPdu.copyOf()
        }

        val function = ReadFunction.fromCode(fcCode)
            ?: return byteArrayOf((fcCode or 0x80).toByte(), 0x01)
        val state = buildState(scenario, elapsedSeconds())

        return if (function.isBitAccess) {
            if (function != mapping.bitReadFunction) {
                return byteArrayOf((fcCode or 0x80).toByte(), 0x01)
            }
            encodeBitResponse(fcCode, docAddr, param, state.bits)
        } else {
            if (function != mapping.wordReadFunction) {
                return byteArrayOf((fcCode or 0x80).toByte(), 0x01)
            }
            encodeWordResponse(fcCode, docAddr, param, state.words)
        }
    }

    private fun encodeBitResponse(fcCode: Int, docStart: Int, qty: Int, table: Map<Int, Boolean>): ByteArray {
        for (i in 0 until qty) {
            if (!table.containsKey(docStart + i)) {
                return byteArrayOf((fcCode or 0x80).toByte(), 0x02)
            }
        }
        val byteCount = (qty + 7) / 8
        val out = ByteArray(2 + byteCount)
        out[0] = fcCode.toByte()
        out[1] = byteCount.toByte()
        for (i in 0 until qty) {
            if (table[docStart + i] == true) {
                out[2 + i / 8] = (out[2 + i / 8].toInt() or (1 shl (i % 8))).toByte()
            }
        }
        return out
    }

    private fun encodeWordResponse(fcCode: Int, docStart: Int, qty: Int, table: Map<Int, Int>): ByteArray {
        for (i in 0 until qty) {
            if (!table.containsKey(docStart + i)) {
                return byteArrayOf((fcCode or 0x80).toByte(), 0x02)
            }
        }
        val out = ByteArray(2 + qty * 2)
        out[0] = fcCode.toByte()
        out[1] = (qty * 2).toByte()
        for (i in 0 until qty) {
            val v = table[docStart + i] ?: 0
            out[2 + 2 * i] = (v ushr 8).toByte()
            out[3 + 2 * i] = v.toByte()
        }
        return out
    }

    private fun elapsedSeconds(): Long = ((System.currentTimeMillis() - startMillis) / 1000L).coerceAtLeast(0L)

    private data class SimState(val bits: Map<Int, Boolean>, val words: Map<Int, Int>)

    private fun buildState(scenario: SimulationScenario, tSec: Long): SimState {
        val ripple = sin(tSec * 0.4)
        val bits = mutableMapOf<Int, Boolean>()
        val words = mutableMapOf<Int, Int>()

        for (def in Bpc02RegisterMap.all) {
            when (def.table) {
                VendorTable.BIT -> bits[def.documentedAddress] = false
                VendorTable.WORD -> words[def.documentedAddress] = 0
            }
        }

        // Apply persistent simulated hardware configuration
        bits.putAll(cfgBits)
        words.putAll(cfgWords)

        // Baseline mains supply (3-phase 230/400 V, 50.00 Hz)
        val vShift = (ripple * 1.5).roundToInt()
        words[0] = 0
        words[3] = 231 + vShift
        words[4] = 229 - vShift
        words[5] = 230
        words[6] = 400 + vShift
        words[7] = 399
        words[8] = 401 - vShift
        words[9] = 5000 + (ripple * 4).roundToInt()

        fun setRunTime(pump: Int, baseHours: Int, baseMin: Int, baseSec: Int, running: Boolean) {
            val totalSec = baseSec + if (running) tSec.toInt() else 0
            val sec = totalSec % 60
            val totalMin = baseMin + totalSec / 60
            val min = totalMin % 60
            val hrs = baseHours + totalMin / 60
            val base = 34 + (pump - 1) * 3
            words[base] = sec
            words[base + 1] = min
            words[base + 2] = hrs
        }

        bits[32] = true // Floater L
        bits[33] = true // Floater H
        bits[10] = true // DR status

        when (scenario) {
            SimulationScenario.NORMAL_DUTY -> {
                words[2] = (420 + ripple * 8).roundToInt()
                words[10] = (980 + ripple * 25).roundToInt()
                bits[19] = cfgBits[4096] != false
                bits[0] = bits[19] == true
                bits[11] = bits[19] == true
                setRunTime(1, 2, 44, 40, running = bits[19] == true)
                setRunTime(2, 2, 42, 57, running = false)
                setRunTime(3, 2, 41, 53, running = false)
                setRunTime(4, 2, 40, 59, running = false)
                words[80] = 0
            }

            SimulationScenario.PEAK_DEMAND -> {
                words[2] = (385 + ripple * 12).roundToInt()
                words[10] = (1460 + ripple * 20).roundToInt()
                bits[19] = cfgBits[4096] != false
                bits[20] = cfgBits[4097] != false
                bits[21] = cfgBits[4098] != false
                bits[0] = bits[19] == true
                bits[1] = bits[20] == true
                bits[2] = bits[21] == true
                bits[11] = bits[19] == true
                bits[12] = bits[20] == true
                bits[13] = bits[21] == true
                bits[33] = false
                setRunTime(1, 2, 44, 40, running = bits[19] == true)
                setRunTime(2, 2, 42, 57, running = bits[20] == true)
                setRunTime(3, 2, 41, 53, running = bits[21] == true)
                setRunTime(4, 2, 40, 59, running = false)
                words[80] = 0
            }

            SimulationScenario.FLOATER_LOW_ALARM -> {
                words[2] = 115
                words[10] = 0
                bits[10] = false
                bits[32] = false
                bits[33] = false
                bits[15] = true
                setRunTime(1, 2, 44, 40, running = false)
                setRunTime(2, 2, 42, 57, running = false)
                setRunTime(3, 2, 41, 53, running = false)
                setRunTime(4, 2, 40, 59, running = false)
                words[80] = 1 shl SystemErrorBit.FLOATER.bit
            }

            SimulationScenario.PUMP_AND_DRIVE_FAULT -> {
                words[2] = 685
                words[3] = 254
                words[6] = 432
                words[10] = 0
                bits[19] = true
                bits[0] = true
                bits[11] = true
                bits[25] = true
                bits[27] = true
                setRunTime(1, 2, 44, 40, running = true)
                setRunTime(2, 2, 42, 57, running = false)
                setRunTime(3, 2, 41, 53, running = false)
                setRunTime(4, 2, 40, 59, running = false)
                words[80] = (1 shl SystemErrorBit.MAX_PRESSURE.bit) or
                    (1 shl SystemErrorBit.PHASE_MAX_VOLTAGE.bit)
            }

            SimulationScenario.LINK_TIMEOUT -> Unit
        }

        return SimState(bits, words)
    }
}

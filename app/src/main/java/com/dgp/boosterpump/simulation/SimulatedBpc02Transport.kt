package com.dgp.boosterpump.simulation

import com.dgp.boosterpump.controller.SystemErrorBit
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.VendorTable
import com.dgp.boosterpump.gateway.ProtocolMapping
import com.dgp.boosterpump.modbus.ModbusException
import com.dgp.boosterpump.modbus.ModbusTransport
import com.dgp.boosterpump.modbus.ReadFunction
import kotlinx.coroutines.delay
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
 * In-memory [ModbusTransport] that responds to standard Modbus read PDUs using the
 * documented BPC02 register addresses. Because it implements [ModbusTransport], the entire
 * [com.dgp.boosterpump.modbus.ModbusReadClient] and [com.dgp.boosterpump.controller.Bpc02Profile]
 * pipeline runs identically in simulation and live modes.
 *
 * Simulation values are clearly tagged as [com.dgp.boosterpump.gateway.DataSourceMode.SIMULATION]
 * by the session and never presented as live controller data.
 */
class SimulatedBpc02Transport(
    private val mapping: ProtocolMapping,
    private val scenarioProvider: () -> SimulationScenario,
    private val responseDelayMs: Long = 35L,
) : ModbusTransport {

    @Volatile
    private var open = false
    private var startMillis = 0L

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
        val function = ReadFunction.fromCode(fcCode)
            ?: return byteArrayOf((fcCode or 0x80).toByte(), 0x01)
        val pduAddr = ((requestPdu[1].toInt() and 0xFF) shl 8) or (requestPdu[2].toInt() and 0xFF)
        val qty = ((requestPdu[3].toInt() and 0xFF) shl 8) or (requestPdu[4].toInt() and 0xFF)

        val state = buildState(scenario, elapsedSeconds())
        val docStart = pduAddr - mapping.addressOffset

        return if (function.isBitAccess) {
            if (function != mapping.bitReadFunction) {
                return byteArrayOf((fcCode or 0x80).toByte(), 0x01)
            }
            encodeBitResponse(fcCode, docStart, qty, state.bits)
        } else {
            if (function != mapping.wordReadFunction) {
                return byteArrayOf((fcCode or 0x80).toByte(), 0x01)
            }
            encodeWordResponse(fcCode, docStart, qty, state.words)
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

        // Initialise every documented polled & config address to a baseline value
        for (def in Bpc02RegisterMap.polled + Bpc02RegisterMap.configuration) {
            when (def.table) {
                VendorTable.BIT -> bits[def.documentedAddress] = false
                VendorTable.WORD -> words[def.documentedAddress] = 0
            }
        }

        // Baseline hardware configuration (read-only on demand)
        bits[4096] = true // Pump 1 enabled
        bits[4097] = true // Pump 2 enabled
        bits[4098] = true // Pump 3 enabled
        bits[4099] = true // Pump 4 enabled
        bits[4106] = true // Control phase enabled
        bits[4107] = true // OFF changing enabled
        bits[4108] = true // Time changing enabled
        bits[4109] = false
        bits[4110] = false
        words[4173] = 1 // Control status
        words[4175] = 30 // 0.30 bar sleep gap
        words[4177] = 24 // 24 h
        words[4178] = 50 // 5.0 s
        words[4179] = 15 // 15 min
        words[4180] = 5 // MFI1: FL
        words[4181] = 3 // MFI2: EMG
        words[4182] = 0
        words[4183] = 0
        words[4184] = 3 // MFO1: Alarm
        words[4185] = 2 // MFO2: Fan
        words[4186] = 0
        words[4195] = 1 // Modbus address

        // Baseline mains supply (3-phase 230/400 V, 50.00 Hz)
        val vShift = (ripple * 1.5).roundToInt()
        words[0] = 1 // Displayed page number
        words[3] = 231 + vShift
        words[4] = 229 - vShift
        words[5] = 230
        words[6] = 400 + vShift
        words[7] = 399
        words[8] = 401 - vShift
        words[9] = 5000 + (ripple * 4).roundToInt() // 50.00 Hz (0.01 Hz scale)

        // Baseline run times (advancing gently for running pumps)
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

        // Tank floaters normally connected
        bits[32] = true // Floater L
        bits[33] = true // Floater H
        bits[10] = true // DR status

        when (scenario) {
            SimulationScenario.NORMAL_DUTY -> {
                words[2] = (420 + ripple * 8).roundToInt() // 4.20 bar
                words[10] = (980 + ripple * 25).roundToInt() // raw 0-1600 (scale unverified)
                bits[19] = true // Pump 1 running
                bits[0] = true // Output 0
                bits[11] = true // PR1
                setRunTime(1, 142, 35, 12, running = true)
                setRunTime(2, 138, 10, 44, running = false)
                setRunTime(3, 121, 52, 5, running = false)
                setRunTime(4, 119, 4, 30, running = false)
                words[80] = 0
            }

            SimulationScenario.PEAK_DEMAND -> {
                words[2] = (385 + ripple * 12).roundToInt() // 3.85 bar
                words[10] = (1460 + ripple * 20).roundToInt()
                bits[19] = true
                bits[20] = true
                bits[21] = true
                bits[0] = true
                bits[1] = true
                bits[2] = true
                bits[11] = true
                bits[12] = true
                bits[13] = true
                bits[33] = false // High floater open while drawing down
                setRunTime(1, 142, 35, 12, running = true)
                setRunTime(2, 138, 10, 44, running = true)
                setRunTime(3, 121, 52, 5, running = true)
                setRunTime(4, 119, 4, 30, running = false)
                words[80] = 0
            }

            SimulationScenario.FLOATER_LOW_ALARM -> {
                words[2] = 115 // 1.15 bar
                words[10] = 0
                bits[10] = false
                bits[32] = false // Floater L not connected
                bits[33] = false // Floater H not connected
                bits[15] = true // MFI1 triggered
                setRunTime(1, 142, 35, 12, running = false)
                setRunTime(2, 138, 10, 44, running = false)
                setRunTime(3, 121, 52, 5, running = false)
                setRunTime(4, 119, 4, 30, running = false)
                words[80] = 1 shl SystemErrorBit.FLOATER.bit
            }

            SimulationScenario.PUMP_AND_DRIVE_FAULT -> {
                words[2] = 685 // 6.85 bar
                words[3] = 254 // high phase voltage
                words[6] = 432
                words[10] = 0
                bits[19] = true // Pump 1 running direct
                bits[0] = true
                bits[11] = true
                bits[25] = true // Drive fail
                bits[27] = true // Pump 2 fail
                setRunTime(1, 142, 35, 12, running = true)
                setRunTime(2, 138, 10, 44, running = false)
                setRunTime(3, 121, 52, 5, running = false)
                setRunTime(4, 119, 4, 30, running = false)
                words[80] = (1 shl SystemErrorBit.MAX_PRESSURE.bit) or
                    (1 shl SystemErrorBit.PHASE_MAX_VOLTAGE.bit)
            }

            SimulationScenario.LINK_TIMEOUT -> Unit
        }

        return SimState(bits, words)
    }
}

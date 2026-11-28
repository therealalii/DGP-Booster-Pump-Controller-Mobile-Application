package com.dgp.boosterpump.simulation

import com.dgp.boosterpump.controller.SystemErrorBit
import com.dgp.boosterpump.controller.SystemErrorBit2
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
        "Steady ~4.2 bar (setpoint 4.50 bar), Pump 1 running on drive, Pump 4 deactivated (S), healthy 3-phase supply.",
    ),
    PEAK_DEMAND(
        "Peak demand (3 pumps)",
        "Higher flow at ~3.85 bar with Pumps 1-3 running and Drive 1 at ~47.5 Hz.",
    ),
    FLOATER_LOW_ALARM(
        "Low-tank floater fault",
        "Floater L & H disconnected, Word 80 bit 0 (FLOATER_ERROR) active, all pumps stopped.",
    ),
    PUMP_AND_DRIVE_FAULT(
        "Pump 2 & drive fault",
        "Drive fail (bit 25 / Word 80 bit 7), Pump 2 fail (bit 27 / Word 80 bit 9), max pressure & max voltage errors.",
    ),
    LINK_TIMEOUT(
        "Simulated timeout",
        "Simulates a non-responsive RS485 bus to test timeout handling and error UI.",
    ),
}

/**
 * Configurable station architecture for Simulation/Demo mode, wired directly into simulated
 * BPC-02 registers Word 4170 (`Wiring Mode`), Word 4133 (`Main`), and Word 4134 (`Drive`).
 */
data class SimulatedStationProfile(
    val wiringMode: Int = 2,
    val pumpCount: Int = 4,
    val driveCount: Int = 1,
) {
    fun normalized(): SimulatedStationProfile {
        val mode = wiringMode.coerceIn(1, 3)
        val maxPumps = if (mode == 1) 6 else 4
        val pumps = pumpCount.coerceIn(1, maxPumps)
        val drives = when (mode) {
            1 -> driveCount.coerceIn(0, minOf(2, pumps))
            else -> 1
        }
        return SimulatedStationProfile(wiringMode = mode, pumpCount = pumps, driveCount = drives)
    }
}

/**
 * In-memory [ModbusTransport] simulating a DGP BPC02 across all 154 registers of
 * Booster Pump Memory Map.xlsx, enforcing exact R vs RW permissions (returning Exception 0x02
 * on read-only registers and supporting FC06 Word 99 = 25 Reset Pump Error).
 */
class SimulatedBpc02Transport(
    private val mapping: ProtocolMapping,
    private val scenarioProvider: () -> SimulationScenario,
    private val profileProvider: () -> SimulatedStationProfile = { SimulatedStationProfile() },
    private val responseDelayMs: Long = 35L,
) : ModbusTransport {

    @Volatile
    private var open = false
    private var startMillis = 0L

    @Volatile
    private var faultsResetForScenario: SimulationScenario? = null

    /** Mutable simulated hardware configuration mirroring official Excel Bit addresses 4096..4110. */
    private val cfgBits = ConcurrentHashMap<Int, Boolean>().apply {
        put(4096, true) // Mode 2,3 Drive Activation (RW)
        put(4097, true) // Pump 1 Activation (RW)
        put(4098, true) // Pump 2 Activation (RW)
        put(4099, true) // Pump 3 Activation (RW)
        put(4100, true) // Pump 4 Activation (RW)
        put(4101, true) // Pump 5 Activation (RW)
        put(4102, true) // Pump 6 Activation (RW)
        put(4106, true) // Control Phase Activation (R)
        put(4107, true) // Off Changing Activation (R)
        put(4108, false) // Time Changing Activation (R)
        put(4109, false) // Reserve Auto Activation (R)
        put(4110, false) // Reserve Auto Type: 0 = NC, 1 = NO (R)
    }

    /** Mutable simulated hardware configuration mirroring official Excel Word addresses 4096..4210. */
    private val cfgWords = ConcurrentHashMap<Int, Int>().apply {
        put(4096, 450) // System Pressure Setpoint = 4.50 bar (RW)
        put(4097, 2) // On Delay Pump 1 = 2 s
        put(4098, 500) // OFF Pressure Pump 1 = 5.00 bar
        put(4099, 3) // OFF Delay Pump 1 = 3 s
        put(4100, 380) // On Pressure Pump 2 = 3.80 bar
        put(4101, 2)
        put(4102, 480)
        put(4103, 3)
        put(4104, 340) // On Pressure Pump 3
        put(4105, 2)
        put(4106, 460)
        put(4107, 3)
        put(4108, 300) // On Pressure Pump 4
        put(4109, 2)
        put(4110, 440)
        put(4111, 3)

        put(4122, 3) // Control Phase Type = 3P (R)
        put(4123, 190) // Min Voltage = 190 V (RW)
        put(4124, 255) // Max Voltage = 255 V (RW)
        put(4125, 5) // Control Phase Delay = 5 s (RW)
        put(4127, 10) // Sensitivity = 10% (RW)
        put(4128, 350) // Floater Trigger = 350 kOhm (RW)
        put(4129, 3) // Floater Delay = 3 s (RW)
        put(4131, 1) // Floater Type = Internal (R)
        put(4132, 2) // Pressure Sensor Output = 4~20mA (R)
        put(4133, 4) // Number Of Pump = 4 (R)
        put(4134, 1) // Number Of Variable Pump = 1 (R)
        put(4135, 0) // Jockey Type = Disable (RW)
        put(4136, 120) // P Value (RW)
        put(4137, 45) // I Value (RW)
        put(4138, 10) // D Value (RW)
        put(4142, 850) // Max Pressure = 8.50 bar (R)
        put(4143, 500) // Sensor Calibration (RW)
        put(4149, 50) // Wake up Differential = 0.50 bar (RW)
        put(4150, 2800) // Sleep Frequency = 28.00 Hz (RW)
        put(4151, 15) // Sleep Off Delay = 15 s (RW)
        put(4152, 1) // Pressure Sensor Input = 0-10 bar (R)
        put(4156, 30) // Mode 3 Switch Time = 0.30 s (R)
        put(4161, 60) // Time Changing Time = 60 min (RW)
        put(4162, 50) // Add Delay = 5.0 s (RW)
        put(4163, 50) // Remove Delay = 5.0 s (RW)
        put(4164, 2) // Wake up On Delay = 2 s (RW)
        put(4166, 85) // Constant Pump Percent = 85% (RW)
        put(4170, 2) // Wiring Mode = 2 (R)
        put(4173, 1) // Control Status = 1 (PID) (RW)
        put(4175, 40) // Sleep Gap = 0.40 bar (RW)
        put(4177, 159) // Auto Service Off Time = 159 h (RW)
        put(4178, 33) // Auto Service On Time = 3.3 s (RW)
        put(4179, 11) // Auto Service Interval = 11 min (RW)
        put(4180, 6) // MFI1 = FL (External Floater, R)
        put(4181, 4) // MFI2 = EMG (Emergency Stop, R)
        put(4182, 0) // MFI3 = Disable (R)
        put(4183, 0) // MFI4 = Disable (R)
        put(4184, 5) // MFO1 (OUT 7) = Alarm (R)
        put(4185, 7) // MFO2 (OUT 8) = P.Run (R)
        put(4186, 4) // MFO3 (OUT 9) = Fan (R)
        put(4187, 0) // MFO4 (OUT 10) = Disable (R)
        put(4188, 24081) // Serial Number (R)
        put(4189, 2500) // Start Frequency = 25.00 Hz (R)
        put(4190, 1) // Full Load Status = Enable (R)
        put(4191, 200) // Full Load Pressure = 2.00 bar (R)
        put(4192, 120) // Full Load Time = 120 s (R)
        put(4193, 2000) // Stop Frequency = 20.00 Hz (R)
        put(4194, 1) // Control Direction = Up (R)
        put(4195, 1) // ModBus Address = 1 (R)
        put(4206, 0) // AO1 Type = Pump1 (R)
        put(4207, 1) // AO2 Type = Pump2 (R)
        put(4208, 12) // LCD Contrast = 12 (R)
        put(4209, 1000) // AO1 Gain = 0.1000 (R)
        put(4210, 1000) // AO2 Gain = 0.1000 (R)
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
            throw ModbusException.Timeout(2500)
        }
        if (responseDelayMs > 0) delay(responseDelayMs)

        if (requestPdu.size < 5) throw ModbusException.InvalidResponse("short request")
        val fcCode = requestPdu[0].toInt() and 0xFF
        val pduAddr = ((requestPdu[1].toInt() and 0xFF) shl 8) or (requestPdu[2].toInt() and 0xFF)
        val param = ((requestPdu[3].toInt() and 0xFF) shl 8) or (requestPdu[4].toInt() and 0xFF)
        val docAddr = pduAddr - mapping.addressOffset

        // Handle verified FC05 / FC06 writes strictly per official RW policy
        if (fcCode == WriteFunction.WRITE_SINGLE_COIL.code) {
            if (param != 0xFF00 && param != 0x0000) return byteArrayOf(0x85.toByte(), 0x03)
            if (docAddr !in writableBitAddrs) return byteArrayOf(0x85.toByte(), 0x02)
            cfgBits[docAddr] = (param == 0xFF00)
            return requestPdu.copyOf()
        }
        if (fcCode == WriteFunction.WRITE_SINGLE_REGISTER.code) {
            if (docAddr == 99) {
                return if (param == Bpc02RegisterMap.CMD_RESET_PUMP_ERROR) {
                    faultsResetForScenario = scenario
                    requestPdu.copyOf()
                } else {
                    byteArrayOf(0x86.toByte(), 0x03)
                }
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

        // Apply persistent simulated hardware configuration + live station architecture profile
        bits.putAll(cfgBits)
        words.putAll(cfgWords)
        val profile = profileProvider().normalized()
        words[4170] = profile.wiringMode
        words[4133] = profile.pumpCount
        words[4134] = profile.driveCount

        val systemEnabled = (cfgWords[4173] ?: 1) != 0
        val hasDrive = profile.driveCount > 0
        val driveEnabled = hasDrive && cfgBits[4096] != false
        val pumpEnabled = { n: Int -> n <= profile.pumpCount && cfgBits[4096 + n] != false }
        val cleared = faultsResetForScenario == scenario

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
        words[99] = 0 // Command register auto-clears to 0

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
        bits[10] = systemEnabled && driveEnabled // DR status

        if (!systemEnabled) {
            words[2] = 210
            words[25] = 0
            words[26] = 0
            words[80] = 1 shl SystemErrorBit.SYSTEM_STOP_BY_USER.bit
            words[81] = 1 shl SystemErrorBit2.SYSTEM_STOPPED.bit
            setRunTime(1, 2, 44, 40, running = false)
            setRunTime(2, 2, 42, 57, running = false)
            setRunTime(3, 2, 41, 53, running = false)
            setRunTime(4, 2, 40, 59, running = false)
            return SimState(bits, words)
        }

        when (scenario) {
            SimulationScenario.NORMAL_DUTY -> {
                val p1Run = pumpEnabled(1) && (!hasDrive || driveEnabled)
                words[2] = (420 + ripple * 8).roundToInt()
                words[25] = if (p1Run && hasDrive) (3850 + ripple * 65).roundToInt() else 0 // ~38.50 Hz
                words[26] = 0
                bits[19] = p1Run
                bits[0] = p1Run
                bits[7] = p1Run // OUT 8 = MFO2 (P.Run)
                bits[11] = p1Run
                setRunTime(1, 2, 44, 40, running = p1Run)
                setRunTime(2, 2, 42, 57, running = false)
                setRunTime(3, 2, 41, 53, running = false)
                setRunTime(4, 2, 40, 59, running = false)
                words[80] = 0
                words[81] = 0
            }

            SimulationScenario.PEAK_DEMAND -> {
                val targetActive = when {
                    profile.pumpCount >= 5 -> 4
                    else -> minOf(3, profile.pumpCount)
                }
                val runningFlags = (1..6).map { n ->
                    n <= targetActive && pumpEnabled(n) && (n > 1 || !hasDrive || driveEnabled)
                }
                words[2] = (385 + ripple * 12).roundToInt()
                words[25] = if (runningFlags[0] && hasDrive) (4750 + ripple * 45).roundToInt() else 0 // ~47.50 Hz
                words[26] = if (profile.driveCount >= 2 && runningFlags[1]) (4320 + ripple * 40).roundToInt() else 0
                runningFlags.forEachIndexed { idx, run ->
                    bits[19 + idx] = run
                    if (idx < 6) bits[idx] = run
                    if (idx < 4) bits[11 + idx] = run
                }
                bits[7] = runningFlags.any { it } // OUT 8 = MFO2 (P.Run)
                bits[8] = true // OUT 9 = MFO3 (Fan)
                bits[33] = false
                setRunTime(1, 2, 44, 40, running = runningFlags[0])
                setRunTime(2, 2, 42, 57, running = runningFlags[1])
                setRunTime(3, 2, 41, 53, running = runningFlags[2])
                setRunTime(4, 2, 40, 59, running = runningFlags[3])
                words[80] = 0
                words[81] = 0
            }

            SimulationScenario.FLOATER_LOW_ALARM -> {
                words[2] = 115
                words[25] = 0
                words[26] = 0
                bits[10] = false
                bits[32] = false
                bits[33] = false
                bits[15] = true
                bits[6] = !cleared // OUT 7 = MFO1 (Alarm)
                setRunTime(1, 2, 44, 40, running = false)
                setRunTime(2, 2, 42, 57, running = false)
                setRunTime(3, 2, 41, 53, running = false)
                setRunTime(4, 2, 40, 59, running = false)
                words[80] = if (cleared) 0 else (1 shl SystemErrorBit.FLOATER.bit)
                words[81] = 0
            }

            SimulationScenario.PUMP_AND_DRIVE_FAULT -> {
                val faultPump = if (profile.pumpCount >= 2) 2 else 1
                val p1Run = faultPump != 1 && pumpEnabled(1)
                words[2] = 685
                words[3] = 254
                words[6] = 432
                words[25] = 0
                words[26] = 0
                bits[19] = p1Run
                bits[0] = p1Run
                bits[6] = !cleared // OUT 7 = MFO1 (Alarm)
                bits[7] = p1Run // OUT 8 = MFO2 (P.Run)
                bits[11] = p1Run
                bits[25] = !cleared && hasDrive // Drive fail
                bits[25 + faultPump] = !cleared // Faulted pump bit
                setRunTime(1, 2, 44, 40, running = p1Run)
                setRunTime(2, 2, 42, 57, running = false)
                setRunTime(3, 2, 41, 53, running = false)
                setRunTime(4, 2, 40, 59, running = false)
                words[80] = if (cleared) {
                    0
                } else {
                    val pumpBit = if (faultPump == 2) SystemErrorBit.PUMP2.bit else SystemErrorBit.PUMP1.bit
                    val driveMask = if (hasDrive) (1 shl SystemErrorBit.DRIVE.bit) else 0
                    (1 shl SystemErrorBit.MAX_PRESSURE.bit) or
                        (1 shl SystemErrorBit.PHASE_MAX_VOLTAGE.bit) or
                        driveMask or
                        (1 shl pumpBit)
                }
                words[81] = 0
            }

            SimulationScenario.LINK_TIMEOUT -> Unit
        }

        return SimState(bits, words)
    }
}

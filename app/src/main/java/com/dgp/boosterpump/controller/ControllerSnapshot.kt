package com.dgp.boosterpump.controller

import com.dgp.boosterpump.controller.register.RegisterDefinition
import com.dgp.boosterpump.gateway.DataSourceMode

/**
 * Official bits of BPC02 "System Error" Word 1 (Word address 80, Booster Pump Memory Map.xlsx).
 */
enum class SystemErrorBit(
    val bit: Int,
    val label: String,
    val hint: String,
    val severity: AlarmSeverity = AlarmSeverity.FAULT,
) {
    FLOATER(0, "FLOATER_ERROR", "Storage tank water level below the configured limit."),
    MAX_PRESSURE(1, "MAX_PRESSURE_ERROR", "System pressure exceeded the configured maximum."),
    PRESSURE_SENSOR_DISCONNECTED(2, "SENSOR_DISCONNECT_ERROR", "Pressure sensor cable disconnected or out of range."),
    PHASE_SENSITIVITY(3, "CP_SENSIVITY_ERROR", "R/S/T phase imbalance exceeded the configured limit."),
    PHASE_MAX_VOLTAGE(4, "CP_MAX_ERROR", "Input voltage exceeded the configured maximum."),
    PHASE_MIN_VOLTAGE(5, "CP_MIN_ERROR", "Input voltage dropped below the configured minimum."),
    PHASE_SEQUENCE(6, "CP_SQUENCY_ERROR", "Input phases R/S/T sequence is reversed."),
    DRIVE(7, "DRIVE_ERROR", "Variable frequency drive reported a fault."),
    PUMP1(8, "PUMP1_ERROR", "Pump 1 thermal/protection fault."),
    PUMP2(9, "PUMP2_ERROR", "Pump 2 thermal/protection fault."),
    PUMP3(10, "PUMP3_ERROR", "Pump 3 thermal/protection fault."),
    PUMP4(11, "PUMP4_ERROR", "Pump 4 thermal/protection fault."),
    PUMP5(12, "PUMP5_ERROR", "Pump 5 thermal/protection fault."),
    PUMP6(13, "PUMP6_ERROR", "Pump 6 thermal/protection fault."),
    ALL_PUMP(14, "ALL_PUMP_ERROR", "All pumps are in fault state."),
    SYSTEM_STOP_BY_USER(15, "SYSTEM_STOP_BY_USER", "Booster pump system stopped by user.", AlarmSeverity.WARNING),
    ;

    companion object {
        val documentedMask: Int = entries.fold(0) { acc, e -> acc or (1 shl e.bit) }
    }
}

/**
 * Official bits of BPC02 "System Error" Word 2 (Word address 81, Booster Pump Memory Map.xlsx).
 * Bits 3 and 6 are marked RESERVE in the official map.
 */
enum class SystemErrorBit2(
    val bit: Int,
    val label: String,
    val hint: String,
    val severity: AlarmSeverity = AlarmSeverity.FAULT,
) {
    MODE3_MAIN_PUMP(0, "MODE3_MAIN_PUMP_ERROR", "Mode 3 main pump fault."),
    MODE3_DRIVE(1, "MODE3_DRIVE_ERROR", "Mode 3 drive fault."),
    FULL_LOAD(2, "FULL_LOAD_ERROR", "System ran at full load beyond configured time without reaching pressure."),
    SYSTEM_STOPPED(4, "SYSTEM_STOPPED", "Controller is in stopped state.", AlarmSeverity.WARNING),
    EMG_STOPPED(5, "EMG_STOPPED", "Emergency stop (EMG) input triggered."),
    BATTERY_VOLTAGE(7, "BATTERY_VOLTAGE_ERROR", "Controller backup battery voltage error.", AlarmSeverity.WARNING),
    ;

    companion object {
        /** Bits 0..7 including reserved bits 3 and 6 are defined in Word 81. */
        val knownMask: Int = entries.fold(0) { acc, e -> acc or (1 shl e.bit) } or (1 shl 3) or (1 shl 6)
    }
}

data class PhaseVoltages(
    val rn: Int?,
    val sn: Int?,
    val tn: Int?,
    val rs: Int?,
    val st: Int?,
    val tr: Int?,
)

/** Pump run time as three separate documented registers (hours, minutes, seconds parts). */
data class RunTime(val hours: Int, val minutes: Int, val seconds: Int) {
    fun format(): String = "%d h %02d min %02d s".format(hours, minutes, seconds)
}

data class PumpState(
    val number: Int,
    val running: Boolean?,
    val failed: Boolean?,
    val enabled: Boolean? = null,
    val runTime: RunTime?,
    val hasRunTimeRegisters: Boolean,
)

data class RegisterReading(val definition: RegisterDefinition, val raw: Int?)

enum class AlarmSeverity { FAULT, WARNING }

data class Alarm(val label: String, val detail: String, val severity: AlarmSeverity)

/**
 * A decoded, immutable view of one poll cycle. Null fields mean "not read" (block failed or
 * not available) and must be shown as unavailable, never as zero.
 */
data class ControllerSnapshot(
    val model: ControllerModel,
    val source: DataSourceMode,
    val timestampMillis: Long,
    val pressureBar: Double?,
    val pressureSetpointBar: Double? = null,
    val controlStatus: Int? = null,
    val voltages: PhaseVoltages,
    val mainsFrequencyHz: Double?,
    val drive1FrequencyHz: Double? = null,
    val drive2FrequencyHz: Double? = null,
    val displayedPage: Int?,
    val pumps: List<PumpState>,
    val driveEnabled: Boolean? = null,
    val driveFailed: Boolean?,
    val drStatus: Boolean?,
    val outputs: List<Boolean?>,
    val prInputs: List<Boolean?>,
    val mfiInputs: List<Boolean?>,
    val floaterLowConnected: Boolean?,
    val floaterHighConnected: Boolean?,
    val systemErrorWord: Int?,
    val systemErrorWord2: Int? = null,
    val readings: List<RegisterReading>,
    val blockErrors: List<BlockError>,
) {
    val isSimulated: Boolean get() = source == DataSourceMode.SIMULATION

    val activeSystemErrors: List<SystemErrorBit>
        get() = systemErrorWord?.let { w -> SystemErrorBit.entries.filter { (w ushr it.bit) and 1 == 1 } }.orEmpty()

    val activeSystemErrors2: List<SystemErrorBit2>
        get() = systemErrorWord2?.let { w -> SystemErrorBit2.entries.filter { (w ushr it.bit) and 1 == 1 } }.orEmpty()

    /** Bits set in Word 80 that are not documented (all 16 bits are documented in official Excel). */
    val undocumentedErrorBits: List<Int>
        get() = systemErrorWord?.let { w ->
            (0 until 16).filter { (w ushr it) and 1 == 1 && (SystemErrorBit.documentedMask ushr it) and 1 == 0 }
        }.orEmpty()

    /** Bits set in Word 81 outside documented bits 0..7. */
    val undocumentedErrorBits2: List<Int>
        get() = systemErrorWord2?.let { w ->
            (0 until 16).filter { (w ushr it) and 1 == 1 && (SystemErrorBit2.knownMask ushr it) and 1 == 0 }
        }.orEmpty()

    val runningPumpCount: Int get() = pumps.count { it.running == true }

    /** Number of pumps manually deactivated ('S' on controller LCD, Coil 4097..4102 == 0). */
    val disabledPumpCount: Int get() = pumps.count { it.enabled == false }

    val alarms: List<Alarm>
        get() = buildList {
            activeSystemErrors.forEach {
                add(Alarm(it.label, "Word 80 bit ${it.bit}. ${it.hint}", it.severity))
            }
            activeSystemErrors2.forEach {
                add(Alarm(it.label, "Word 81 bit ${it.bit}. ${it.hint}", it.severity))
            }
            undocumentedErrorBits.forEach {
                add(Alarm("Undocumented error bit $it", "Bit $it of Word 80 is set.", AlarmSeverity.WARNING))
            }
            undocumentedErrorBits2.forEach {
                add(Alarm("Undocumented error word2 bit $it", "Bit $it of Word 81 is set.", AlarmSeverity.WARNING))
            }
            // Also surface coil-level Drive/Pump failure flags when not already reported by Word 80
            if (driveFailed == true && SystemErrorBit.DRIVE !in activeSystemErrors) {
                add(Alarm("Drive fail", "Coil 25 reports drive failure.", AlarmSeverity.FAULT))
            }
            pumps.filter { it.failed == true }.forEach { pump ->
                val wordBit = when (pump.number) {
                    1 -> SystemErrorBit.PUMP1
                    2 -> SystemErrorBit.PUMP2
                    3 -> SystemErrorBit.PUMP3
                    4 -> SystemErrorBit.PUMP4
                    5 -> SystemErrorBit.PUMP5
                    6 -> SystemErrorBit.PUMP6
                    else -> null
                }
                if (wordBit == null || wordBit !in activeSystemErrors) {
                    add(Alarm("Pump ${pump.number} fail", "Coil ${25 + pump.number} reports pump failure.", AlarmSeverity.FAULT))
                }
            }
        }
}

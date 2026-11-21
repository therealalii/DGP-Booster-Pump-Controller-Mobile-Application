package com.dgp.boosterpump.controller

import com.dgp.boosterpump.controller.register.RegisterDefinition
import com.dgp.boosterpump.gateway.DataSourceMode

/**
 * Documented bits of the BPC02 "System Error" word (Word address 80, manual p.27).
 * Bit numbering assumes bit 0 = least significant bit; confirm on hardware.
 */
enum class SystemErrorBit(val bit: Int, val label: String, val hint: String) {
    FLOATER(3, "Floater error", "Storage tank water level below the set limit (manual troubleshooting table)."),
    MAX_PRESSURE(4, "Max pressure error", "Water pressure exceeded the configured maximum."),
    PRESSURE_SENSOR_DISCONNECTED(5, "Pressure sensor disconnected", "Check sensor cable and terminals."),
    PHASE_SENSITIVITY(6, "Control phase sensitivity error", "R/S/T phase imbalance exceeded the configured limit."),
    PHASE_MAX_VOLTAGE(7, "Control phase max voltage error", "Input voltage above the configured limit."),
    PHASE_MIN_VOLTAGE(8, "Control phase min voltage error", "Input voltage below the configured limit."),
    PHASE_SEQUENCE(9, "Control phase sequence error", "Input phases R/S/T are swapped."),
    PUMP1(10, "Pump 1 error", "Listed in the manual as 'Pump1 Error'."),
    ;

    companion object {
        val documentedMask: Int = entries.fold(0) { acc, e -> acc or (1 shl e.bit) }
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
    val voltages: PhaseVoltages,
    val mainsFrequencyHz: Double?,
    val driveFrequencyRaw: Int?,
    val displayedPage: Int?,
    val pumps: List<PumpState>,
    val driveFailed: Boolean?,
    val drStatus: Boolean?,
    val outputs: List<Boolean?>,
    val prInputs: List<Boolean?>,
    val mfiInputs: List<Boolean?>,
    val floaterLowConnected: Boolean?,
    val floaterHighConnected: Boolean?,
    val systemErrorWord: Int?,
    val readings: List<RegisterReading>,
    val blockErrors: List<BlockError>,
) {
    val isSimulated: Boolean get() = source == DataSourceMode.SIMULATION

    val activeSystemErrors: List<SystemErrorBit>
        get() = systemErrorWord?.let { w -> SystemErrorBit.entries.filter { (w ushr it.bit) and 1 == 1 } }.orEmpty()

    /** Bits set in the error word that the manual does not describe. */
    val undocumentedErrorBits: List<Int>
        get() = systemErrorWord?.let { w ->
            (0 until 16).filter { (w ushr it) and 1 == 1 && (SystemErrorBit.documentedMask ushr it) and 1 == 0 }
        }.orEmpty()

    val runningPumpCount: Int get() = pumps.count { it.running == true }

    val alarms: List<Alarm>
        get() = buildList {
            activeSystemErrors.forEach { add(Alarm(it.label, "System error bit ${it.bit}. ${it.hint}", AlarmSeverity.FAULT)) }
            undocumentedErrorBits.forEach {
                add(Alarm("Undocumented error bit $it", "Bit $it of the system error word is set but not described in the manual.", AlarmSeverity.WARNING))
            }
            if (driveFailed == true) add(Alarm("Drive fail", "Bit address 25 reports drive failure.", AlarmSeverity.FAULT))
            pumps.filter { it.failed == true }.forEach {
                add(Alarm("Pump ${it.number} fail", "Bit address ${25 + it.number} reports pump failure.", AlarmSeverity.FAULT))
            }
        }
}

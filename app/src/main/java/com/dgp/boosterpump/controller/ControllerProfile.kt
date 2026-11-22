package com.dgp.boosterpump.controller

import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.ReadBlock
import com.dgp.boosterpump.controller.register.RegisterDefinition
import com.dgp.boosterpump.gateway.DataSourceMode

/** Supported controller families. */
enum class ControllerModel(val label: String, val isSupported: Boolean, val note: String) {
    BPC02("DGP BPC02", isSupported = true, note = "Register map from BPC-02 User Manual V1."),
    BPC04(
        "DGP BPC04",
        isSupported = false,
        note = "Planned. Requires the BPC-04 manual / DGP confirmation that its map matches BPC02.",
    ),
}

/** Values read from the controller, keyed by documented (vendor) address. */
data class RawReadings(
    val bits: Map<Int, Boolean> = emptyMap(),
    val words: Map<Int, Int> = emptyMap(),
    val blockErrors: List<BlockError> = emptyList(),
)

/** A read block that failed with a Modbus exception response (link still healthy). */
data class BlockError(val block: ReadBlock, val message: String)

/**
 * Model-specific knowledge: which registers exist, how to read them and how to turn raw
 * values into a [ControllerSnapshot]. Adding BPC04 means adding another implementation.
 */
interface ControllerProfile {
    val model: ControllerModel
    val polled: List<RegisterDefinition>
    val pollBlocks: List<ReadBlock>
    val configuration: List<RegisterDefinition>
    val configurationBlocks: List<ReadBlock>
    val manualControl: List<RegisterDefinition>
    fun decode(readings: RawReadings, source: DataSourceMode, timestampMillis: Long): ControllerSnapshot
}

object ControllerProfiles {
    fun forModel(model: ControllerModel): ControllerProfile = when (model) {
        ControllerModel.BPC02 -> Bpc02Profile
        ControllerModel.BPC04 -> throw UnsupportedOperationException(
            "BPC04 is not supported yet: no verified register map is available.",
        )
    }
}

/** BPC02 profile backed by [Bpc02RegisterMap]. */
object Bpc02Profile : ControllerProfile {
    private val map = Bpc02RegisterMap

    override val model = ControllerModel.BPC02
    override val polled = map.polled
    override val pollBlocks = map.pollBlocks
    override val configuration = map.configuration
    override val configurationBlocks = map.configurationBlocks
    override val manualControl = map.manualControl

    override fun decode(readings: RawReadings, source: DataSourceMode, timestampMillis: Long): ControllerSnapshot {
        fun raw(def: RegisterDefinition): Int? = when (def.table) {
            com.dgp.boosterpump.controller.register.VendorTable.BIT ->
                readings.bits[def.documentedAddress]?.let { if (it) 1 else 0 }
            com.dgp.boosterpump.controller.register.VendorTable.WORD -> readings.words[def.documentedAddress]
        }

        fun raw(key: String): Int? = raw(map.byKey(key))
        fun bit(key: String): Boolean? = raw(key)?.let { it != 0 }
        fun scaled(key: String): Double? {
            val def = map.byKey(key)
            val scale = def.scale ?: return null
            return raw(def)?.let { it * scale }
        }

        val pumps = (1..Bpc02RegisterMap.STATUS_PUMP_COUNT).map { n ->
            val runTime = if (n <= Bpc02RegisterMap.RUN_TIME_PUMP_COUNT) {
                val h = raw(map.runHoursKey(n))
                val m = raw(map.runMinutesKey(n))
                val s = raw(map.runSecondsKey(n))
                if (h != null && m != null && s != null) RunTime(h, m, s) else null
            } else {
                null
            }
            PumpState(
                number = n,
                running = bit(map.pumpStatusKey(n)),
                failed = bit(map.pumpFailKey(n)),
                runTime = runTime,
                hasRunTimeRegisters = n <= Bpc02RegisterMap.RUN_TIME_PUMP_COUNT,
            )
        }

        return ControllerSnapshot(
            model = model,
            source = source,
            timestampMillis = timestampMillis,
            pressureBar = scaled(Bpc02RegisterMap.SYSTEM_PRESSURE),
            voltages = PhaseVoltages(
                rn = raw(Bpc02RegisterMap.V_RN),
                sn = raw(Bpc02RegisterMap.V_SN),
                tn = raw(Bpc02RegisterMap.V_TN),
                rs = raw(Bpc02RegisterMap.V_RS),
                st = raw(Bpc02RegisterMap.V_ST),
                tr = raw(Bpc02RegisterMap.V_TR),
            ),
            mainsFrequencyHz = scaled(Bpc02RegisterMap.MAINS_FREQUENCY),
            driveFrequencyRaw = raw(Bpc02RegisterMap.DRIVE_FREQUENCY),
            displayedPage = raw(Bpc02RegisterMap.DISPLAYED_PAGE),
            pumps = pumps,
            driveFailed = bit(Bpc02RegisterMap.DRIVE_FAIL),
            drStatus = bit(Bpc02RegisterMap.DR_STATUS),
            outputs = (0 until Bpc02RegisterMap.OUTPUT_COUNT).map { bit(map.outputKey(it)) },
            prInputs = (1..Bpc02RegisterMap.PR_INPUT_COUNT).map { bit(map.prKey(it)) },
            mfiInputs = (1..Bpc02RegisterMap.MFI_INPUT_COUNT).map { bit(map.mfiKey(it)) },
            floaterLowConnected = bit(Bpc02RegisterMap.FLOATER_LOW),
            floaterHighConnected = bit(Bpc02RegisterMap.FLOATER_HIGH),
            systemErrorWord = raw(Bpc02RegisterMap.SYSTEM_ERROR),
            readings = polled.map { RegisterReading(it, raw(it)) },
            blockErrors = readings.blockErrors,
        )
    }
}

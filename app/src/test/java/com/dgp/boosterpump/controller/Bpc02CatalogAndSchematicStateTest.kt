package com.dgp.boosterpump.controller

import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.gateway.GatewayConfig
import com.dgp.boosterpump.session.ControllerSession
import com.dgp.boosterpump.simulation.SimulatedStationProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Bpc02CatalogAndSchematicStateTest {

    @Test
    fun catalogMenuCoversAll82ConfigurationRegistersWithEnglishNamesAndPersianHelp() {
        val allMenuKeys = Bpc02Catalog.allMenuRegisterKeys

        val configKeys = Bpc02RegisterMap.configuration.map { it.key }.toSet()
        assertEquals("Every config register must appear once without duplicates", configKeys.size, allMenuKeys.size)
        assertEquals(configKeys, allMenuKeys.toSet())

        val persianRegex = Regex("[\\u0600-\\u06FF]")
        for (def in Bpc02RegisterMap.configuration) {
            val help = Bpc02Catalog.helpFor(def)
            assertTrue("${def.key} English LCD name must be non-blank", help.englishName.isNotBlank())
            assertTrue("${def.key} full English name must be non-blank", help.fullEnglishName.isNotBlank())
            assertTrue(
                "${def.key} English name must not contain Persian script: ${help.englishName}",
                !persianRegex.containsMatchIn(help.englishName),
            )
            assertTrue(
                "${def.key} Persian explanation must contain Persian text",
                persianRegex.containsMatchIn(help.helpFa),
            )
            assertTrue(
                "${def.key} Persian explanation must be informative",
                help.helpFa.length >= 25,
            )
        }
    }

    @Test
    fun physicalControllerMenuOrderOnOffPagesModeTitleAndMfxOptionsMatchSpec() {
        // Exact top-level Menu order from controller-pics-new/photo_6
        assertEquals(
            listOf("System", "Input", "Time/Date", "Hardware"),
            Bpc02Catalog.MainMenuSection.entries.map { it.title },
        )

        // Exact 8 Hardware categories from controller-pics-new/photo_9 & photo_10
        assertEquals(
            listOf(
                "Control Phase",
                "Floater",
                "Sensor",
                "Control",
                "MFx",
                "Serial Number",
                "Network",
                "Reset to Factory",
            ),
            Bpc02Catalog.MenuCategory.entries.map { it.title },
        )

        // Exact ON/OFF Pump 1..4 pages & field order from photo_1, photo_2, photo_7:
        // On Delay -> Off Delay -> On Pressure -> Off Pressure
        val onOffPages = Bpc02Catalog.MenuCategory.CONTROL.pages
            .filter { it.controlMode == Bpc02Catalog.ControlPageMode.ON_OFF_ONLY }
        assertEquals(listOf("Pump 1", "Pump 2", "Pump 3", "Pump 4"), onOffPages.map { it.title })
        assertEquals(
            listOf("pump_1_on_delay", "pump_1_off_delay", Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT, "pump_1_off_pressure"),
            onOffPages[0].displayRegisterKeys,
        )
        assertEquals(
            listOf("pump_4_on_delay", "pump_4_off_delay", "pump_4_on_pressure", "pump_4_off_pressure"),
            onOffPages[3].displayRegisterKeys,
        )

        // Mode 1 title includes pump & drive counts instead of old "(Independent VFD / DOL)"
        val mode1Title = Bpc02Catalog.wiringModeArchitecture(1, pumpCount = 4, driveCount = 2)
        assertTrue(mode1Title.contains("Mode 1"))
        assertTrue(mode1Title.contains("تعداد پمپ"))
        assertTrue(mode1Title.contains("تعداد درایو"))

        // Allowed MFI (11) & MFO (14) read-only options
        assertEquals(11, Bpc02Catalog.mfiAllowedOptions.size)
        assertEquals(14, Bpc02Catalog.mfoAllowedOptions.size)
        assertEquals("FL", Bpc02Catalog.mfiOptionLabel(6))
        assertEquals("Alarm", Bpc02Catalog.mfoOptionLabel(5))
        assertEquals("P.Run", Bpc02Catalog.mfoOptionLabel(7))
    }

    @Test
    fun visiblePumpsAdaptsDynamicallyToExactPumpCountAcrossModes1Through3() {
        fun decodeFor(mode: Int, pumps: Int, drives: Int): ControllerSnapshot {
            val raw = RawReadings(
                bits = mapOf(
                    19 to true,
                    32 to true,
                    33 to true,
                    4096 to true,
                    4097 to true,
                    4098 to true,
                    4099 to true,
                    4100 to true,
                    4101 to true,
                    4102 to true,
                ),
                words = mapOf(
                    2 to 420,
                    4096 to 450,
                    4133 to pumps,
                    4134 to drives,
                    4170 to mode,
                    4173 to 1,
                ),
            )
            return Bpc02Profile.decode(raw, DataSourceMode.LIVE, timestampMillis = 1000L)
        }

        // Mode 1: supports 1..6 pumps without double-counting drive count
        assertEquals(1, decodeFor(mode = 1, pumps = 1, drives = 1).visiblePumps.size)
        assertEquals(2, decodeFor(mode = 1, pumps = 2, drives = 1).visiblePumps.size)
        assertEquals(2, decodeFor(mode = 1, pumps = 2, drives = 2).visiblePumps.size)
        assertEquals(3, decodeFor(mode = 1, pumps = 3, drives = 1).visiblePumps.size)
        assertEquals(4, decodeFor(mode = 1, pumps = 4, drives = 1).visiblePumps.size)
        assertEquals(6, decodeFor(mode = 1, pumps = 6, drives = 2).visiblePumps.size)

        // Mode 2 & Mode 3: support 1..4 pumps
        assertEquals(1, decodeFor(mode = 2, pumps = 1, drives = 1).visiblePumps.size)
        assertEquals(2, decodeFor(mode = 2, pumps = 2, drives = 1).visiblePumps.size)
        assertEquals(3, decodeFor(mode = 3, pumps = 3, drives = 1).visiblePumps.size)
        assertEquals(4, decodeFor(mode = 3, pumps = 4, drives = 1).visiblePumps.size)
        // Mode 2/3 clamps any out-of-mode >4 value safely to 4
        assertEquals(4, decodeFor(mode = 2, pumps = 6, drives = 1).visiblePumps.size)
    }

    @Test
    fun simulationProfileImmediatelyUpdatesLiveSessionPumpCountAndModeForSchematic() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val session = ControllerSession(scope = scope, ioDispatcher = Dispatchers.Default)
        try {
            session.connect(GatewayConfig(mode = DataSourceMode.SIMULATION, pollIntervalMs = 500))
            val initial = withTimeout(4000) { session.snapshot.first { it != null } }!!
            assertEquals(4, initial.visiblePumps.size)
            assertEquals(2, initial.wiringMode)

            // Switch to Mode 1 with 1 pump -> schematic receives exactly 1 pump immediately
            session.setSimulationProfile(SimulatedStationProfile(wiringMode = 1, pumpCount = 1, driveCount = 1))
            val onePump = withTimeout(3000) { session.snapshot.first { it?.visiblePumps?.size == 1 && it.wiringMode == 1 } }!!
            assertEquals(1, onePump.configuredPumpCount)
            assertEquals(1, onePump.variablePumpCount)

            // Switch to Mode 3 with 3 pumps -> schematic receives exactly 3 pumps
            session.setSimulationProfile(SimulatedStationProfile(wiringMode = 3, pumpCount = 3, driveCount = 1))
            val threePumps = withTimeout(3000) { session.snapshot.first { it?.visiblePumps?.size == 3 && it.wiringMode == 3 } }!!
            assertEquals(3, threePumps.configuredPumpCount)
            assertEquals(1, threePumps.variablePumpCount)

            // Switch to Mode 1 with 6 pumps and 2 drives -> schematic receives all 6 pumps
            session.setSimulationProfile(SimulatedStationProfile(wiringMode = 1, pumpCount = 6, driveCount = 2))
            val sixPumps = withTimeout(3000) { session.snapshot.first { it?.visiblePumps?.size == 6 && it.variablePumpCount == 2 } }!!
            assertEquals(6, sixPumps.visiblePumps.size)
        } finally {
            session.disconnect()
            scope.cancel()
        }
    }

    @Test
    fun hydraulicFlowSpeedScalesProportionallyWithDriveFrequencyAndActivePumps() {
        fun snapshotFor(runningPumps: Int, driveHz: Double, lowWater: Boolean = false): ControllerSnapshot {
            val bits = buildMap {
                for (i in 1..4) {
                    put(18 + i, i <= runningPumps)
                    put(4096 + i, true)
                }
                put(4096, true)
                put(32, !lowWater)
                put(33, !lowWater)
            }
            val words = mapOf(
                2 to 420,
                25 to (driveHz * 100).toInt(),
                4096 to 450,
                4133 to 4,
                4134 to 1,
                4170 to 2,
                4173 to 1,
            )
            return Bpc02Profile.decode(RawReadings(bits = bits, words = words), DataSourceMode.LIVE, timestampMillis = 1000L)
        }

        val idle = com.dgp.boosterpump.ui.components.computeHydraulicFlowSpeedFactor(snapshotFor(runningPumps = 0, driveHz = 0.0))
        val lowWater = com.dgp.boosterpump.ui.components.computeHydraulicFlowSpeedFactor(snapshotFor(runningPumps = 2, driveHz = 45.0, lowWater = true))
        val onePumpLowHz = com.dgp.boosterpump.ui.components.computeHydraulicFlowSpeedFactor(snapshotFor(runningPumps = 1, driveHz = 22.0))
        val onePumpHighHz = com.dgp.boosterpump.ui.components.computeHydraulicFlowSpeedFactor(snapshotFor(runningPumps = 1, driveHz = 48.0))
        val twoPumps = com.dgp.boosterpump.ui.components.computeHydraulicFlowSpeedFactor(snapshotFor(runningPumps = 2, driveHz = 45.0))
        val threePumps = com.dgp.boosterpump.ui.components.computeHydraulicFlowSpeedFactor(snapshotFor(runningPumps = 3, driveHz = 50.0))

        assertEquals(0f, idle, 1e-6f)
        assertEquals(0f, lowWater, 1e-6f)
        assertTrue("Active pump must produce positive flow speed", onePumpLowHz > 0.2f)
        assertTrue("Higher drive frequency must increase flow speed ($onePumpLowHz -> $onePumpHighHz)", onePumpHighHz > onePumpLowHz)
        assertTrue("Two running pumps must flow faster than one ($onePumpHighHz -> $twoPumps)", twoPumps > onePumpHighHz)
        assertTrue("Three running pumps must flow fastest ($twoPumps -> $threePumps)", threePumps > twoPumps)
        assertTrue("Speed factor must stay clamped <= 1.0", threePumps <= 1.0f)
    }

    @Test
    fun measuredThreePhaseKinematicsComputesDynamicCyclePeriodAndPerPhaseOffsets() {
        // 1. Unbalanced live 3-phase grid at 49.80 Hz — shared Cycle Period = 1000 / 49.80, per-phase angles & offsets via Law of Cosines
        val unbalanced = PhaseVoltages(rn = 231, sn = 229, tn = 230, rs = 400, st = 399, tr = 401)
        val kin = com.dgp.boosterpump.ui.components.computeMeasuredThreePhaseKinematics(
            voltages = unbalanced,
            mainsFrequencyHz = 49.80,
            phaseSequenceReversed = false,
        )
        val expected4980PeriodMs = 1000.0 / 49.80 // 20.080321...
        assertEquals(expected4980PeriodMs, kin.mainsPeriodMs!!, 1e-6)
        assertEquals(0.0, kin.rn.angleDeg!!, 1e-6)
        assertEquals(0.0, kin.rn.offsetMs!!, 1e-6)
        // cos(Δθ_RS) = (231² + 229² - 400²) / (2 * 231 * 229) => ~120.816°
        assertEquals(120.816, kin.sn.angleDeg!!, 0.02)
        assertEquals((kin.sn.angleDeg!! / 360.0) * expected4980PeriodMs, kin.sn.offsetMs!!, 1e-4)
        // cos(Δθ_TR) = (230² + 231² - 401²) / (2 * 230 * 231) => Δθ_TR ~120.882° => θ_T = 360 - 120.882 = ~239.118°
        assertEquals(239.118, kin.tn.angleDeg!!, 0.02)
        assertEquals((kin.tn.angleDeg!! / 360.0) * expected4980PeriodMs, kin.tn.offsetMs!!, 1e-4)

        // 2. Reversed phase sequence (Word 80 bit 6) at 50.20 Hz: shared Cycle Period = 19.920 mS, S & T phase offsets swap
        val reversed = com.dgp.boosterpump.ui.components.computeMeasuredThreePhaseKinematics(
            voltages = unbalanced,
            mainsFrequencyHz = 50.20,
            phaseSequenceReversed = true,
        )
        val expected5020PeriodMs = 1000.0 / 50.20 // 19.920318...
        assertEquals(expected5020PeriodMs, reversed.mainsPeriodMs!!, 1e-6)
        assertEquals(360.0 - 120.816, reversed.sn.angleDeg!!, 0.02)
        assertEquals(120.882, reversed.tn.angleDeg!!, 0.02)
        assertEquals((reversed.sn.angleDeg!! / 360.0) * expected5020PeriodMs, reversed.sn.offsetMs!!, 1e-4)
        assertEquals((reversed.tn.angleDeg!! / 360.0) * expected5020PeriodMs, reversed.tn.offsetMs!!, 1e-4)

        // 3. Bench test with 0 V on all phases and live FRQ (51.80 Hz): shared Cycle Period still reports 19.305 mS while per-phase offsets stay null
        val zeroVoltBench = PhaseVoltages(rn = 0, sn = 0, tn = 0, rs = 0, st = 0, tr = 0)
        val benchKin = com.dgp.boosterpump.ui.components.computeMeasuredThreePhaseKinematics(zeroVoltBench, 51.80)
        assertEquals(1000.0 / 51.80, benchKin.mainsPeriodMs!!, 1e-6)
        assertEquals(null, benchKin.rn.angleDeg)
        assertEquals(null, benchKin.rn.offsetMs)
        assertEquals(null, benchKin.sn.offsetMs)
        assertEquals(null, benchKin.tn.offsetMs)

        // 4. Single-phase jumpered supply (R=S=T=230V, VRS=VST=VTR=0V): must measure 0.0° and 0.00 ms offset (never hardcoded 120°/6.67 ms)
        val inPhase = PhaseVoltages(rn = 230, sn = 230, tn = 230, rs = 0, st = 0, tr = 0)
        val inPhaseKin = com.dgp.boosterpump.ui.components.computeMeasuredThreePhaseKinematics(inPhase, 50.0)
        assertEquals(20.0, inPhaseKin.mainsPeriodMs!!, 1e-6)
        assertEquals(0.0, inPhaseKin.rn.offsetMs!!, 1e-6)
        assertEquals(0.0, inPhaseKin.sn.offsetMs!!, 1e-6)
        assertEquals(0.0, inPhaseKin.tn.offsetMs!!, 1e-6)

        // 5. Missing or zero live mains frequency: never falls back to hardcoded 20 ms -> mainsPeriodMs & all offsetMs stay null
        val noFreq = com.dgp.boosterpump.ui.components.computeMeasuredThreePhaseKinematics(unbalanced, null)
        assertEquals(null, noFreq.mainsPeriodMs)
        assertEquals(null, noFreq.rn.offsetMs)
        assertEquals(null, noFreq.sn.offsetMs)
        assertEquals(null, noFreq.tn.offsetMs)
    }
}

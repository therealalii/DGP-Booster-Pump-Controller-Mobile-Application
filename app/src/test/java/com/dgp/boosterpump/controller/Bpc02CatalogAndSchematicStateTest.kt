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
        val allMenuKeys = Bpc02Catalog.MenuCategory.entries
            .flatMap { cat -> cat.pages.flatMap { it.registerKeys } }

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
}

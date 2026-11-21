package com.dgp.boosterpump.controller

import com.dgp.boosterpump.controller.register.AccessPolicy
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.ReadBlock
import com.dgp.boosterpump.controller.register.VendorTable
import com.dgp.boosterpump.controller.register.Verification
import com.dgp.boosterpump.gateway.ConfigValidation
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.gateway.GatewayConfig
import com.dgp.boosterpump.gateway.validate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Bpc02RegisterMapAndProfileTest {

    @Test
    fun pollBlocksRespectGapsAndNeverSpanUndocumentedAddresses() {
        val expected = listOf(
            ReadBlock(VendorTable.BIT, 0, 34), // 0..33
            ReadBlock(VendorTable.WORD, 0, 1), // 0 (Word 1 is undocumented)
            ReadBlock(VendorTable.WORD, 2, 9), // 2..10 (Words 11..33 are undocumented)
            ReadBlock(VendorTable.WORD, 34, 12), // 34..45 (Words 46..79 are undocumented)
            ReadBlock(VendorTable.WORD, 80, 1), // 80
        )
        assertEquals(expected, Bpc02RegisterMap.pollBlocks)
    }

    @Test
    fun manualControlRegistersAreLockedAndNeverPolled() {
        val polledKeys = Bpc02RegisterMap.polled.map { it.key }.toSet()
        assertTrue(Bpc02RegisterMap.manualControl.isNotEmpty())
        Bpc02RegisterMap.manualControl.forEach { def ->
            assertEquals(AccessPolicy.CONTROL_LOCKED, def.access)
            assertFalse("${def.key} must not be polled", def.key in polledKeys)
        }
    }

    @Test
    fun driveFrequencyIsMarkedAmbiguousWithNoAssumedScale() {
        val drive = Bpc02RegisterMap.byKey(Bpc02RegisterMap.DRIVE_FREQUENCY)
        assertEquals(10, drive.documentedAddress)
        assertNull(drive.scale)
        assertEquals(Verification.AMBIGUOUS, drive.verification)
    }

    @Test
    fun decodeAppliesDocumentedScalesAndFlagsUndocumentedErrorBits() {
        val raw = RawReadings(
            bits = mapOf(
                19 to true, // Pump 1 running
                25 to true, // Drive fail
                27 to true, // Pump 2 fail
                32 to false, // Floater L not connected
                33 to true, // Floater H connected
            ),
            words = mapOf(
                0 to 2,
                2 to 435, // 4.35 bar
                3 to 230,
                9 to 5002, // 50.02 Hz
                10 to 1200, // raw drive freq
                34 to 15, // Pump 1 sec
                35 to 42, // Pump 1 min
                36 to 105, // Pump 1 hours
                // Bit 3 (Floater) + Bit 5 (Sensor disconnected) + Bit 0 (undocumented!)
                80 to ((1 shl 3) or (1 shl 5) or (1 shl 0)),
            ),
        )

        val snap = Bpc02Profile.decode(raw, DataSourceMode.LIVE, timestampMillis = 1234L)
        assertFalse(snap.isSimulated)
        assertEquals(4.35, snap.pressureBar!!, 1e-6)
        assertEquals(50.02, snap.mainsFrequencyHz!!, 1e-6)
        assertEquals(1200, snap.driveFrequencyRaw)
        assertEquals(230, snap.voltages.rn)
        assertNull(snap.voltages.sn) // Unread value stays null, never 0

        assertEquals(6, snap.pumps.size)
        assertEquals(true, snap.pumps[0].running)
        assertEquals(RunTime(105, 42, 15), snap.pumps[0].runTime)
        assertEquals(true, snap.pumps[1].failed)
        assertFalse(snap.pumps[4].hasRunTimeRegisters)
        assertNull(snap.pumps[4].runTime)

        assertEquals(
            listOf(SystemErrorBit.FLOATER, SystemErrorBit.PRESSURE_SENSOR_DISCONNECTED),
            snap.activeSystemErrors,
        )
        assertEquals(listOf(0), snap.undocumentedErrorBits)
        // 2 documented system errors + 1 undocumented bit + Drive fail + Pump 2 fail = 5 alarms
        assertEquals(5, snap.alarms.size)
    }

    @Test
    fun gatewayConfigValidationRejectsBlankLiveHostAndUnsupportedBpc04() {
        val liveNoHost = GatewayConfig(mode = DataSourceMode.LIVE, host = "").validate()
        assertFalse(liveNoHost.isValid)
        assertTrue(ConfigValidation.Field.HOST in liveNoHost.errors)

        val liveBadIp = GatewayConfig(mode = DataSourceMode.LIVE, host = "192.168.1.999").validate()
        assertFalse(liveBadIp.isValid)
        assertTrue(ConfigValidation.Field.HOST in liveBadIp.errors)

        val liveGood = GatewayConfig(mode = DataSourceMode.LIVE, host = "192.168.1.50", port = 502, unitId = 1).validate()
        assertTrue(liveGood.isValid)

        val bpc04 = GatewayConfig(mode = DataSourceMode.SIMULATION, controllerModel = ControllerModel.BPC04).validate()
        assertFalse(bpc04.isValid)
        assertTrue(ConfigValidation.Field.MODEL in bpc04.errors)
    }
}

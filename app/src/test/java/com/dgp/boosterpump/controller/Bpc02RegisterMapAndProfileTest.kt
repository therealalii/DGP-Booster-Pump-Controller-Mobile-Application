package com.dgp.boosterpump.controller

import com.dgp.boosterpump.controller.register.AccessPolicy
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.ReadBlock
import com.dgp.boosterpump.controller.register.VendorTable
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
    fun officialExcelMapContainsAll154RegistersWithoutDuplicates() {
        val bits = Bpc02RegisterMap.all.filter { it.table == VendorTable.BIT }
        val words = Bpc02RegisterMap.all.filter { it.table == VendorTable.WORD }
        assertEquals("Official Excel has 56 Bit addresses", 56, bits.size)
        assertEquals("Official Excel has 98 Word addresses", 98, words.size)
        assertEquals(154, Bpc02RegisterMap.all.size)
        assertEquals(bits.size, bits.map { it.documentedAddress }.toSet().size)
        assertEquals(words.size, words.map { it.documentedAddress }.toSet().size)
    }

    @Test
    fun pollBlocksIncludePumpActivationsDriveFrequenciesErrorsSetpointOperatingModeAndControlMode() {
        val expected = listOf(
            ReadBlock(VendorTable.BIT, 0, 34), // 0..33 live status I/O & faults
            ReadBlock(VendorTable.BIT, 4096, 7), // 4096 Drive + 4097..4102 Pump 1..6 activation ('S')
            ReadBlock(VendorTable.WORD, 0, 1), // Displayed Page
            ReadBlock(VendorTable.WORD, 2, 8), // 2..9 Pressure, Voltages, Mains Freq
            ReadBlock(VendorTable.WORD, 25, 2), // 25..26 Drive1 & Drive2 Frequency (0.01 Hz)
            ReadBlock(VendorTable.WORD, 34, 12), // 34..45 Pump 1..4 Work Time
            ReadBlock(VendorTable.WORD, 80, 2), // 80..81 System Error Word 1 & Word 2
            ReadBlock(VendorTable.WORD, 4096, 1), // System Pressure Setpoint
            ReadBlock(VendorTable.WORD, 4133, 2), // Number Of Pump & Variable Pump (Drivers)
            ReadBlock(VendorTable.WORD, 4170, 1), // Wiring / Operating Mode (1..3)
            ReadBlock(VendorTable.WORD, 4173, 1), // Control Status (0=Disable, 1=PID, 2=ON/OFF)
            ReadBlock(VendorTable.WORD, 4180, 8), // MFI1..4 & MFO1..4 configured types
        )
        assertEquals(expected, Bpc02RegisterMap.pollBlocks)
        assertEquals(
            "Fast + slow poll tiers must partition pollBlocks with zero overlap",
            Bpc02RegisterMap.pollBlocks.toSet(),
            (Bpc02RegisterMap.fastPollBlocks + Bpc02RegisterMap.slowPollBlocks).toSet(),
        )
        assertEquals(
            Bpc02RegisterMap.pollBlocks.size,
            Bpc02RegisterMap.fastPollBlocks.size + Bpc02RegisterMap.slowPollBlocks.size,
        )
    }

    @Test
    fun manualOutputRegistersAreLockedAndNeverPolled() {
        val polledKeys = Bpc02RegisterMap.polled.map { it.key }.toSet()
        assertEquals(12, Bpc02RegisterMap.manualControl.size) // 10 coils (50..59) + 2 words (50..51)
        Bpc02RegisterMap.manualControl.forEach { def ->
            assertEquals(AccessPolicy.CONTROL_LOCKED, def.access)
            assertFalse("${def.key} must not be polled", def.key in polledKeys)
        }
    }

    @Test
    fun configurationBlocksCoverCompleteOfficialRangesAndSkipUndocumentedGaps() {
        val expected = listOf(
            ReadBlock(VendorTable.BIT, 4096, 7), // 4096..4102
            ReadBlock(VendorTable.BIT, 4106, 5), // 4106..4110
            ReadBlock(VendorTable.WORD, 4096, 16), // 4096..4111
            ReadBlock(VendorTable.WORD, 4122, 4), // 4122..4125
            ReadBlock(VendorTable.WORD, 4127, 3), // 4127..4129
            ReadBlock(VendorTable.WORD, 4131, 8), // 4131..4138
            ReadBlock(VendorTable.WORD, 4142, 2), // 4142..4143
            ReadBlock(VendorTable.WORD, 4149, 4), // 4149..4152
            ReadBlock(VendorTable.WORD, 4156, 1), // 4156
            ReadBlock(VendorTable.WORD, 4161, 4), // 4161..4164
            ReadBlock(VendorTable.WORD, 4166, 1), // 4166
            ReadBlock(VendorTable.WORD, 4170, 1), // 4170
            ReadBlock(VendorTable.WORD, 4173, 1), // 4173
            ReadBlock(VendorTable.WORD, 4175, 1), // 4175
            ReadBlock(VendorTable.WORD, 4177, 19), // 4177..4195
            ReadBlock(VendorTable.WORD, 4206, 5), // 4206..4210
        )
        assertEquals(expected, Bpc02RegisterMap.configurationBlocks)
    }

    @Test
    fun writePoliciesMatchOfficialExcelReadWriteColumnsExactly() {
        val expectedWritableCoils = setOf(4096, 4097, 4098, 4099, 4100, 4101, 4102)
        val actualWritableCoils = Bpc02RegisterMap.all
            .filter { it.table == VendorTable.BIT && it.isWritable }
            .map { it.documentedAddress }
            .toSet()
        assertEquals(expectedWritableCoils, actualWritableCoils)

        val expectedWritableWords = setOf(
            99, // Multi Function Register (25 = Reset Pump Error)
            4096, // System Pressure Setpoint
            4123, 4124, 4125, 4127, 4128, 4129, // Control Phase & Floater
            4135, 4136, 4137, 4138, // Jockey & PID
            4143, // Sensor Calibration
            4149, 4150, 4151, // Wakeup & Sleep
            4161, 4162, 4163, 4164, 4166, // Timing & Constant Pump %
            4173, 4175, // Control Status & Sleep Gap
            4177, 4178, 4179, // Auto Service
        )
        val actualWritableWords = Bpc02RegisterMap.all
            .filter { it.table == VendorTable.WORD && it.isWritable }
            .map { it.documentedAddress }
            .toSet()
        assertEquals(expectedWritableWords, actualWritableWords)

        // All writable entries must declare explicit min/max bounds
        Bpc02RegisterMap.all.filter { it.isWritable }.forEach { def ->
            assertTrue(
                "${def.key} must have minValue <= maxValue",
                def.minValue != null && def.maxValue != null && def.minValue!! <= def.maxValue!!,
            )
        }

        // Verify previously mis-marked registers are strictly Read-Only (R) per official Excel
        listOf(
            "control_phase_activation",
            "off_changing_activation",
            "time_changing_activation",
            "reserve_auto_activation",
            "reserve_auto_type",
            "mfi_1_type",
            "mfo_1_type",
            "mfo_4_type",
            "modbus_address",
        ).forEach { key ->
            val def = Bpc02RegisterMap.byKey(key)
            assertEquals("$key must be READ", AccessPolicy.READ, def.access)
            assertFalse("$key must not be writable", def.isWritable)
        }
    }

    @Test
    fun decodeMapsPumpDeactivatedStateOperatingModeDriversAndWord80Word81Errors() {
        val raw = RawReadings(
            bits = mapOf(
                19 to true, // Pump 1 running
                20 to true, // Stale/spurious Coil 20 = true while Pump 2 is disabled (S)!
                23 to true, // Unconfigured ghost pump 5 bit = true
                24 to true, // Unconfigured ghost pump 6 bit = true
                25 to true, // Drive fail
                27 to true, // Pump 2 fail
                32 to false, // Floater L not connected
                33 to true, // Floater H connected
                4096 to true, // Mode 2,3 Drive Activation = Enabled
                4097 to true, // Pump 1 Enabled
                4098 to false, // Pump 2 manually deactivated ('S' on LCD!)
                4099 to true, // Pump 3 Enabled
                4100 to true, // Pump 4 Enabled
            ),
            words = mapOf(
                0 to 2,
                2 to 435, // 4.35 bar
                3 to 230,
                9 to 5002, // 50.02 Hz
                25 to 3850, // Drive 1 = 38.50 Hz
                26 to 2400, // Drive 2 = 24.00 Hz
                34 to 15, // Pump 1 sec
                35 to 42, // Pump 1 min
                36 to 105, // Pump 1 hours
                // Word 80: Bit 0 (FLOATER) + Bit 2 (SENSOR_DISCONNECT) + Bit 7 (DRIVE)
                80 to ((1 shl 0) or (1 shl 2) or (1 shl 7)),
                // Word 81: Bit 5 (EMG_STOPPED) + Bit 7 (BATTERY_VOLTAGE) + Bit 12 (undocumented)
                81 to ((1 shl 5) or (1 shl 7) or (1 shl 12)),
                4096 to 450, // Setpoint = 4.50 bar
                4133 to 4, // 4 configured pumps
                4134 to 1, // 1 connected driver (variable pump)
                4170 to 2, // Wiring Mode 2
                4173 to 1, // Control Status = PID
                4180 to 6, // MFI1 = FL
                4181 to 4, // MFI2 = EMG
                4184 to 5, // MFO1 = Alarm
                4185 to 7, // MFO2 = P.Run
            ),
        )

        val snap = Bpc02Profile.decode(raw, DataSourceMode.LIVE, timestampMillis = 1234L)
        assertFalse(snap.isSimulated)
        assertEquals(4.35, snap.pressureBar!!, 1e-6)
        assertEquals(4.50, snap.pressureSetpointBar!!, 1e-6)
        assertEquals(1, snap.controlStatus)
        assertEquals(2, snap.wiringMode)
        assertEquals(4, snap.configuredPumpCount)
        assertEquals(1, snap.variablePumpCount)
        assertEquals(listOf(6, 4, null, null), snap.mfiTypes)
        assertEquals(listOf(5, 7, null, null), snap.mfoTypes)
        assertEquals(50.02, snap.mainsFrequencyHz!!, 1e-6)
        assertEquals(38.50, snap.drive1FrequencyHz!!, 1e-6)
        assertEquals(24.00, snap.drive2FrequencyHz!!, 1e-6)
        assertEquals(230, snap.voltages.rn)
        assertNull(snap.voltages.sn) // Unread value stays null, never 0

        assertEquals(true, snap.driveEnabled)
        assertEquals(6, snap.pumps.size)
        // Mode 2 with 4 configured pumps hides unconfigured ghost pumps 5-6
        assertEquals(4, snap.visiblePumps.size)
        // Pump 1 runs; Pump 2 is disabled ('S') so MUST NOT be counted; ghost pumps 5-6 are excluded
        assertEquals(1, snap.runningPumpCount)
        assertEquals(3, snap.enabledPumpCount)
        assertEquals(1, snap.disabledPumpCount)

        assertEquals(true, snap.pumps[0].running)
        assertEquals(true, snap.pumps[0].enabled)
        assertEquals(RunTime(105, 42, 15), snap.pumps[0].runTime)
        assertEquals(false, snap.pumps[1].enabled) // Pump 2 deactivated ('S')
        assertEquals(true, snap.pumps[1].failed)
        assertFalse(snap.pumps[4].hasRunTimeRegisters)
        assertNull(snap.pumps[4].runTime)

        assertEquals(
            listOf(SystemErrorBit.FLOATER, SystemErrorBit.PRESSURE_SENSOR_DISCONNECTED, SystemErrorBit.DRIVE),
            snap.activeSystemErrors,
        )
        assertEquals(
            listOf(SystemErrorBit2.EMG_STOPPED, SystemErrorBit2.BATTERY_VOLTAGE),
            snap.activeSystemErrors2,
        )
        assertEquals(listOf(12), snap.undocumentedErrorBits2)
        // 3 Word80 errors + 2 Word81 errors + 1 undocumented Word81 bit + Pump 2 fail coil (Drive coil deduplicated) = 7 alarms
        assertEquals(7, snap.alarms.size)
    }

    @Test
    fun gatewayConfigValidationRejectsBlankLiveHostAndUnsupportedBpc04() {
        val liveNoHost = GatewayConfig(mode = DataSourceMode.LIVE, host = "").validate()
        assertFalse(liveNoHost.isValid)
        assertTrue(ConfigValidation.Field.HOST in liveNoHost.errors)

        val liveBadIp = GatewayConfig(mode = DataSourceMode.LIVE, host = "192.168.1.999").validate()
        assertFalse(liveBadIp.isValid)
        assertTrue(ConfigValidation.Field.HOST in liveBadIp.errors)

        val liveGood = GatewayConfig(mode = DataSourceMode.LIVE, host = "192.168.1.74", port = 502, unitId = 1).validate()
        assertTrue(liveGood.isValid)

        val bpc04 = GatewayConfig(mode = DataSourceMode.SIMULATION, controllerModel = ControllerModel.BPC04).validate()
        assertFalse(bpc04.isValid)
        assertTrue(ConfigValidation.Field.MODEL in bpc04.errors)
    }
}

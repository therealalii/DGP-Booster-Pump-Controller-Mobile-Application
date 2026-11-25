package com.dgp.boosterpump.config

import com.dgp.boosterpump.auth.UserRole
import com.dgp.boosterpump.controller.ControllerModel
import com.dgp.boosterpump.controller.RegisterReading
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.gateway.GatewayConfig
import com.dgp.boosterpump.session.ControllerSession
import com.dgp.boosterpump.simulation.SimulatedBpc02Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerConfigTransferTest {

    private fun baselineReadings(): List<RegisterReading> = Bpc02RegisterMap.configuration.map { def ->
        val sampleVal = when {
            def.key == Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT -> 450
            def.key == "sleep_gap" -> 40
            def.key == "pid_p_value" -> 250
            def.key == "wiring_mode" -> 2
            def.key == "number_of_pump" -> 4
            def.key == "number_of_variable_pump" -> 1
            def.key == Bpc02RegisterMap.CONTROL_STATUS -> 1
            def.key == "pump_2_activation" -> 1
            def.enumLabels.isNotEmpty() -> def.enumLabels.keys.sorted().first()
            else -> (def.minValue ?: 0)
        }
        RegisterReading(def, sampleVal)
    }

    @Test
    fun minimalJsonRoundTripsStructuredTypesAndRejectsMalformedJson() {
        val original = JsonValue.Obj(
            linkedMapOf(
                "schemaVersion" to JsonValue.IntNum(1),
                "title" to JsonValue.Str("DGP BPC02 — تنظیمات \"ایستگاه\""),
                "ratio" to JsonValue.DoubleNum(4.5),
                "writable" to JsonValue.Bool(true),
                "note" to JsonValue.Null,
                "items" to JsonValue.Arr(listOf(JsonValue.IntNum(10), JsonValue.IntNum(20))),
            ),
        )
        val encoded = MinimalJson.stringify(original)
        val decoded = MinimalJson.parse(encoded)
        assertEquals(original, decoded)

        val broken = runCatching { MinimalJson.parse("{ \"a\": 1, }") }
        assertTrue("Trailing comma must fail strict RFC-8259 parsing", broken.isFailure)
    }

    @Test
    fun exportAndImportRoundTripComputesExactChangesAndHardwareWarnings() {
        val baseline = baselineReadings()
        val json = ControllerConfigTransfer.exportToJson(
            model = ControllerModel.BPC02,
            configReadings = baseline,
            liveSnapshot = null,
            timestampMillis = 1_764_000_000_000L,
        )

        // Simulate target controller having different setpoint, sleep_gap, and Mode 1 / 2 Pumps
        val targetCurrent = baseline.associate { it.definition.key to it.raw }.toMutableMap().apply {
            this[Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT] = 520
            this["sleep_gap"] = 25
            this["wiring_mode"] = 1
            this["number_of_pump"] = 2
        }

        val result = ControllerConfigTransfer.parseAndValidate(
            jsonText = json,
            targetModel = ControllerModel.BPC02,
            currentValuesByKey = targetCurrent,
            userRole = UserRole.ADMIN,
        )

        assertTrue("Valid exported file must pass validation", result.canApply)
        assertTrue("Zero errors expected", result.errors.isEmpty())
        assertNotNull(result.metadata)
        assertEquals(ControllerConfigTransfer.SCHEMA_VERSION, result.metadata!!.schemaVersion)
        assertEquals("BPC02", result.metadata!!.controllerModelName)
        assertEquals(2, result.metadata!!.wiringMode)
        assertEquals(4, result.metadata!!.numberOfPumps)

        // Exactly 2 writable parameters changed (setpoint + sleep_gap)
        val changedKeys = result.changesToApply.map { it.definition.key }.toSet()
        assertEquals(setOf(Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT, "sleep_gap"), changedKeys)

        // Read-only hardware architecture mismatches (wiring_mode & number_of_pump) must surface as warnings
        val warnedKeys = result.warnings.mapNotNull { it.parameterKey }.toSet()
        assertTrue("wiring_mode mismatch must warn", "wiring_mode" in warnedKeys)
        assertTrue("number_of_pump mismatch must warn", "number_of_pump" in warnedKeys)
    }

    @Test
    fun importValidationBlocksIncompatibleModelUnknownKeysWrongTypesOutOfRangeAndOperatorRestrictedParams() {
        val current = baselineReadings().associate { it.definition.key to it.raw }

        // 1. Incompatible controller model (BPC04 into BPC02)
        val wrongModelJson = """
            {
              "schemaVersion": 1,
              "format": "dgp-bpc-config",
              "controllerModel": "BPC04",
              "parameters": { "sleep_gap": { "raw": 30 } }
            }
        """.trimIndent()
        val wrongModelRes = ControllerConfigTransfer.parseAndValidate(wrongModelJson, ControllerModel.BPC02, current, UserRole.ADMIN)
        assertFalse(wrongModelRes.canApply)
        assertTrue(wrongModelRes.errors.any { it.parameterKey == "controllerModel" })

        // 2. Unsupported future schema version
        val futureSchemaJson = """
            {
              "schemaVersion": 99,
              "format": "dgp-bpc-config",
              "controllerModel": "BPC02",
              "parameters": { "sleep_gap": 30 }
            }
        """.trimIndent()
        val futureSchemaRes = ControllerConfigTransfer.parseAndValidate(futureSchemaJson, ControllerModel.BPC02, current, UserRole.ADMIN)
        assertFalse(futureSchemaRes.canApply)
        assertTrue(futureSchemaRes.errors.any { it.parameterKey == "schemaVersion" })

        // 3. Mixed valid + invalid parameters (unknown key, wrong type string, out-of-range, invalid enum) -> NEVER partially applicable!
        val mixedInvalidJson = """
            {
              "schemaVersion": 1,
              "format": "dgp-bpc-config",
              "controllerModel": "BPC02",
              "parameters": {
                "system_pressure_setpoint": 480,
                "unknown_param_xyz": 10,
                "pid_p_value": "250",
                "sleep_gap": 999,
                "control_status": 7
              }
            }
        """.trimIndent()
        val mixedRes = ControllerConfigTransfer.parseAndValidate(mixedInvalidJson, ControllerModel.BPC02, current, UserRole.ADMIN)
        assertFalse("Files with ANY invalid parameter must strictly block apply", mixedRes.canApply)
        val errKeys = mixedRes.errors.mapNotNull { it.parameterKey }.toSet()
        assertTrue("unknown_param_xyz" in errKeys)
        assertTrue("pid_p_value" in errKeys)
        assertTrue("sleep_gap" in errKeys)
        assertTrue("control_status" in errKeys)

        // 4. Operator role blocked from importing Admin-only advanced parameters
        val adminOnlyJson = """
            {
              "schemaVersion": 1,
              "format": "dgp-bpc-config",
              "controllerModel": "BPC02",
              "parameters": { "sleep_gap": 35 }
            }
        """.trimIndent()
        val opRes = ControllerConfigTransfer.parseAndValidate(adminOnlyJson, ControllerModel.BPC02, current, UserRole.OPERATOR)
        assertFalse("Operator must be blocked from applying admin-only config", opRes.canApply)
        assertTrue("sleep_gap" in opRes.roleRestrictedKeys)
    }

    @Test
    fun endToEndSessionImportAppliesValidatedChangesAndAbortsInvalidBatchesAtomically() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val session = ControllerSession(
            scope = scope,
            ioDispatcher = Dispatchers.Default,
            transportFactory = { cfg, scenario -> SimulatedBpc02Transport(cfg.mapping, scenario, responseDelayMs = 5L) },
        )

        try {
            session.connect(GatewayConfig(mode = DataSourceMode.SIMULATION, pollIntervalMs = 500))
            withTimeout(4000) { session.snapshot.first { it != null } }
            session.readHardwareConfiguration()
            val cfgBefore = withTimeout(4000) {
                session.configSnapshot.first { it.readings.size == Bpc02RegisterMap.configuration.size && !it.loading }
            }

            // 1. Export live simulated config, then validate an imported file with 3 changes
            val exported = ControllerConfigTransfer.exportToJson(
                model = ControllerModel.BPC02,
                configReadings = cfgBefore.readings,
                liveSnapshot = session.snapshot.value,
            )
            assertTrue(exported.contains("\"schemaVersion\": 1"))
            assertTrue(exported.contains("\"controllerModel\": \"BPC02\""))

            val importJson = """
                {
                  "schemaVersion": 1,
                  "format": "dgp-bpc-config",
                  "controllerModel": "BPC02",
                  "parameters": {
                    "system_pressure_setpoint": { "raw": 525 },
                    "sleep_gap": { "raw": 32 },
                    "pump_2_activation": { "raw": 0 }
                  }
                }
            """.trimIndent()
            val currentMap = cfgBefore.readings.associate { it.definition.key to it.raw }
            val validated = ControllerConfigTransfer.parseAndValidate(importJson, ControllerModel.BPC02, currentMap, UserRole.ADMIN)
            assertTrue(validated.canApply)
            assertEquals(3, validated.changesToApply.size)

            session.applyImportedConfiguration(validated.changesToApply.map { it.definition to it.targetValue })
            val afterApply = withTimeout(5000) {
                session.configSnapshot.first { it.importProgress?.completed == true && !it.importProgress.applying }
            }
            assertTrue("All 3 imported parameters must succeed", afterApply.importProgress!!.allSucceeded)
            assertEquals(3, afterApply.importProgress!!.appliedCount)
            assertEquals(525, afterApply.readings.first { it.definition.key == Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT }.raw)
            assertEquals(32, afterApply.readings.first { it.definition.key == "sleep_gap" }.raw)
            assertEquals(0, afterApply.readings.first { it.definition.key == "pump_2_activation" }.raw)
            assertEquals(5.25, session.snapshot.value!!.pressureSetpointBar!!, 1e-6)
            assertEquals(false, session.snapshot.value!!.pumps[1].enabled)

            // 2. Atomic safety guard: batch containing a read-only register transmits ZERO write frames
            val writesBefore = session.transactions.value.count { it.isWrite }
            session.applyImportedConfiguration(
                listOf(
                    Bpc02RegisterMap.byKey("sleep_gap") to 35,
                    Bpc02RegisterMap.byKey("wiring_mode") to 1, // Read-Only!
                ),
            )
            val aborted = session.configSnapshot.value.importProgress!!
            assertFalse(aborted.allSucceeded)
            assertEquals(0, aborted.appliedCount)
            assertTrue(aborted.failedDetails.isNotEmpty())
            assertEquals("Aborted batch must transmit zero write frames", writesBefore, session.transactions.value.count { it.isWrite })
        } finally {
            session.disconnect()
            scope.cancel()
        }
    }
}

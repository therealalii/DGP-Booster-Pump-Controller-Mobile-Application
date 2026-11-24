package com.dgp.boosterpump.session

import com.dgp.boosterpump.controller.SystemErrorBit
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.gateway.GatewayConfig
import com.dgp.boosterpump.gateway.ProtocolMapping
import com.dgp.boosterpump.modbus.ReadFunction
import com.dgp.boosterpump.simulation.SimulatedBpc02Transport
import com.dgp.boosterpump.simulation.SimulationScenario
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

class ControllerSessionTest {

    @Test
    fun simulationSessionPollsScenariosProbesAndReadsHardwareConfig() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val session = ControllerSession(
            scope = scope,
            ioDispatcher = Dispatchers.Default,
            transportFactory = { cfg, scenario -> SimulatedBpc02Transport(cfg.mapping, scenario, responseDelayMs = 5L) },
        )

        try {
            val simConfig = GatewayConfig(
                mode = DataSourceMode.SIMULATION,
                pollIntervalMs = 500,
                mapping = ProtocolMapping(
                    bitReadFunction = ReadFunction.READ_DISCRETE_INPUTS,
                    wordReadFunction = ReadFunction.READ_INPUT_REGISTERS,
                    addressOffset = 0,
                ),
            )
            session.connect(simConfig)

            val snap1 = withTimeout(4000) { session.snapshot.first { it != null } }!!
            assertTrue("Simulated snapshot must always be flagged simulated", snap1.isSimulated)
            assertNotNull(snap1.pressureBar)
            assertTrue(snap1.pressureBar!! in 3.5..5.0)
            assertEquals(4.50, snap1.pressureSetpointBar!!, 1e-6)
            assertEquals(1, snap1.controlStatus)
            assertEquals(2, snap1.wiringMode)
            assertEquals(4, snap1.configuredPumpCount)
            assertEquals(1, snap1.variablePumpCount)
            assertEquals(4, snap1.visiblePumps.size)
            assertTrue(snap1.alarms.isEmpty())
            assertTrue(session.transactions.value.isNotEmpty())
            assertTrue("Pressure history must record live samples", session.pressureHistory.value.isNotEmpty())

            // Switch to floater alarm scenario and confirm Word 80 bit 0 alarm appears on next poll
            session.setSimulationScenario(SimulationScenario.FLOATER_LOW_ALARM)
            val snap2 = withTimeout(4000) {
                session.snapshot.first { it != null && SystemErrorBit.FLOATER in it.activeSystemErrors }
            }!!
            assertEquals(false, snap2.floaterLowConnected)
            assertEquals(0, snap2.runningPumpCount)

            // Read-only probe against Word 2 using the configured FC04
            session.runReadProbe(ReadFunction.READ_INPUT_REGISTERS, pduAddress = 2, quantity = 1)
            val probe = withTimeout(3000) { session.probeResult.first { it != null } }!!
            assertEquals(null, probe.error)
            assertEquals(listOf(115), probe.values)

            // On-demand hardware configuration read covers all configuration registers
            session.readHardwareConfiguration()
            val cfgState = withTimeout(4000) { session.configSnapshot.first { it.readings.isNotEmpty() && !it.loading } }
            assertEquals(Bpc02RegisterMap.configuration.size, cfgState.readings.size)

            // Simulated timeout transitions connection state to Error
            session.setSimulationScenario(SimulationScenario.LINK_TIMEOUT)
            val errState = withTimeout(4000) { session.connectionState.first { it is ConnectionState.Error } }
            assertTrue((errState as ConnectionState.Error).isSimulated)

            // Disconnect / logout MUST wipe all session telemetry & config so zero stale data lingers
            session.disconnect()
            assertEquals(ConnectionState.Disconnected, session.connectionState.value)
            assertEquals(null, session.snapshot.value)
            assertEquals(null, session.activeConfig.value)
            assertEquals(null, session.probeResult.value)
            assertTrue(session.pressureHistory.value.isEmpty())
            assertTrue(session.configSnapshot.value.readings.isEmpty())
        } finally {
            session.disconnect()
            scope.cancel()
        }
    }

    @Test
    fun verifiedHardwareWritesResetErrorsAndSafetyGuardsBlockReadOnlyRegisters() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val session = ControllerSession(
            scope = scope,
            ioDispatcher = Dispatchers.Default,
            transportFactory = { cfg, scenario -> SimulatedBpc02Transport(cfg.mapping, scenario, responseDelayMs = 5L) },
        )

        try {
            // Default verified BPC02 mapping (FC01 + FC03 + offset 0)
            session.connect(GatewayConfig(mode = DataSourceMode.SIMULATION, pollIntervalMs = 500))
            withTimeout(4000) { session.snapshot.first { it != null } }

            // Load full hardware config
            session.readHardwareConfiguration()
            val initialCfg = withTimeout(4000) {
                session.configSnapshot.first { it.readings.size == Bpc02RegisterMap.configuration.size && !it.loading }
            }
            assertEquals(40, initialCfg.readings.first { it.definition.key == "sleep_gap" }.raw)
            assertEquals(1, initialCfg.readings.first { it.definition.key == "pump_6_activation" }.raw)
            assertEquals(450, initialCfg.readings.first { it.definition.key == "system_pressure_setpoint" }.raw)

            // 1. FC06 write to Sleep Gap (4175 = 45 -> 0.45 bar) with immediate read-back
            val sleepGap = Bpc02RegisterMap.byKey("sleep_gap")
            session.writeHardwareParameter(sleepGap, 45)
            val afterWordWrite = withTimeout(3000) {
                session.configSnapshot.first { it.lastWrite?.definition?.key == "sleep_gap" && it.writingKey == null }
            }
            assertTrue(afterWordWrite.lastWrite!!.success)
            assertEquals(45, afterWordWrite.lastWrite!!.readBackValue)
            assertEquals(45, afterWordWrite.readings.first { it.definition.key == "sleep_gap" }.raw)
            assertTrue(session.transactions.value.any { it.isWrite && it.functionCode == 0x06 && it.pduAddress == 4175 })

            // 2. FC05 write to Pump 1 Activation (4097 = 0 -> Disabled 'S') immediately patches live snapshot & running count
            val pump1 = Bpc02RegisterMap.byKey("pump_1_activation")
            session.writeHardwareParameter(pump1, 0)
            val afterCoilWrite = withTimeout(3000) {
                session.configSnapshot.first { it.lastWrite?.definition?.key == "pump_1_activation" && it.writingKey == null }
            }
            assertTrue(afterCoilWrite.lastWrite!!.success)
            assertEquals(0, afterCoilWrite.lastWrite!!.readBackValue)
            assertEquals(false, session.snapshot.value!!.pumps[0].enabled)
            assertEquals(false, session.snapshot.value!!.pumps[0].running)
            assertEquals(0, session.snapshot.value!!.runningPumpCount)
            assertTrue(session.transactions.value.any { it.isWrite && it.functionCode == 0x05 && it.pduAddress == 4097 })

            // 3. FC06 Reset Pump Error command (Word 99 = 25) succeeds and clears active simulated faults
            session.setSimulationScenario(SimulationScenario.PUMP_AND_DRIVE_FAULT)
            withTimeout(4000) {
                session.snapshot.first { it != null && it.alarms.isNotEmpty() }
            }
            session.resetPumpErrors()
            val afterReset = withTimeout(3000) {
                session.configSnapshot.first {
                    it.lastWrite?.definition?.key == Bpc02RegisterMap.MULTI_FUNCTION_REGISTER && it.writingKey == null
                }
            }
            assertTrue(afterReset.lastWrite!!.success)
            assertTrue(session.transactions.value.any { it.isWrite && it.functionCode == 0x06 && it.pduAddress == 99 })
            withTimeout(4000) {
                session.snapshot.first { it != null && it.alarms.isEmpty() }
            }

            // 4. Safety guardrails MUST block all R-only & locked registers + out-of-range values
            val txCountBeforeGuards = session.transactions.value.count { it.isWrite }

            session.writeHardwareParameter(Bpc02RegisterMap.byKey("modbus_address"), 2)
            assertFalse(session.configSnapshot.value.lastWrite!!.success)

            session.writeHardwareParameter(Bpc02RegisterMap.byKey("control_phase_activation"), 1)
            assertFalse(session.configSnapshot.value.lastWrite!!.success)

            session.writeHardwareParameter(Bpc02RegisterMap.byKey("mfi_1_type"), 1)
            assertFalse(session.configSnapshot.value.lastWrite!!.success)

            session.writeHardwareParameter(Bpc02RegisterMap.byKey("output_0_manual"), 1)
            assertFalse(session.configSnapshot.value.lastWrite!!.success)

            session.writeHardwareParameter(Bpc02RegisterMap.byKey("multi_function_register"), 1234)
            assertFalse(session.configSnapshot.value.lastWrite!!.success)

            session.writeHardwareParameter(sleepGap, 101) // max is 100
            assertFalse(session.configSnapshot.value.lastWrite!!.success)

            assertEquals(
                "Blocked writes must never transmit any write frame",
                txCountBeforeGuards,
                session.transactions.value.count { it.isWrite },
            )
        } finally {
            session.disconnect()
            scope.cancel()
        }
    }
}

package com.dgp.boosterpump.session

import com.dgp.boosterpump.controller.SystemErrorBit
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
            assertTrue(snap1.alarms.isEmpty())
            assertTrue(session.transactions.value.isNotEmpty())

            // Switch to floater alarm scenario and confirm alarm appears on next poll
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

            // On-demand hardware configuration read
            session.readHardwareConfiguration()
            val cfgState = withTimeout(3000) { session.configSnapshot.first { it.readings.isNotEmpty() && !it.loading } }
            assertEquals(24, cfgState.readings.size)

            // Simulated timeout transitions connection state to Error
            session.setSimulationScenario(SimulationScenario.LINK_TIMEOUT)
            val errState = withTimeout(4000) { session.connectionState.first { it is ConnectionState.Error } }
            assertTrue((errState as ConnectionState.Error).isSimulated)

            session.disconnect()
            assertEquals(ConnectionState.Disconnected, session.connectionState.value)
        } finally {
            session.disconnect()
            scope.cancel()
        }
    }
}

# DGP Booster Pump Controller — Android Application

Industrial Android application (Kotlin, Jetpack Compose, Material 3, MVVM) for monitoring **DGP Booster Pump Controllers** over Modbus TCP/IP through a **Samin ES 487** RS485-to-Ethernet Modbus TCP gateway.

- **Initial target:** DGP **BPC02** (hardware-verified on COM4, 9600 8N1, Slave ID 1 — see [`BPC02_REGISTER_MAP.pdf`](BPC02_REGISTER_MAP.pdf) and [`BPC02_REGISTER_MAP.md`](BPC02_REGISTER_MAP.md))
- **Future target:** DGP **BPC04** (architecture ready via `ControllerProfile`; disabled until the BPC04 register map is confirmed)
- **Safety posture:** **Strict READ-ONLY mode.** Write function codes (`05`, `06`, `0F`, `10`) are intentionally omitted from the Modbus client so no unverified write can ever reach a physical controller.

---

## 1. Hardware & Communication Architecture

```text
Android App (Modbus TCP Client)
        │  Local Wi-Fi / LAN (TCP/IP)
        ▼
Samin ES 487 Ethernet Gateway (Modbus TCP/IP ↔ Modbus RTU, 5–48 V DC)
        │  RS485 (2-wire A/B + GND, 9600 baud, 8N1)
        ▼
DGP BPC02 Booster Pump Controller (Modbus RTU Slave, Unit ID 1)
```

### Samin ES 487 Gateway Notes (مبدل مدباس RS485 به مدباس اترنت مدل ES 487)

- **Selected hardware:** **Samin ES 487** industrial RS485-to-Ethernet Modbus converter:
  - **Power supply:** `5–48 V DC` wide input range (ships with a `12 V DC` adapter).
  - **Configuration:** Built-in web browser interface + IP finder utility + hardware factory-reset button.
  - **Network modes:** `Server` / `Client`, `TCP` / `UDP`, `DHCP` or `Static IP`.
  - **Protocol conversion:** `Modbus RTU` (RS485) ↔ `Modbus TCP/IP` (Ethernet).
- **Required ES 487 settings for the DGP BPC02:**
  1. **Serial Port (RS485):** `9600` bps, `8` data bits, `No` parity, `1` stop bit (`9600 8N1`) — hardware-verified on the BPC02.
  2. **Working Mode:** **TCP Server** with **Modbus TCP/IP ↔ Modbus RTU** conversion (select `Modbus TCP (MBAP)` framing in the app), or transparent serial-over-TCP if configured for raw RTU tunnelling (`RTU over TCP` framing in the app).
  3. **IP & TCP Port:** Assign a **Static IP** on the local LAN (or discover the DHCP address using the ES 487 IP finder software) and note the configured TCP port (commonly `502`). Do not hardcode or assume factory settings.
  4. **BPC02 Slave / Unit ID:** Verified as `1` (readable at Holding Register `4195`; configurable in the BPC02 `Hardware → Network` menu).
- The transport layer (`ModbusTransport`) is gateway-agnostic so any compatible Modbus TCP or transparent RS485-to-Ethernet gateway can also be supported without changing controller or UI code.

---

## 2. Hardware-Verified BPC02 Protocol & Register Summary

Full tables are published in **[`BPC02_REGISTER_MAP.pdf`](BPC02_REGISTER_MAP.pdf)** and **[`BPC02_REGISTER_MAP.md`](BPC02_REGISTER_MAP.md)** (from `BPC-02.pdf` pp. 25–28 and live captures in `BPC02_TEST_RESULTS.md`, `REMAINING 01`, and `REMAINING 03`):

### Hardware-Verified on Real BPC02 (`9600 8N1`, Slave ID `1`)
- **Zero-based PDU addressing (`offset = 0`):** Printed decimal manual addresses map directly to wire PDU addresses without any `±1` shift.
- **Bit Table = `FC01` (Read Coils) ONLY:**
  - `0–33` (polled in one `qty 34` request): Outputs `0–9`, `DR` (`10`), `PR1–4` (`11–14`), `MFI1–4` (`15–18`), Pumps `1–6` status (`19–24`), Drive fail (`25`), Pumps `1–6` fail (`26–31`), Low/High floaters (`32–33`).
  - `50–59` (`qty 10`): Manual outputs `0–9` (readable via `FC01`; **locked for writes**).
  - `4096–4101` (`qty 6`) & `4106–4110` (`qty 5`): Pump `1–6` Activation and Hardware Config bits (read-only on demand).
- **Word Table = `FC03` (Read Holding Registers) ONLY:**
  - `0`: Displayed LCD page number.
  - `2–10` (`qty 9`): System pressure (`2`, `0.01 bar`), Phase-to-neutral voltages `Vrn/Vsn/Vtn` (`3–5`, `V`), Phase-to-phase voltages `Vrs/Vst/Vtr` (`6–8`, `V`), Mains frequency (`9`, `0.01 Hz`), Drive frequency (`10`, range `0–1600`, read `1000` on hardware — scale/unit unverified, displayed raw).
  - `34–45` (`qty 12`): Pump `1–4` operating time (Seconds, Minutes, Hours per pump — 1 Hz tick and 60-second rollover verified on hardware). Pumps 5–6 have no run-time registers.
  - `50–51` (`qty 2`): Manual analog outputs `0–1` (`0–500`, `0.1 Hz`; readable via `FC03`; **locked for writes**).
  - `80` (`qty 1`): System Error bitfield (bits `3–10` documented: Floater, Max Pressure, Pressure Sensor Disconnected, Phase Sensitivity, Phase Max/Min Voltage, Phase Sequence, Pump 1 Error).
  - `99` (`qty 1`): `Multi Function Register` (confirmed readable via `FC03`, returns `0` in Automatic mode; **locked for writes**).
  - `4173`, `4175`, `4177–4186`, `4195`: Hardware configuration words (read-only on demand). PDF print typos `417B` and `41B0` on page 28 are confirmed as decimal **`4178`** and **`4180`**.
- **Firmware Quirks Discovered:**
  1. **`FC02` and `FC04` are rejected** with a non-standard 4-byte RTU frame carrying a valid CRC (`01 02 81 E1` and `01 04 01 E3`). Never send `FC02` or `FC04` to a BPC02.
  2. **Unmasked trailing coil bits:** On `FC01` reads where `quantity` is not a multiple of 8 (`4096 qty 6 → 0xFF`, `4106 qty 5 → 0xD0`), high padding bits are not cleared by the controller; `ModbusPdu.parseBitsResponse` reads strictly `0 until quantity`.

---

## 3. Application Architecture

```text
app/src/main/java/com/dgp/boosterpump/
├── modbus/          # ReadFunction (FC01–FC04 only), ModbusPdu, Crc16, ModbusTransport,
│                    # TcpModbusTransport (MBAP + RTU-over-TCP), ModbusReadClient
├── controller/      # ControllerProfile (BPC02 + BPC04 extension point), ControllerReader,
│   └── register/    # ControllerSnapshot, SystemErrorBit, RegisterDefinition, Bpc02RegisterMap
├── gateway/         # GatewayConfig, ProtocolMapping (default FC01 + FC03 + offset 0), DataStore
├── simulation/      # SimulatedBpc02Transport (5 test scenarios through the real Modbus decoder)
├── session/         # ControllerSession (polling loop, backoff, transaction log, read probe)
├── viewmodel/       # MonitoringViewModel, GatewaySettingsViewModel
└── ui/              # Material 3 industrial theme, shared components, 5 Compose screens
```

---

## 4. Build & Test Instructions

```bash
# Run all unit tests (Modbus codec, loopback TCP/RTU transport, BPC02 register map, session)
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
```
The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 5. Repository Documentation

- [`BPC02_REGISTER_MAP.pdf`](BPC02_REGISTER_MAP.pdf) & [`BPC02_REGISTER_MAP.md`](BPC02_REGISTER_MAP.md) — Complete hardware-verified BPC02 Modbus register map & Samin ES 487 setup
- [`BPC02_TEST_RESULTS.md`](BPC02_TEST_RESULTS.md), `REMAINING 01 ... .txt`, `REMAINING 03 ... .txt` — Raw Modbus RTU hardware test logs on COM4
- `BPC-02.pdf` — DGP BPC-02 User Manual (primary controller reference)
- `DGP_BPC02_Modbus_Research_Findings.txt` — Initial Modbus protocol audit
- `ETOR-4_EN.pdf`, `Modbus register map .txt`, `modbuscloud-rs485-modbus-checklist-en.pdf` — General Modbus gateway and RS485 wiring reference material

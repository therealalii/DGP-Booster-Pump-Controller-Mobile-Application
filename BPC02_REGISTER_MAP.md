# DGP BPC02 — Complete Modbus Register Map

> **Sources:** `BPC-02.pdf` (User Manual V1, pages 21 & 25–28), hardware test captures in `BPC02_TEST_RESULTS.md`, and follow-up captures in `REMAINING 01 LIVE NON-ZERO LCD CHECK (ADDRESSES 0 & 2–10).txt` and `REMAINING 03 LIVE STATUS BITS VS LCDLEDS (COILS 0–33).txt` (COM4, Modbus RTU, 9600 8N1, Slave ID 1, read-only).
>
> **Selected RS485-to-Ethernet Gateway:** **Samin ES 487** (مبدل مدباس RS485 به مدباس اترنت مدل ES 487 — 5–48 V DC, Web UI configuration, Server/Client, TCP/UDP, DHCP/Static IP, Modbus RTU ↔ Modbus TCP/IP).
>
> **Safety Status:** **READ-ONLY.** All write operations (`FC05`, `FC06`, `FC15`, `FC16`) are unverified and remain **LOCKED** until confirmed by DGP and approved for isolated bench testing.

---

## 1. Confirmed Communication & Protocol Profile

| Parameter | Value | Status |
|---|---|---|
| Controller Model | DGP BPC02 Booster Pump Controller | **VERIFIED** |
| Serial Protocol | Modbus RTU over RS485 (2-wire A/B + GND) | **VERIFIED** |
| Serial Settings | `9600` baud, `8` data bits, `No` parity, `1` stop bit (`9600 8N1`) | **VERIFIED** |
| Tested Slave / Unit ID | `1` (configurable in BPC02 `Hardware → Network`, readable at Holding Reg `4195`) | **VERIFIED** |
| Addressing Base | **Zero-based PDU addressing (`offset = 0`)** — printed decimal address = wire address | **VERIFIED** |
| Bit Table Function Code | **`FC01` (Read Coils) ONLY** | **VERIFIED** |
| Word Table Function Code | **`FC03` (Read Holding Registers) ONLY** | **VERIFIED** |
| `FC02` (Discrete Inputs) | **NOT SUPPORTED** — BPC02 replies with non-standard 4-byte frame `01 02 81 E1` | **VERIFIED** |
| `FC04` (Input Registers) | **NOT SUPPORTED** — BPC02 replies with non-standard 4-byte frame `01 04 01 E3` | **VERIFIED** |
| Byte Order (16-bit words) | Big-endian (MSB first), standard Modbus | **VERIFIED** |
| Signedness | Not declared in manual; treated as unsigned `UINT16` (`0–65535`) | Documented/Assumed |
| Multi-Register Reads | Supported up to at least `qty 34` (`FC01`) and `qty 12` (`FC03`) | **VERIFIED** |
| Ethernet Gateway | **Samin ES 487** (Modbus RTU ↔ Modbus TCP/IP, Server mode, `9600 8N1` on RS485) | Selected Hardware |

### Important BPC02 Firmware Quirks Discovered on Hardware
1. **Non-standard 4-byte rejection frame for unsupported function codes:**  
   When sent `FC02` or `FC04`, the BPC02 does **not** return a standard 5-byte Modbus exception (`[ID] [FC|0x80] [ExCode] [CRC_L] [CRC_H]`). Instead, it replies with a **4-byte frame whose CRC-16 is valid**:
   - `FC02` → `01 02 81 E1` (`CRC16(01 02) = 81 E1`, valid)
   - `FC04` → `01 04 01 E3` (`CRC16(01 04) = 01 E3`, valid)
   - **Rule:** Never send `FC02` or `FC04` to a BPC02 (especially through the Samin ES 487 Modbus TCP gateway, where non-standard 4-byte RTU replies can trigger timeouts or framing errors).
2. **Unmasked trailing padding bits on `FC01`:**  
   When reading a coil count that is not a multiple of 8 (e.g., `4096 qty 6` → `0xFF`, `4106 qty 5` → `0xD0`), the BPC02 does **not** zero out the unused high bits in the final byte. Clients must decode strictly `0 until quantity` and ignore padding bits.
3. **PDF print typos resolved (`417B` and `41B0`):**  
   `BPC-02.pdf` page 28 prints `417B` (row 59) and `41B0` (row 61). Hardware tests confirmed these are print/font typos for decimal **`4178`** and **`4180`** (contiguous block reads `4177 qty 3` and `4180 qty 7` both succeeded).

---

## 2. Bit Address Table — Coils (`FC01` Read Coils)

### 2.1 Live Status Bits (Polled — Single Block Read: `FC01`, Addr `0`, Qty `34`)

Captured repeatedly (`01 01 00 00 00 22 BC 13` → `01 01 05 3F 00 F8 01 00 05 36`, ~93 ms response time):

| Dec Addr | Hex Addr | Manual Row | Name | Values (`0` / `1`) | Observed on Hardware | Verification Status |
|---:|---:|---:|---|---|---|---|
| 0 | `0x0000` | 1 | Output 0 Status | `0: OFF` / `1: ON` | `1` (ON) | **HARDWARE VERIFIED** |
| 1 | `0x0001` | 2 | Output 1 Status | `0: OFF` / `1: ON` | `1` (ON) | **HARDWARE VERIFIED** |
| 2 | `0x0002` | 3 | Output 2 Status | `0: OFF` / `1: ON` | `1` (ON) | **HARDWARE VERIFIED** |
| 3 | `0x0003` | 4 | Output 3 Status | `0: OFF` / `1: ON` | `1` (ON) | **HARDWARE VERIFIED** |
| 4 | `0x0004` | 5 | Output 4 Status | `0: OFF` / `1: ON` | `1` (ON) | **HARDWARE VERIFIED** |
| 5 | `0x0005` | 6 | Output 5 Status | `0: OFF` / `1: ON` | `1` (ON) | **HARDWARE VERIFIED** |
| 6 | `0x0006` | 7 | Output 6 Status | `0: OFF` / `1: ON` | `0` (OFF) | **HARDWARE VERIFIED** |
| 7 | `0x0007` | 8 | Output 7 Status | `0: OFF` / `1: ON` | `0` (OFF) | **HARDWARE VERIFIED** |
| 8 | `0x0008` | 9 | Output 8 Status | `0: OFF` / `1: ON` | `0` (OFF) | **HARDWARE VERIFIED** |
| 9 | `0x0009` | 10 | Output 9 Status | `0: OFF` / `1: ON` | `0` (OFF) | **HARDWARE VERIFIED** |
| 10 | `0x000A` | 11 | DR Status | `0: OFF` / `1: ON` | `0` (OFF) | **HARDWARE VERIFIED** *(physical signal not detailed in manual)* |
| 11 | `0x000B` | 12 | PR1 Status | `0: OFF` / `1: ON` | `0` (OFF) | **HARDWARE VERIFIED** *(Pump 1 direct-contactor feedback, p.8)* |
| 12 | `0x000C` | 13 | PR2 Status | `0: OFF` / `1: ON` | `0` (OFF) | **HARDWARE VERIFIED** *(Pump 2 direct-contactor feedback, p.8)* |
| 13 | `0x000D` | 14 | PR3 Status | `0: OFF` / `1: ON` | `0` (OFF) | **HARDWARE VERIFIED** *(Pump 3 direct-contactor feedback, p.8)* |
| 14 | `0x000E` | 15 | PR4 Status | `0: OFF` / `1: ON` | `0` (OFF) | **HARDWARE VERIFIED** *(Pump 4 direct-contactor feedback, p.8)* |
| 15 | `0x000F` | 16 | MFI1 Status | `0: OFF` / `1: ON` | `0` (OFF) | **HARDWARE VERIFIED** *(configurable input, type at Reg 4180)* |
| 16 | `0x0010` | 17 | MFI2 Status | `0: OFF` / `1: ON` | `0` (OFF) | **HARDWARE VERIFIED** *(configurable input, type at Reg 4181)* |
| 17 | `0x0011` | 18 | MFI3 Status | `0: OFF` / `1: ON` | `0` (OFF) | **HARDWARE VERIFIED** *(configurable input, type at Reg 4182)* |
| 18 | `0x0012` | 19 | MFI4 Status | `0: OFF` / `1: ON` | `0` (OFF) | **HARDWARE VERIFIED** *(configurable input, type at Reg 4183)* |
| 19 | `0x0013` | 20 | Pump 1 Status | `0: OFF` / `1: ON` | `1` (ON) | **HARDWARE VERIFIED** |
| 20 | `0x0014` | 21 | Pump 2 Status | `0: OFF` / `1: ON` | `1` (ON) | **HARDWARE VERIFIED** |
| 21 | `0x0015` | 22 | Pump 3 Status | `0: OFF` / `1: ON` | `1` (ON) | **HARDWARE VERIFIED** |
| 22 | `0x0016` | 23 | Pump 4 Status | `0: OFF` / `1: ON` | `1` (ON) | **HARDWARE VERIFIED** |
| 23 | `0x0017` | 24 | Pump 5 Status | `0: OFF` / `1: ON` | `1` (ON) | **HARDWARE VERIFIED** |
| 24 | `0x0018` | 25 | Pump 6 Status | `0: OFF` / `1: ON` | `1` (ON) | **HARDWARE VERIFIED** |
| 25 | `0x0019` | 26 | Drive Fail | `0: Normal` / `1: Failed` | `0` (Normal) | **HARDWARE VERIFIED** |
| 26 | `0x001A` | 27 | Pump 1 Fail | `0: Normal` / `1: Failed` | `0` (Normal) | **HARDWARE VERIFIED** |
| 27 | `0x001B` | 28 | Pump 2 Fail | `0: Normal` / `1: Failed` | `0` (Normal) | **HARDWARE VERIFIED** |
| 28 | `0x001C` | 29 | Pump 3 Fail | `0: Normal` / `1: Failed` | `0` (Normal) | **HARDWARE VERIFIED** |
| 29 | `0x001D` | 30 | Pump 4 Fail | `0: Normal` / `1: Failed` | `0` (Normal) | **HARDWARE VERIFIED** |
| 30 | `0x001E` | 31 | Pump 5 Fail | `0: Normal` / `1: Failed` | `0` (Normal) | **HARDWARE VERIFIED** |
| 31 | `0x001F` | 32 | Pump 6 Fail | `0: Normal` / `1: Failed` | `0` (Normal) | **HARDWARE VERIFIED** |
| 32 | `0x0020` | 33 | Floater L Level | `0: Not Connected` / `1: Connected` | `0` | **HARDWARE VERIFIED** |
| 33 | `0x0021` | 34 | Floater H Level | `0: Not Connected` / `1: Connected` | `0` | **HARDWARE VERIFIED** |

### 2.2 Manual Output Bits (LOCKED for Writes — Read-Only Tested: `FC01`, Addr `50`, Qty `10`)

| Dec Addr | Hex Addr | Manual Row | Name | Values | Access in App | Status |
|---:|---:|---:|---|---|---|---|
| 50–59 | `0x0032`–`0x003B` | 35–44 | Output 0–9 Manual | `0: OFF` / `1: ON` | **LOCKED (no writes)** | Read via `FC01` OK (all `0`); write FC (`FC05`/`FC0F`) unverified |

### 2.3 Hardware Configuration Bits (On-Demand Read-Only — Never Written)

> Manual warning (p. 28): hardware setting changes can damage the booster pump. Read-only in this app.

**Block A (`FC01`, Addr `4096`, Qty `6`):**

| Dec Addr | Hex Addr | Manual Row | Name | Values | Observed (`0xFF`) | Status |
|---:|---:|---:|---|---|---|---|
| 4096 | `0x1000` | 45 | Pump 1 Activation | `0: Disable` / `1: Enable` | `1` (Enable) | **HARDWARE VERIFIED** |
| 4097 | `0x1001` | 46 | Pump 2 Activation | `0: Disable` / `1: Enable` | `1` (Enable) | **HARDWARE VERIFIED** |
| 4098 | `0x1002` | 47 | Pump 3 Activation | `0: Disable` / `1: Enable` | `1` (Enable) | **HARDWARE VERIFIED** |
| 4099 | `0x1003` | 48 | Pump 4 Activation | `0: Disable` / `1: Enable` | `1` (Enable) | **HARDWARE VERIFIED** |
| 4100 | `0x1004` | 49 | Pump 5 Activation | `0: Disable` / `1: Enable` | `1` (Enable) | **HARDWARE VERIFIED** |
| 4101 | `0x1005` | 50 | Pump 6 Activation | `0: Disable` / `1: Enable` | `1` (Enable) | **HARDWARE VERIFIED** |

**Block B (`FC01`, Addr `4106`, Qty `5`):**

| Dec Addr | Hex Addr | Manual Row | Name | Values | Observed (`0xD0`) | Status |
|---:|---:|---:|---|---|---|---|
| 4106 | `0x100A` | 51 | Control Phase Activation | `0: Disable` / `1: Enable` | `0` (Disable) | **HARDWARE VERIFIED** |
| 4107 | `0x100B` | 52 | OFF Changing Activation | `0: Disable` / `1: Enable` | `0` (Disable) | **HARDWARE VERIFIED** |
| 4108 | `0x100C` | 53 | Time Changing Activation | `0: Disable` / `1: Enable` | `0` (Disable) | **HARDWARE VERIFIED** |
| 4109 | `0x100D` | 54 | Reserve Automatic Activation | `0: Disable` / `1: Enable` | `1` (Enable) | **HARDWARE VERIFIED** |
| 4110 | `0x100E` | 55 | Reserve Automatic Type | `0: NO` / `1: NC` | `1` (NC) | **HARDWARE VERIFIED** |

*(Note: Bits 6–7 of `0xFF` and Bits 5–7 of `0xD0` are unmasked padding bits beyond `quantity`.)*

---

## 3. Word Address Table — Holding Registers (`FC03` Read Holding Registers)

### 3.1 Live Measurements, Run Times & Errors (Polled)

Gap-aware poll blocks (matches `Bpc02RegisterMap.pollBlocks`):
1. `FC03`, Addr `0`, Qty `1`
2. `FC03`, Addr `2`, Qty `9` (`2–10`)
3. `FC03`, Addr `34`, Qty `12` (`34–45`)
4. `FC03`, Addr `80`, Qty `1`

| Dec Addr | Hex Addr | Manual Row | Name | Type | Scale / Unit | Documented Range | Observed on Hardware | Verification Status |
|---:|---:|---:|---|---|---|---|---|---|
| 0 | `0x0000` | 1 | Displayed Page Number | UINT16 | `1` (index) | — | `0` (`01 03 02 00 00 B8 44`) | **HARDWARE VERIFIED** |
| 2 | `0x0002` | 2 | System Pressure | UINT16 | `× 0.01 bar` | — | `0` (idle) | **FC03 VERIFIED** *(0.01 bar documented)* |
| 3 | `0x0003` | 3 | Voltage R–N (`Vrn`) | UINT16 | `× 1 V` | — | `0` *(Coil 4106 OFF)* | **FC03 VERIFIED** *(Volt documented)* |
| 4 | `0x0004` | 4 | Voltage S–N (`Vsn`) | UINT16 | `× 1 V` | — | `0` *(Coil 4106 OFF)* | **FC03 VERIFIED** *(Volt documented)* |
| 5 | `0x0005` | 5 | Voltage T–N (`Vtn`) | UINT16 | `× 1 V` | — | `0` *(Coil 4106 OFF)* | **FC03 VERIFIED** *(Volt documented)* |
| 6 | `0x0006` | 6 | Voltage R–S (`Vrs`) | UINT16 | `× 1 V` | — | `0` *(Coil 4106 OFF)* | **FC03 VERIFIED** *(Volt documented)* |
| 7 | `0x0007` | 7 | Voltage S–T (`Vst`) | UINT16 | `× 1 V` | — | `0` *(Coil 4106 OFF)* | **FC03 VERIFIED** *(Volt documented)* |
| 8 | `0x0008` | 8 | Voltage T–R (`Vtr`) | UINT16 | `× 1 V` | — | `0` *(Coil 4106 OFF)* | **FC03 VERIFIED** *(Volt documented)* |
| 9 | `0x0009` | 9 | Mains Frequency | UINT16 | `× 0.01 Hz` | — | `0` *(Coil 4106 OFF)* | **FC03 VERIFIED** *(0.01 Hz documented)* |
| 10 | `0x000A` | 10 | Drive Frequency | UINT16 | **UNVERIFIED** | `0–1600` | `1000` (`0x03E8`) | **AMBIGUOUS** *(shown as raw value until scale vs LCD is confirmed)* |
| 34 | `0x0022` | 11 | Pump 1 Work Time — Sec | UINT16 | `× 1 s` | `0–59` | Ticking + 60 s rollover | **HARDWARE VERIFIED** |
| 35 | `0x0023` | 12 | Pump 1 Work Time — Min | UINT16 | `× 1 min` | `0–59` | Incremented on rollover | **HARDWARE VERIFIED** |
| 36 | `0x0024` | 13 | Pump 1 Work Time — Hour | UINT16 | `× 1 h` | — | `0` | **HARDWARE VERIFIED** |
| 37 | `0x0025` | 14 | Pump 2 Work Time — Sec | UINT16 | `× 1 s` | `0–59` | Ticking + 60 s rollover | **HARDWARE VERIFIED** |
| 38 | `0x0026` | 15 | Pump 2 Work Time — Min | UINT16 | `× 1 min` | `0–59` | Incremented on rollover | **HARDWARE VERIFIED** |
| 39 | `0x0027` | 16 | Pump 2 Work Time — Hour | UINT16 | `× 1 h` | — | `0` | **HARDWARE VERIFIED** |
| 40 | `0x0028` | 17 | Pump 3 Work Time — Sec | UINT16 | `× 1 s` | `0–59` | Ticking + 60 s rollover | **HARDWARE VERIFIED** |
| 41 | `0x0029` | 18 | Pump 3 Work Time — Min | UINT16 | `× 1 min` | `0–59` | Incremented on rollover | **HARDWARE VERIFIED** |
| 42 | `0x002A` | 19 | Pump 3 Work Time — Hour | UINT16 | `× 1 h` | — | `0` | **HARDWARE VERIFIED** |
| 43 | `0x002B` | 20 | Pump 4 Work Time — Sec | UINT16 | `× 1 s` | `0–59` | Ticking | **HARDWARE VERIFIED** |
| 44 | `0x002C` | 21 | Pump 4 Work Time — Min | UINT16 | `× 1 min` | `0–59` | `10` | **HARDWARE VERIFIED** |
| 45 | `0x002D` | 22 | Pump 4 Work Time — Hour | UINT16 | `× 1 h` | — | `0` | **HARDWARE VERIFIED** |
| 80 | `0x0050` | 25 | System Error Bitfield | BITFIELD16 | See §3.2 | 16-bit mask | `0x0000` (no faults) | **HARDWARE VERIFIED** |

*(Pumps 5 and 6 have no work-time registers documented in `BPC-02.pdf`.)*

### 3.2 System Error Bitfield — Holding Register `80` (`0x0050`)

Assumes standard LSB = bit 0 (`(raw ushr bit) and 1`). Any set bit outside `3–10` is surfaced as an undocumented-bit warning.

| Bit | Mask | Error Description (Manual p. 27) | Troubleshooting Summary (Manual p. 23) |
|---:|---:|---|---|
| 0–2 | `0x0007` | *(Undocumented)* | — |
| 3 | `0x0008` | Floater Error | Storage tank water level below limit |
| 4 | `0x0010` | Max Pressure Error | Water pressure exceeded configured maximum |
| 5 | `0x0020` | Pressure Sensor Disconnected | Sensor cable/connection open |
| 6 | `0x0040` | Control Phase Sensitivity Error | R/S/T phase imbalance exceeded limit |
| 7 | `0x0080` | Control Phase Max Voltage Error | Input voltage above upper limit |
| 8 | `0x0100` | Control Phase Min Voltage Error | Input voltage below lower limit |
| 9 | `0x0200` | Control Phase Sequence Error | Input phases R/S/T swapped |
| 10 | `0x0400` | Pump 1 Error | Listed in manual table as `Pump1 Error` *(Pumps 1–6 also have Coil fail bits `26–31`)* |
| 11–15 | `0xF800` | *(Undocumented)* | — |

### 3.3 Manual Control Words (LOCKED for Writes — Read-Only Tested via `FC03`)

| Dec Addr | Hex Addr | Manual Ref | Name | Scale / Values | Observed | Access in App |
|---:|---:|---|---|---|---|---|
| 50 | `0x0032` | Row 23 | Analog Output 0 Manual | `0–500` (`× 0.1 Hz`) | `0` | **LOCKED** (read OK; write FC unverified) |
| 51 | `0x0033` | Row 24 | Analog Output 1 Manual | `0–500` (`× 0.1 Hz`) | `0` | **LOCKED** (read OK; write FC unverified) |
| 99 | `0x0063` | Note p. 28 | Multi Function Register | `1234`: Manual, `0`: Auto | `0` | **LOCKED** (readable via `FC03`; write FC unverified) |

### 3.4 Hardware Configuration Words (On-Demand Read-Only via `FC03` — Never Written)

Recommended block reads: `4173 (qty 1)`, `4175 (qty 1)`, `4177 (qty 3)`, `4180 (qty 7)`, `4195 (qty 1)`.

| Dec Addr | Hex Addr | Manual Row | Name | Scale / Values | Documented Range | Observed on Hardware | Verification Status |
|---:|---:|---:|---|---|---|---|---|
| 4173 | `0x104D` | 56 | Control Status | `0–1` | `0–1` | `0` | **HARDWARE VERIFIED** |
| 4175 | `0x104F` | 57 | Sleep Gap | `× 0.01 bar` | `1–100` | `40` (`0.40 bar`) | **HARDWARE VERIFIED** |
| 4177 | `0x1051` | 58 | Auto Service Off Time | `× 1 h` | `1–9999` | `159` (`159 h`) | **HARDWARE VERIFIED** |
| 4178 | `0x1052` | 59 *(PDF: `417B`)* | Auto Service On Time | `× 0.1 s` | `1–999` | `33` (`3.3 s`) | **HARDWARE VERIFIED** |
| 4179 | `0x1053` | 60 | Auto Service Interval | `× 1 min` | `1–99` | `11` (`11 min`) | **HARDWARE VERIFIED** |
| 4180 | `0x1054` | 61 *(PDF: `41B0`)* | MFI1 Type | `0:Disable, 1:PS, 2:MP, 3:EMG, 4:CP, 5:FL` | `0–5` | `0` (`Disable`) | **HARDWARE VERIFIED** |
| 4181 | `0x1055` | 62 | MFI2 Type | Same as MFI1 | `0–5` | `0` (`Disable`) | **HARDWARE VERIFIED** |
| 4182 | `0x1056` | 63 | MFI3 Type | Same as MFI1 | `0–5` | `0` (`Disable`) | **HARDWARE VERIFIED** |
| 4183 | `0x1057` | 64 | MFI4 Type | Same as MFI1 | `0–5` | `0` (`Disable`) | **HARDWARE VERIFIED** |
| 4184 | `0x1058` | 65 | MFO1 Type | `0:Disable, 1:E Valve, 2:Fan, 3:Alarm, 4:DC3, 5:MC3` | `0–5` | `0` (`Disable`) | **HARDWARE VERIFIED** |
| 4185 | `0x1059` | 66 | MFO2 Type | Same as MFO1 | `0–5` | `0` (`Disable`) | **HARDWARE VERIFIED** |
| 4186 | `0x105A` | 67 | MFO3 Type | Same as MFO1 | `0–5` | `0` (`Disable`) | **HARDWARE VERIFIED** |
| 4195 | `0x1063` | 68 | ModBus Address | Slave ID (`1–247`) | `1` shown | `1` | **HARDWARE VERIFIED** |

---

## 4. Undocumented Registers Observed During Early Scans (DO NOT USE IN APP)

These Holding Registers returned valid `FC03` values during exploratory scans (`previouslogs`), but are **not documented** in `BPC-02.pdf`. Do not expose them in the UI unless DGP supplies an official specification.

| Dec Addr | Raw / Observed Values | Notes |
|---:|---|---|
| `1` | `0` | Gap between Page Number (`0`) and System Pressure (`2`) |
| `11` | `1000` (`0x03E8`) | Constant, matches Address `10` (`1000`) — possibly Drive 2 frequency (unconfirmed) |
| `12–14` | `18655–20447`, `6658–9002`, changing | Live varying internal values; meaning unknown |
| `15–22`, `24`, `27–29` | `0` | Constant zero during scan |
| `23` | `10` (`0x000A`) | Constant *(note: `BPC02_TEST_RESULTS.md` table shifted 23→25 and 25–26→27–28; raw hex confirms 23=10, 25=5000, 26=5000)* |
| `25–26` | `5000`, `5000` (`0x1388`) | Possibly 50.00 Hz internal parameter/limit (unconfirmed) |
| `30–33` | `~3217–3254`, slowly changing | Live varying internal values; meaning unknown |

---

## 5. Samin ES 487 Gateway Configuration Summary

| ES 487 Setting | Required Value for DGP BPC02 |
|---|---|
| Power Supply | `5–48 V DC` (12 V DC adapter included with ES 487) |
| RS485 Wiring | 2-wire `A (+)` to BPC02 `A`, `B (-)` to BPC02 `B`, plus common `GND`; `120 Ω` termination at bus ends |
| Serial Parameters | `9600` bps, `8` data bits, `No` parity, `1` stop bit (`9600 8N1`) |
| Serial Protocol | `Modbus RTU` |
| Network Mode | `TCP Server` |
| Protocol Conversion | `Modbus TCP/IP ↔ Modbus RTU` (or `Transparent` if using RTU-over-TCP framing in the app) |
| IP Configuration | `Static IP` recommended on plant LAN (or `DHCP` discovered via ES 487 IP Finder / Web UI) |
| TCP Port | Configured TCP server port in ES 487 Web UI (commonly `502`) |
| Target Unit / Slave ID | `1` (or the Slave ID configured in BPC02 `Hardware → Network`) |

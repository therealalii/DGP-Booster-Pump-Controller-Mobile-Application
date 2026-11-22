# BPC02 — MODBUS TEST RESULTS (DETAILED ANSWER TEXT)

Connection: Serial COM4, Modbus RTU, 9600 8N1, Slave ID 1.
All tests performed READ-ONLY (FC01/FC02/FC03/FC04 only). No writes sent.
Every frame below was captured from the supplied logs; representative frames are quoted.

Legend: responses are `ID FC ByteCount Data... CRC`. Register values are big-endian
16-bit words.

---

## PART 1 — WORD REGISTERS (FC03)

### TEST 00 / Previous logs — Addresses 0–9, 1, 2

| Log | Request | Register | Raw value | Decoded |
|---|---|---|---|---|
| previouslogs(1) | FC03 addr 2 qty 1 (`01 03 00 02 00 01 25 CA`) | 2 — System Pressure | 0x0000 | 0 (0.00 bar) |
| previouslogs(3) | FC03 addr 1 qty 1 (`01 03 00 01 00 01 D5 CA`) | 1 | 0x0000 | 0 |
| previouslogs(2) | FC03 addr 0 qty 10 (`01 03 00 00 00 0A C5 CD`) | 0–9 | all 0x0000 | all zero at time of test |

Result: `01 03 02 00 00 B8 44` every time — valid, stable responses.
At the moment of these reads the system pressure, voltages and frequency
registers were 0 (controller idle / no sensor input), so **no LCD comparison
of non-zero values was possible**. TEST 00 (LCD verification of addresses
0–10 scales) therefore remains **PENDING** — must be re-run with supply
voltage present and compared to the LCD.

### previouslogs(4) — Addresses 10–19 (`01 03 00 0A 00 0A E5 CF`)

| Reg | Raw | Dec | Interpretation |
|---|---|---|---|
| 10 | 03 E8 | 1000 | Drive Frequency — value constant 1000 (scale still unknown) |
| 11 | 03 E8 | 1000 | constant 1000 |
| 12 | 50 DF / 4F 80 / 49 62 … | 18655–20447 | changing |
| 13 | 25 82 / 19 FC / 23 2C … | 6658–9002 | changing |
| 14 | 25 85 / 35 CC / 23 30 … | changing | changing |
| 15–19 | 00 00 | 0 | constant |

Addresses 12–14 carry live changing values; 15–19 return 0. Units unknown.

### previouslogs(6) — Addresses 20–29 (`01 03 00 14 00 0A 85 C9`)

Response data: `00 00 00 00 00 00 00 0A 00 00 13 88 13 88 00 00 00 00 00 00`

| Reg | Raw | Dec |
|---|---|---|
| 20–24 | 0000 | 0 |
| 25 | 000A | 10 |
| 26 | 0000 | 0 |
| 27 | 1388 | 5000 |
| 28 | 1388 | 5000 |
| 29 | 0000 | 0 |

**Observation:** 5000 is exactly what a 50.00 Hz value looks like with 0.01 Hz
scaling. Registers 27–28 very likely hold frequency-type values (0.01 Hz),
even though the provisional map put "Frequency" at address 9 (which read 0).
The 20–29 block needs re-verification against the LCD; do not trust the
provisional address naming until then.

### previouslogs(5) — Addresses 30–39 (`01 03 00 1E 00 0A A5 CB`)

Response data: `0C B6 0C A5 0C 9B 0C 91 00 0E 00 36 00 00 00 39 00 35 00 00`

| Reg | Raw | Dec | Interpretation |
|---|---|---|---|
| 30 | 0C B6 | 3254 | changing |
| 31 | 0C A5 | 3237 | changing |
| 32 | 0C 9B | 3227 | changing |
| 33 | 0C 91 | 3217 | changing |
| 34 | 000E | 14 | Pump 1 Seconds |
| 35 | 0036 | 54 | Pump 1 Minutes |
| 36 | 0000 | 0 | Pump 1 Hours |
| 37 | 0039 | 57 | Pump 2 Seconds |
| 38 | 0035 | 53 | Pump 2 Minutes |
| 39 | 0000 | 0 | Pump 2 Hours |

Registers 30–33 change continuously (values ~3200, slowly falling in this
capture — could be countdowns, voltages or similar; unit unknown).

---

### TEST 01 — PUMP WORKING TIMES — **VERIFIED**

Request: `01 03 00 22 00 0C E5 C5` (FC03, addr 34, qty 12)
Response: `01 03 18 <24 data bytes> CRC` — 12 registers, pumps 1–4.

First frame decoded (09:35:55):

| Reg | Meaning | Raw | Value |
|---|---|---|---|
| 34 | Pump 1 Seconds | 0030 | 48 |
| 35 | Pump 1 Minutes | 000B | 11 |
| 36 | Pump 1 Hours | 0000 | 0 |
| 37 | Pump 2 Seconds | 0011 | 17 |
| 38 | Pump 2 Minutes | 000B | 11 |
| 39 | Pump 2 Hours | 0000 | 0 |
| 40 | Pump 3 Seconds | 0038 | 56 |
| 41 | Pump 3 Minutes | 000A | 10 |
| 42 | Pump 3 Hours | 0000 | 0 |
| 43 | Pump 4 Seconds | 0028 | 40 |
| 44 | Pump 4 Minutes | 000A | 10 |
| 45 | Pump 4 Hours | 0000 | 0 |

Behaviour observed over the ~35 s capture:
- All four pumps' second counters increment once per second (48→49→50…).
- **60-second rollover CONFIRMED twice**: e.g. Pump 1 seconds 0x3B (59) → 0x00
  while minutes 0x0B → 0x0C; Pump 3 seconds 0x3B → 0x00 with minutes 0x0A → 0x0B.
- Hours stay 0 (test duration << 1 h).
- Reading all 12 registers in one request works (no Exception 02).

Conclusion: map 34–45 = Pump1–4 Seconds/Minutes/Hours is **hardware-verified**.

### TEST 02 — ANALOG OUTPUTS — **DONE (both read 0)**

Request: `01 03 00 32 00 02 65 C4` (addr 50, qty 2)
Response: `01 03 04 00 00 00 00 FA 33` — constant in every frame.

- Register 50 (Analog Output 0 Manual, 0.1 Hz): 0x0000 = 0
- Register 51 (Analog Output 1 Manual, 0.1 Hz): 0x0000 = 0

Outputs were not activated (per safety rules), so 0 is the expected idle value.
Scaling (0.1 Hz) unverified against a non-zero value.

### TEST 03 — SYSTEM ERROR — **DONE (no active errors)**

Request: `01 03 00 50 00 01 84 1B` (addr 80, qty 1)
Response: `01 03 02 00 00 B8 44` — constant.

- Register 80 = 0x0000 → dec 0 / hex 0x0000 / bin 0000 0000 0000 0000.
- No documented bits set (Floater, Max Pressure, Pressure Sensor, Phase
  Sensitivity, Max/Min Phase Voltage, Phase Sequence, Pump 1 — all clear).
- No undocumented bits (0–2, 11–15) set.
- Consistent with no active errors on the controller. Bit decode remains
  unverified until a real fault is observed (fault injection forbidden).

### TEST 04 — CONTROL STATUS (4173) — **DONE (reads 0)**

Request: `01 03 10 4D 00 01 10 DD`
Response: `01 03 02 00 00 B8 44` — constant across the whole 3.5-minute log.

- Register 4173 = 0, inside documented range 0–1. Interpreted as
  "Automatic" mode, but the LCD/menu state at capture time was not recorded —
  confirm 0=Automatic / 1=Manual (or vice versa) on the next bench session.

### TEST 05 — SLEEP GAP (4175) — **DONE**

Request: `01 03 10 4F 00 01 B1 1D`
Response: `01 03 02 00 28 B8 5A` — constant.

- Register 4175 = 0x0028 = 40 → 40 × 0.01 bar = **0.40 bar**.
- Inside documented range 1–100. Compare with the Sleep Gap entry in the
  controller menu to close the verification.

### TEST 06 — AUTOMATIC SERVICE SETTINGS (4177–4179) — **DONE**

Request (qty 3 worked, no fallback needed): `01 03 10 51 00 03 50 DA`
Response: `01 03 06 00 9F 00 21 00 0B A4 A4` — constant.

| Reg | Meaning | Raw | Value |
|---|---|---|---|
| 4177 | Auto Service Off Time (Hour) | 009F | 159 h |
| 4178 | Auto Service On Time (0.1 s) | 0021 | 33 → 3.3 s |
| 4179 | Auto Service Interval (Min) | 000B | 11 min |

The PDF print typo confirmed: reading 4177 qty 3 returns the real 4178
between them (no gap at the printed "417B"). Compare the three numbers with
the controller menu to mark verified.

### TEST 07 — MFI / MFO TYPES (4180–4186) — **DONE (all 0)**

Request: `01 03 10 54 00 07 41 18`
Response: `01 03 0E 00 00 … (14 bytes, all 0x0000) EF 15`

- 4180–4183 (MFI 1–4 Type) = 0 and 4184–4186 (MFO 1–3 Type) = 0.
- All inside documented range 0–5; type 0 presumably "not used / none".
  Reading qty 7 in one request is supported.

### TEST 08 — MODBUS ADDRESS (4195) — **VERIFIED**

Request: `01 03 10 63 00 01 70 D4`
Response: `01 03 02 00 01 79 84`

- Register 4195 = 1 → matches Slave ID 1 used on the bus and the manual.

### TEST 08B — MULTI FUNCTION REGISTER (addr 99, read-only check) — **DONE**

Request: `01 03 00 63 00 01 74 14`
Response: `01 03 02 00 00 B8 44` — readable, returns 0 (no exception).

- Address 99 is readable as a normal Holding Register and returns 0, matching
  the manual's description (0 in Automatic mode). **Nothing was written to it.**

---

## PART 2 — BIT REGISTERS: FC01 WORKS, FC02 DOES NOT

### TEST 09A — STATUS BITS, FC01 (coils 0–33) — **VERIFIED**

Request: `01 01 00 00 00 22 BC 13` (FC01, addr 0, qty 34)
Response: `01 01 05 3F 00 F8 01 00 05 36`

5 data bytes → bit map (bit 0 = coil 0):

| Bytes | Hex | Bits set | Meaning |
|---|---|---|---|
| 0 | 3F | 0–5 | Output 0–5 Status ON |
| 1 | 00 | — | Outputs 6–9 OFF, DR OFF (bit 10) |
| 2 | F8 | 19–23 | **Pump 1–5 Status ON** |
| 3 | 01 | 24 | **Pump 6 Status ON** |
| 4 | 00 | — | Pump Fail bits (25–26…), Floater L/H (32–33) OFF |

So in this capture: Outputs 0–5 active, all six Pumps running, no drive/pump
failures, both floaters low. Compare with the pump/LED state at 10:13 to close
the loop; the group layout (0–9 outputs, 19–24 pumps, 25+ fails, 32–33
floaters) is consistent and readable in one request.

### TEST 09B — STATUS BITS, FC02 — **NOT SUPPORTED**

Request: `01 02 00 00 00 22 F8 13`
Response: `01 02 81 E1` — **truncated 4-byte exception frame** (a valid
exception is 5 bytes: ID, 0x82, code, CRC). The log shows the slave answering
with an exception (high bit pattern), but the capture is missing its final
byte(s). Same truncated shape appears in every FC02 test below.

### TEST 10A — MANUAL OUTPUT BITS, FC01 (coils 50–59) — **DONE**

Request: `01 01 00 32 00 0A 1D C2`
Response: `01 01 02 00 00 B9 FC` → all 10 manual-output bits = 0 (OFF).

### TEST 10B — same range, FC02 — exception (truncated frame `01 02 81 E1`).

### TEST 11A — PUMP ACTIVATION BITS, FC01 (coils 4096–4101) — **VERIFIED**

Request: `01 01 10 00 00 06 B8 C8`
Response: `01 01 01 FF 11 C8` → 0xFF = all six bits set.

- Pump 1–6 Activation all ON (coils 4096–4101 = 1,1,1,1,1,1).

### TEST 11B — same range, FC02 — exception (truncated `01 02 81 E1`).

### TEST 12A — HARDWARE CONFIGURATION BITS, FC01 (coils 4106–4110) — **DONE**

Request: `01 01 10 0A 00 05 D8 CB`
Response: `01 01 01 D0 50 14` → 0xD0 = 1101 0000

| Coil | Setting | Bit | Value |
|---|---|---|---|
| 4106 | Control Phase Activation | 0 | 0 (OFF) |
| 4107 | OFF Changing Activation | 1 | 0 (OFF) |
| 4108 | Time Changing Activation | 2 | 0 (OFF) |
| 4109 | Automatic Reserve Activation | 3 | 1 (ON) |
| 4110 | Automatic Reserve Type (NO/NC) | 4 | 1 (NC) |

Check 0xD0 against the controller's Hardware menu to mark fully verified.

### TEST 12B — same range, FC02 — exception (truncated `01 02 81 E1`).

**Part 2 conclusion:** Bit addresses are served by **FC01 (Read Coils)**.
Every FC02 request returned an exception. FC02 is not usable on this slave;
mark all bit groups FC01-only.

---

## PART 3 — FC04

### TEST 13 — INPUT REGISTER COMPARISON — **FC04 NOT SUPPORTED (as far as logged)**

Request: `01 04 00 02 00 01 90 0A` (FC04, addr 2, qty 1)
Response: `01 04 01 E3` — again a **truncated 4-byte frame**.

A valid FC04 response would be 7 bytes (`01 04 02 HI LO CRC CRC`) and a valid
exception 5 bytes (`01 84 code CRC CRC`). The slave does reply (it is not
silent and does not echo), and the pattern matches an exception reply whose
tail was lost in capture, i.e. almost certainly **Exception 01 (illegal
function) → Words are FC03-only**.

Action for the next session: repeat this single request with a tool that logs
full frames (e.g. Modbus Poll as planned) and save the complete 5-byte
exception, or simply accept FC03-only. FC04 must not be assumed to work.

---

## SUMMARY TABLE

| Test | Register/Bit range | Result | Status |
|---|---|---|---|
| 00 | 0–10 vs LCD | values 0 (idle), no non-zero LCD compare possible | PENDING (re-run live) |
| 01 | 34–45 pump timers | seconds tick, 60 s rollover ×2, minutes/hours correct | **VERIFIED** |
| 02 | 50–51 analog outputs | both 0x0000 | DONE (idle values) |
| 03 | 80 system error | 0x0000, no bits | DONE (no faults present) |
| 04 | 4173 control status | 0 (range 0–1) | DONE |
| 05 | 4175 sleep gap | 40 → 0.40 bar | DONE |
| 06 | 4177–4179 auto service | 159 h / 3.3 s / 11 min; qty-3 read works; "417B" is a print typo | DONE |
| 07 | 4180–4186 MFI/MFO | all 0 (range 0–5) | DONE |
| 08 | 4195 Modbus address | 1 = Slave ID | **VERIFIED** |
| 08B | 99 multi function | readable, returns 0, not written | DONE |
| 09A | coils 0–33 status | FC01 OK: outputs 0–5 on, pumps 1–6 running, no fails | VERIFIED layout |
| 09B | FC02 on 0–33 | exception (truncated capture) | FC02 NOT supported |
| 10A/B | coils 50–59 | FC01: all OFF; FC02: exception | DONE |
| 11A/B | coils 4096–4101 | FC01: 0xFF — all 6 pumps activated; FC02: exception | **VERIFIED** |
| 12A/B | coils 4106–4110 | FC01: 0xD0 → reserve ON, NC type; FC02: exception | DONE |
| 13 | FC04 | truncated exception reply → FC03-only | DONE (confirm once with full capture) |

## KEY CONCLUSIONS

1. **Function codes:** FC03 for all word registers; **FC01 for all bit
   registers**; FC02 and FC04 are rejected (exception). Words are FC03-only.
2. **Zero-based addressing confirmed throughout** (34/37 timers, 4195 = slave
   ID 1, etc.). No ±1 offset anywhere.
3. **Multi-register reads work** up to at least qty 12 (FC03) and qty 34
   (FC01); the "417B"/"41B0" entries in the PDF are print typos — the real
   registers 4178 and 4180 sit exactly where decimal addressing puts them.
4. **Outstanding items:** TEST 00 LCD comparison with live non-zero values
   (pressure/voltages/frequency and Word 10 scale); confirm Word 27–28
   (5000 = 50.00 Hz?) naming; confirm 4173 polarity and 412A menu values;
   capture one full FC02/FC04 exception frame with CRC for the record; then
   Part 4 (ETMO-10 gateway pass-through) and Part 5 sign-off.
5. **Safety:** no write function (FC05/06/15/16) appears anywhere in the
   logs; address 99 was only read. The read-only rule was respected.

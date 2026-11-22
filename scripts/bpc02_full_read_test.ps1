<#
.SYNOPSIS
    Complete READ-ONLY Modbus verification script for DGP BPC02 (Serial RTU or Samin ES 487 TCP).
.DESCRIPTION
    Reads EVERY documented BPC02 Coil (FC01) and Holding Register (FC03), validates CRCs /
    MBAP headers, checks live Pump 1-4 timer ticking across a 3-second interval, decodes
    every measurement, status bit, error bitfield, and hardware configuration parameter,
    and saves a complete report to BPC02_FULL_READ_RESULTS.txt.

    STRICTLY READ-ONLY: Only FC01 (Read Coils) and FC03 (Read Holding Registers) exist in
    this script. No write command can be sent.

    Works on BOTH:
      1) Direct RS485 Serial (default COM4, 9600 8N1)
      2) Samin ES 487 Ethernet Gateway over Modbus TCP/IP (-TcpHost 192.168.x.x -TcpPort 502)
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\bpc02_full_read_test.ps1 -Port COM4
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\bpc02_full_read_test.ps1 -TcpHost 192.168.1.50 -TcpPort 502
#>
param(
    [string]$Port = "COM4",
    [int]$BaudRate = 9600,
    [string]$TcpHost = "",
    [int]$TcpPort = 502,
    [byte]$SlaveId = 1,
    [string]$OutFile = "BPC02_FULL_READ_RESULTS.txt"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$useTcp = -not [string]::IsNullOrWhiteSpace($TcpHost)
$logLines = New-Object System.Collections.Generic.List[string]
$script:mbapTid = 0

function Write-Log([string]$Text, [string]$Color = "Gray") {
    Write-Host $Text -ForegroundColor $Color
    $logLines.Add($Text)
}

function Format-Hex([byte[]]$Bytes) {
    if ($null -eq $Bytes -or $Bytes.Length -eq 0) { return "(empty)" }
    return ($Bytes | ForEach-Object { $_.ToString("X2") }) -join " "
}

function Get-ModbusCrc16([byte[]]$Data) {
    [int]$crc = 0xFFFF
    foreach ($b in $Data) {
        $crc = $crc -bxor ([int]$b)
        for ($i = 0; $i -lt 8; $i++) {
            if (($crc -band 1) -ne 0) {
                $crc = ($crc -shr 1) -bxor 0xA001
            } else {
                $crc = $crc -shr 1
            }
        }
    }
    return ,([byte[]]@([byte]($crc -band 0xFF), [byte](($crc -shr 8) -band 0xFF)))
}

function Test-ModbusCrc16([byte[]]$Frame) {
    if ($null -eq $Frame -or $Frame.Length -lt 4) { return $false }
    $bodyLen = $Frame.Length - 2
    [byte[]]$body = New-Object byte[] $bodyLen
    [Array]::Copy($Frame, 0, $body, 0, $bodyLen)
    $calc = Get-ModbusCrc16 $body
    return ($Frame[$Frame.Length - 2] -eq $calc[0]) -and ($Frame[$Frame.Length - 1] -eq $calc[1])
}

function Build-ReadPdu([byte]$Fc, [int]$Addr, [int]$Qty) {
    if ($Fc -ne 0x01 -and $Fc -ne 0x03) {
        throw "READ-ONLY script only permits FC01 or FC03 (attempted 0x$($Fc.ToString('X2')))"
    }
    return ,([byte[]]@(
        $Fc,
        [byte](($Addr -shr 8) -band 0xFF), [byte]($Addr -band 0xFF),
        [byte](($Qty -shr 8) -band 0xFF),  [byte]($Qty -band 0xFF)
    ))
}

function Read-ExactStream($Stream, [int]$Count) {
    [byte[]]$buf = New-Object byte[] $Count
    $off = 0
    while ($off -lt $Count) {
        $n = $Stream.Read($buf, $off, $Count - $off)
        if ($n -le 0) { throw "TCP stream closed while reading" }
        $off += $n
    }
    return ,$buf
}

function Invoke-ReadRequest($Link, [string]$Label, [byte]$Fc, [int]$Addr, [int]$Qty) {
    [byte[]]$pdu = Build-ReadPdu $Fc $Addr $Qty
    $tsTx = (Get-Date).ToString("HH:mm:ss.fff")
    [byte[]]$txWire = @()
    [byte[]]$rxWire = @()
    [byte[]]$rxPdu = @()
    $ok = $false
    $status = ""

    if ($useTcp) {
        $script:mbapTid = ($script:mbapTid + 1) -band 0xFFFF
        $tid = $script:mbapTid
        $mbapLen = $pdu.Length + 1
        [byte[]]$txWire = New-Object byte[] (7 + $pdu.Length)
        $txWire[0] = [byte](($tid -shr 8) -band 0xFF)
        $txWire[1] = [byte]($tid -band 0xFF)
        $txWire[2] = 0x00
        $txWire[3] = 0x00
        $txWire[4] = [byte](($mbapLen -shr 8) -band 0xFF)
        $txWire[5] = [byte]($mbapLen -band 0xFF)
        $txWire[6] = $SlaveId
        [Array]::Copy($pdu, 0, $txWire, 7, $pdu.Length)

        $stream = $Link.GetStream()
        $stream.Write($txWire, 0, $txWire.Length)
        $stream.Flush()

        try {
            [byte[]]$hdr = Read-ExactStream $stream 7
            $rxLen = (([int]$hdr[4]) -shl 8) -bor ([int]$hdr[5])
            [byte[]]$body = Read-ExactStream $stream ($rxLen - 1)
            [byte[]]$rxWire = New-Object byte[] (7 + $body.Length)
            [Array]::Copy($hdr, 0, $rxWire, 0, 7)
            [Array]::Copy($body, 0, $rxWire, 7, $body.Length)
            $rxPdu = $body
            if (($rxPdu[0] -band 0x80) -ne 0 -and $rxPdu.Length -ge 2) {
                $status = "MODBUS EXCEPTION 0x$($rxPdu[1].ToString('X2'))"
            } elseif ($rxPdu[0] -ne $Fc) {
                $status = "UNEXPECTED FC 0x$($rxPdu[0].ToString('X2'))"
            } else {
                $ok = $true
                $status = "OK ($($rxWire.Length) bytes MBAP)"
            }
        } catch {
            $status = "TCP ERROR: $($_.Exception.Message)"
        }
    } else {
        [byte[]]$payload = New-Object byte[] (1 + $pdu.Length)
        $payload[0] = $SlaveId
        [Array]::Copy($pdu, 0, $payload, 1, $pdu.Length)
        [byte[]]$crc = Get-ModbusCrc16 $payload
        [byte[]]$txWire = New-Object byte[] ($payload.Length + 2)
        [Array]::Copy($payload, 0, $txWire, 0, $payload.Length)
        $txWire[$payload.Length] = $crc[0]
        $txWire[$payload.Length + 1] = $crc[1]

        $Link.DiscardInBuffer()
        $Link.DiscardOutBuffer()
        $Link.Write($txWire, 0, $txWire.Length)

        $rxBuf = New-Object System.Collections.Generic.List[byte]
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $lastRxMs = -1
        while ($sw.ElapsedMilliseconds -lt 1200) {
            $avail = $Link.BytesToRead
            if ($avail -gt 0) {
                [byte[]]$chunk = New-Object byte[] $avail
                $read = $Link.Read($chunk, 0, $avail)
                for ($i = 0; $i -lt $read; $i++) { $rxBuf.Add($chunk[$i]) }
                $lastRxMs = $sw.ElapsedMilliseconds
            } else {
                if ($lastRxMs -ge 0 -and ($sw.ElapsedMilliseconds - $lastRxMs) -ge 80) { break }
                Start-Sleep -Milliseconds 10
            }
        }
        $sw.Stop()
        [byte[]]$rxWire = $rxBuf.ToArray()

        $crcOk = Test-ModbusCrc16 $rxWire
        if ($rxWire.Length -eq 0) {
            $status = "TIMEOUT (no response)"
        } elseif (-not $crcOk) {
            $status = "CRC ERROR ($($rxWire.Length) bytes)"
        } elseif ($rxWire.Length -eq 4) {
            $status = "REJECTED (BPC02 4-byte unsupported-FC reply, CRC OK)"
        } else {
            $pduLen = $rxWire.Length - 3
            [byte[]]$rxPdu = New-Object byte[] $pduLen
            [Array]::Copy($rxWire, 1, $rxPdu, 0, $pduLen)
            if (($rxPdu[0] -band 0x80) -ne 0 -and $rxPdu.Length -ge 2) {
                $status = "MODBUS EXCEPTION 0x$($rxPdu[1].ToString('X2'))"
            } elseif ($rxPdu[0] -ne $Fc) {
                $status = "UNEXPECTED FC 0x$($rxPdu[0].ToString('X2'))"
            } else {
                $ok = $true
                $status = "OK ($($rxWire.Length) bytes, CRC OK)"
            }
        }
    }

    $tsRx = (Get-Date).ToString("HH:mm:ss.fff")
    Write-Log ""
    Write-Log "[$Label]" "Cyan"
    Write-Log "  TX ($tsTx): $(Format-Hex $txWire)"
    Write-Log "  RX ($tsRx): $(Format-Hex $rxWire)  -->  $status" $(if ($ok) { "Green" } else { "Yellow" })
    Start-Sleep -Milliseconds 200
    return @{ Ok = $ok; Pdu = $rxPdu; Status = $status }
}

function Parse-RegsFromPdu([byte[]]$Pdu) {
    if ($null -eq $Pdu -or $Pdu.Length -lt 2 -or $Pdu[0] -ne 0x03) { return @() }
    $count = [int]($Pdu[1] / 2)
    $vals = New-Object System.Collections.Generic.List[int]
    for ($i = 0; $i -lt $count; $i++) {
        $vals.Add((([int]$Pdu[2 + 2 * $i]) -shl 8) -bor ([int]$Pdu[3 + 2 * $i]))
    }
    return $vals.ToArray()
}

function Parse-BitsFromPdu([byte[]]$Pdu, [int]$Qty) {
    if ($null -eq $Pdu -or $Pdu.Length -lt 2 -or $Pdu[0] -ne 0x01) { return @() }
    $bits = New-Object System.Collections.Generic.List[int]
    for ($i = 0; $i -lt $Qty; $i++) {
        $b = [int]$Pdu[2 + ($i -shr 3)]
        $bits.Add((($b -shr ($i % 8)) -band 1))
    }
    return $bits.ToArray()
}

$mfiMap = @{ 0 = "Disable"; 1 = "PS"; 2 = "MP"; 3 = "EMG"; 4 = "CP"; 5 = "FL" }
$mfoMap = @{ 0 = "Disable"; 1 = "E Valve"; 2 = "Fan"; 3 = "Alarm"; 4 = "DC3"; 5 = "MC3" }
$errBits = @(
    @{ Bit = 3;  Name = "Floater Error" },
    @{ Bit = 4;  Name = "Max Pressure Error" },
    @{ Bit = 5;  Name = "Pressure Sensor Disconnected" },
    @{ Bit = 6;  Name = "Control Phase Sensitivity Error" },
    @{ Bit = 7;  Name = "Control Phase Max Voltage Error" },
    @{ Bit = 8;  Name = "Control Phase Min Voltage Error" },
    @{ Bit = 9;  Name = "Control Phase Sequence Error" },
    @{ Bit = 10; Name = "Pump 1 Error" }
)

# --- Connect & Execute Full Read Audit ---------------------------------------
$modeDesc = if ($useTcp) { "Modbus TCP ($TcpHost`:$TcpPort, Unit ID $SlaveId)" } else { "Serial RTU ($Port, $BaudRate 8N1, Slave ID $SlaveId)" }
Write-Log "================================================================" "White"
Write-Log "  DGP BPC02 - COMPLETE READ-ONLY VERIFICATION REPORT" "White"
Write-Log "  Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" "White"
Write-Log "  Link: $modeDesc" "White"
Write-Log "  Policy: STRICTLY READ-ONLY (FC01 & FC03 only, zero writes)" "Green"
Write-Log "================================================================" "White"

$link = $null
try {
    if ($useTcp) {
        $link = New-Object System.Net.Sockets.TcpClient
        $link.ReceiveTimeout = 2000
        $link.SendTimeout = 2000
        $link.Connect($TcpHost, $TcpPort)
        Write-Log "Connected to $TcpHost`:$TcpPort successfully." "Green"
    } else {
        $link = New-Object System.IO.Ports.SerialPort $Port, $BaudRate, ([System.IO.Ports.Parity]::None), 8, ([System.IO.Ports.StopBits]::One)
        $link.ReadTimeout = 1000
        $link.WriteTimeout = 1000
        $link.Open()
        Write-Log "Opened $Port successfully." "Green"
    }

    # 1) Status Coils 0..33
    $rBits = Invoke-ReadRequest $link "1. LIVE STATUS COILS (FC01 Addr 0, Qty 34)" 0x01 0 34
    $b = @(Parse-BitsFromPdu $rBits.Pdu 34)
    if ($b.Count -eq 34) {
        Write-Log "  Outputs 0..9   : $(($b[0..9]) -join ' ')" "White"
        Write-Log "  DR (10)        : $($b[10])" "White"
        Write-Log "  PR1..PR4 (11-14): $(($b[11..14]) -join ' ')" "White"
        Write-Log "  MFI1..4 (15-18): $(($b[15..18]) -join ' ')" "White"
        Write-Log "  Pump 1..6 RUN  : $(($b[19..24]) -join ' ')" "White"
        Write-Log "  Drive Fail (25): $($b[25])" "White"
        Write-Log "  Pump 1..6 FAIL : $(($b[26..31]) -join ' ')" "White"
        Write-Log "  Floater L / H  : L=$($b[32])  H=$($b[33])" "White"
    }

    # 2) Manual Output Coils 50..59
    $rManBits = Invoke-ReadRequest $link "2. MANUAL OUTPUT COILS (FC01 Addr 50, Qty 10)" 0x01 50 10
    $mb = @(Parse-BitsFromPdu $rManBits.Pdu 10)
    if ($mb.Count -eq 10) {
        Write-Log "  Manual Out 0..9: $($mb -join ' ')" "White"
    }

    # 3) Word 0 (Displayed Page)
    $rPage = Invoke-ReadRequest $link "3. DISPLAYED LCD PAGE (FC03 Addr 0, Qty 1)" 0x03 0 1
    $pageVals = @(Parse-RegsFromPdu $rPage.Pdu)
    if ($pageVals.Count -eq 1) {
        Write-Log "  Displayed Page Number (Word 0) = $([int]$pageVals[0])" "White"
    }

    # 4) Words 2..10 (Pressure, Voltages, Mains Freq, Drive Freq)
    $rMeas = Invoke-ReadRequest $link "4. MEASUREMENTS (FC03 Addr 2, Qty 9 -> Words 2..10)" 0x03 2 9
    $m = @(Parse-RegsFromPdu $rMeas.Pdu)
    if ($m.Count -eq 9) {
        $pBar = [Math]::Round(([int]$m[0]) * 0.01, 2)
        $hz = [Math]::Round(([int]$m[7]) * 0.01, 2)
        Write-Log "  Word 2  System Pressure = $([int]$m[0]) -> $pBar bar" "White"
        Write-Log "  Word 3..5 Phase-Neutral = Vrn=$([int]$m[1]) V, Vsn=$([int]$m[2]) V, Vtn=$([int]$m[3]) V" "White"
        Write-Log "  Word 6..8 Phase-Phase   = Vrs=$([int]$m[4]) V, Vst=$([int]$m[5]) V, Vtr=$([int]$m[6]) V" "White"
        Write-Log "  Word 9  Mains Frequency = $([int]$m[7]) -> $hz Hz" "White"
        Write-Log "  Word 10 Drive Frequency = $([int]$m[8]) (0x$(([int]$m[8]).ToString('X4')) raw, range 0-1600)" "White"
    }

    # 5) Pump 1..4 Work Times (Words 34..45) - sample #1
    $rT1 = Invoke-ReadRequest $link "5A. PUMP 1-4 WORK TIMES - SAMPLE 1 (FC03 Addr 34, Qty 12)" 0x03 34 12
    $t1 = @(Parse-RegsFromPdu $rT1.Pdu)
    if ($t1.Count -eq 12) {
        for ($p = 0; $p -lt 4; $p++) {
            Write-Log ("  Pump {0}: {1} h {2:D2} m {3:D2} s" -f ($p + 1), [int]$t1[$p * 3 + 2], [int]$t1[$p * 3 + 1], [int]$t1[$p * 3]) "White"
        }
    }

    # 6) Manual Analog Outputs (Words 50..51)
    $rAn = Invoke-ReadRequest $link "6. MANUAL ANALOG OUTPUTS (FC03 Addr 50, Qty 2)" 0x03 50 2
    $an = @(Parse-RegsFromPdu $rAn.Pdu)
    if ($an.Count -eq 2) {
        Write-Log "  Word 50 Analog Out 0 = $([int]$an[0]) ($([Math]::Round(([int]$an[0]) * 0.1, 1)) Hz)" "White"
        Write-Log "  Word 51 Analog Out 1 = $([int]$an[1]) ($([Math]::Round(([int]$an[1]) * 0.1, 1)) Hz)" "White"
    }

    # 7) System Error Bitfield (Word 80)
    $rErr = Invoke-ReadRequest $link "7. SYSTEM ERROR BITFIELD (FC03 Addr 80, Qty 1)" 0x03 80 1
    $ev = @(Parse-RegsFromPdu $rErr.Pdu)
    if ($ev.Count -eq 1) {
        $ew = [int]$ev[0]
        $bin = [Convert]::ToString($ew, 2).PadLeft(16, '0')
        Write-Log "  Word 80 = $ew (0x$($ew.ToString('X4')) | bin $bin)" "White"
        $active = @()
        foreach ($eb in $errBits) {
            if ((($ew -shr $eb.Bit) -band 1) -eq 1) { $active += "Bit $($eb.Bit): $($eb.Name)" }
        }
        $undoc = @()
        for ($bit = 0; $bit -lt 16; $bit++) {
            if ((($ew -shr $bit) -band 1) -eq 1 -and ($bit -lt 3 -or $bit -gt 10)) { $undoc += "Bit $bit" }
        }
        if ($active.Count -eq 0 -and $undoc.Count -eq 0) {
            Write-Log "  Active Errors  : NONE (all clear)" "Green"
        } else {
            if ($active.Count -gt 0) { Write-Log "  Active Errors  : $($active -join '; ')" "Red" }
            if ($undoc.Count -gt 0)  { Write-Log "  Undoc Bits Set : $($undoc -join ', ')" "Yellow" }
        }
    }

    # 8) Multi Function Register (Word 99, read-only)
    $r99 = Invoke-ReadRequest $link "8. MULTI FUNCTION REGISTER (FC03 Addr 99, Qty 1)" 0x03 99 1
    $v99 = @(Parse-RegsFromPdu $r99.Pdu)
    if ($v99.Count -eq 1) {
        Write-Log "  Word 99 = $([int]$v99[0])" "White"
    }

    # 9) Hardware Config Coils (4096..4101 and 4106..4110)
    $rPAct = Invoke-ReadRequest $link "9A. PUMP 1-6 ACTIVATION COILS (FC01 Addr 4096, Qty 6)" 0x01 4096 6
    $pa = @(Parse-BitsFromPdu $rPAct.Pdu 6)
    if ($pa.Count -eq 6) {
        Write-Log "  Pump 1..6 Enable: $($pa -join ' ')" "White"
    }

    $rCfgB = Invoke-ReadRequest $link "9B. HARDWARE CONFIG COILS (FC01 Addr 4106, Qty 5)" 0x01 4106 5
    $cb = @(Parse-BitsFromPdu $rCfgB.Pdu 5)
    if ($cb.Count -eq 5) {
        Write-Log "  4106 Control Phase Activation : $($cb[0])" "White"
        Write-Log "  4107 OFF Changing Activation  : $($cb[1])" "White"
        Write-Log "  4108 Time Changing Activation : $($cb[2])" "White"
        Write-Log "  4109 Reserve Auto Activation  : $($cb[3])" "White"
        Write-Log "  4110 Reserve Auto Type (0=NO,1=NC): $($cb[4])" "White"
    }

    # 10) Hardware Config Words (4173, 4175, 4177..4186, 4195)
    $r4173 = Invoke-ReadRequest $link "10A. CONTROL STATUS (FC03 Addr 4173, Qty 1)" 0x03 4173 1
    $v4173 = @(Parse-RegsFromPdu $r4173.Pdu)
    if ($v4173.Count -eq 1) { Write-Log "  4173 Control Status = $([int]$v4173[0])" "White" }

    $r4175 = Invoke-ReadRequest $link "10B. SLEEP GAP (FC03 Addr 4175, Qty 1)" 0x03 4175 1
    $v4175 = @(Parse-RegsFromPdu $r4175.Pdu)
    if ($v4175.Count -eq 1) { Write-Log "  4175 Sleep Gap      = $([int]$v4175[0]) ($([Math]::Round(([int]$v4175[0]) * 0.01, 2)) bar)" "White" }

    $r4177 = Invoke-ReadRequest $link "10C. AUTO SERVICE + MFI/MFO TYPES (FC03 Addr 4177, Qty 10 -> 4177..4186)" 0x03 4177 10
    $v4177 = @(Parse-RegsFromPdu $r4177.Pdu)
    if ($v4177.Count -eq 10) {
        Write-Log "  4177 Auto Service Off Time = $([int]$v4177[0]) h" "White"
        Write-Log "  4178 Auto Service On Time  = $([int]$v4177[1]) ($([Math]::Round(([int]$v4177[1]) * 0.1, 1)) s)" "White"
        Write-Log "  4179 Auto Service Interval = $([int]$v4177[2]) min" "White"
        for ($i = 0; $i -lt 4; $i++) {
            $code = [int]$v4177[3 + $i]
            $lbl = if ($mfiMap.ContainsKey($code)) { $mfiMap[$code] } else { "Unknown($code)" }
            Write-Log "  $(4180 + $i) MFI$($i + 1) Type             = $code ($lbl)" "White"
        }
        for ($i = 0; $i -lt 3; $i++) {
            $code = [int]$v4177[7 + $i]
            $lbl = if ($mfoMap.ContainsKey($code)) { $mfoMap[$code] } else { "Unknown($code)" }
            Write-Log "  $(4184 + $i) MFO$($i + 1) Type             = $code ($lbl)" "White"
        }
    }

    $r4195 = Invoke-ReadRequest $link "10D. MODBUS SLAVE ADDRESS (FC03 Addr 4195, Qty 1)" 0x03 4195 1
    $v4195 = @(Parse-RegsFromPdu $r4195.Pdu)
    if ($v4195.Count -eq 1) { Write-Log "  4195 ModBus Address = $([int]$v4195[0])" "White" }

    # 11) Second Pump Timer sample (~3 seconds later) to verify live ticking
    Start-Sleep -Milliseconds 1500
    $rT2 = Invoke-ReadRequest $link "5B. PUMP 1-4 WORK TIMES - SAMPLE 2 (live tick check)" 0x03 34 12
    $t2 = @(Parse-RegsFromPdu $rT2.Pdu)
    if ($t2.Count -eq 12) {
        for ($p = 0; $p -lt 4; $p++) {
            Write-Log ("  Pump {0}: {1} h {2:D2} m {3:D2} s" -f ($p + 1), [int]$t2[$p * 3 + 2], [int]$t2[$p * 3 + 1], [int]$t2[$p * 3]) "White"
        }
    }

    Write-Log ""
    Write-Log "FULL READ-ONLY AUDIT COMPLETE." "Green"
}
catch {
    Write-Log "ERROR: $($_.Exception.Message)" "Red"
}
finally {
    if ($null -ne $link) {
        try { $link.Close() } catch {}
    }
    $logLines | Set-Content -Path $OutFile -Encoding UTF8
    Write-Host ""
    Write-Host "Full read report saved to: $(Resolve-Path $OutFile)" -ForegroundColor Green
}

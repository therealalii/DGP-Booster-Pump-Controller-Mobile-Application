package com.dgp.boosterpump.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.controller.RegisterReading
import com.dgp.boosterpump.controller.register.AccessPolicy
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.RegisterGroup
import com.dgp.boosterpump.controller.register.Verification
import com.dgp.boosterpump.modbus.ReadFunction
import com.dgp.boosterpump.modbus.TransactionRecord
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.ui.PersianLabels
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.LtrText
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.ScadaPanel
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.ui.components.formatTime
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.ui.theme.MonoStyle
import com.dgp.boosterpump.viewmodel.MonitoringUiState

private enum class DiagTab(val label: String) {
    PROBE("تست خواندن"),
    LOG("لاگ TX/RX"),
    MAP("نقشه رجیستر"),
}

/**
 * Persian Engineering Console with 3 views:
 *  1. Read-only Modbus probe (FC01/FC03 recommended; FC02/FC04 flagged unsupported on BPC02)
 *  2. Dark monospace TX/RX raw frame terminal log
 *  3. Searchable/filterable centralized BPC02 register map with live values
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnosticsScreen(
    uiState: MonitoringUiState,
    onRunProbe: (ReadFunction, Int, Int) -> Unit,
    onClearLog: () -> Unit,
) {
    val p = LocalScadaPalette.current
    var activeTab by remember { mutableStateOf(DiagTab.PROBE) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tab selector bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DiagTab.entries.forEach { tab ->
                val countSuffix = when (tab) {
                    DiagTab.LOG -> " (${uiState.transactions.size})"
                    DiagTab.MAP -> " (${Bpc02RegisterMap.all.size})"
                    DiagTab.PROBE -> ""
                }
                FilterChip(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    label = { Text("${tab.label}$countSuffix", maxLines = 1) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        when (activeTab) {
            DiagTab.PROBE -> ProbeTabContent(uiState = uiState, onRunProbe = onRunProbe)
            DiagTab.LOG -> TransactionLogTabContent(uiState = uiState, onClearLog = onClearLog)
            DiagTab.MAP -> RegisterMapTabContent(uiState = uiState)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProbeTabContent(
    uiState: MonitoringUiState,
    onRunProbe: (ReadFunction, Int, Int) -> Unit,
) {
    val p = LocalScadaPalette.current
    var selectedFn by remember { mutableStateOf(ReadFunction.READ_HOLDING_REGISTERS) }
    var addrText by remember { mutableStateOf("2") }
    var qtyText by remember { mutableStateOf("9") }
    var inputError by remember { mutableStateOf<String?>(null) }
    val isConnected = uiState.connectionState is ConnectionState.Connected

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScadaPanel(
                title = "تست خواندن مستقیم رجیستر (فقط خواندنی)",
                subtitle = "ارسال یک درخواست تکی FC01 یا FC03 روی ارتباط فعال برای بررسی پاسخ خام کنترلر",
                icon = Icons.Outlined.BugReport,
                accentColor = p.cyan,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ReadFunction.entries.forEach { fn ->
                        FilterChip(
                            selected = fn == selectedFn,
                            onClick = { selectedFn = fn },
                            label = { Text(PersianLabels.readFunctionShort(fn)) },
                        )
                    }
                }

                if (selectedFn == ReadFunction.READ_DISCRETE_INPUTS || selectedFn == ReadFunction.READ_INPUT_REGISTERS) {
                    NoticeBox(
                        text = "توجه: در تست سخت‌افزاری BPC02 مشخص شد که توابع FC02 و FC04 پشتیبانی نمی‌شوند (پاسخ ۴ بایتی غیراستاندارد). برای BPC02 از FC01 و FC03 استفاده کنید.",
                        tone = BadgeTone.WARNING,
                    )
                }

                Text("پیش‌تنظیم‌های سریع BPC02:", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PresetChip("فشار و ولتاژ (W2..10)") {
                        selectedFn = ReadFunction.READ_HOLDING_REGISTERS; addrText = "2"; qtyText = "9"
                    }
                    PresetChip("تایمر پمپ ۱–۴ (W34)") {
                        selectedFn = ReadFunction.READ_HOLDING_REGISTERS; addrText = "34"; qtyText = "12"
                    }
                    PresetChip("وضعیت کل بیت‌ها (B0)") {
                        selectedFn = ReadFunction.READ_COILS; addrText = "0"; qtyText = "34"
                    }
                    PresetChip("خطای سیستم (W80)") {
                        selectedFn = ReadFunction.READ_HOLDING_REGISTERS; addrText = "80"; qtyText = "1"
                    }
                    PresetChip("حالت دستی (W99)") {
                        selectedFn = ReadFunction.READ_HOLDING_REGISTERS; addrText = "99"; qtyText = "1"
                    }
                    PresetChip("شناسه مدباس (W4195)") {
                        selectedFn = ReadFunction.READ_HOLDING_REGISTERS; addrText = "4195"; qtyText = "1"
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = addrText,
                        onValueChange = { addrText = it; inputError = null },
                        label = { Text("آدرس PDU (مبنای صفر)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it; inputError = null },
                        label = { Text("تعداد") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.65f),
                    )
                }

                if (inputError != null) {
                    Text(inputError!!, style = MaterialTheme.typography.bodySmall, color = p.fault)
                }

                Button(
                    onClick = {
                        val addr = addrText.trim().toIntOrNull()
                        val qty = qtyText.trim().toIntOrNull()
                        when {
                            addr == null || addr !in 0..65535 -> inputError = "آدرس PDU باید بین ۰ تا ۶۵۵۳۵ باشد"
                            qty == null || qty !in 1..selectedFn.maxQuantity ->
                                inputError = "تعداد باید بین ۱ تا ${selectedFn.maxQuantity} باشد"
                            addr + qty > 65536 -> inputError = "مجموع آدرس و تعداد از ۶۵۵۳۵ بیشتر است"
                            else -> {
                                inputError = null
                                onRunProbe(selectedFn, addr, qty)
                            }
                        }
                    },
                    enabled = isConnected,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isConnected) "ارسال درخواست خواندن" else "ابتدا به مبدل یا شبیه‌ساز متصل شوید")
                }

                uiState.probeResult?.let { res ->
                    HorizontalDivider(color = p.panelBorder)
                    if (res.error != null) {
                        NoticeBox(
                            text = "خطا در FC%02X آدرس %d (تعداد %d): %s".format(res.function.code, res.pduAddress, res.quantity, res.error),
                            tone = BadgeTone.FAULT,
                        )
                    } else if (res.values != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(p.terminalBg)
                                .border(1.dp, p.ok.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "پاسخ دریافتی (${formatTime(res.timestampMillis)})",
                                style = MaterialTheme.typography.labelMedium,
                                color = p.ok,
                            )
                            val formatted = res.values.mapIndexed { idx, v ->
                                "[%d]=%d (0x%04X)".format(res.pduAddress + idx, v, v)
                            }.joinToString("  ")
                            LtrText(
                                text = "FC%02X @ %d → %s".format(res.function.code, res.pduAddress, formatted),
                                style = MonoStyle,
                                color = p.terminalText,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TransactionLogTabContent(
    uiState: MonitoringUiState,
    onClearLog: () -> Unit,
) {
    val p = LocalScadaPalette.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ScadaPanel(
                title = "ترمینال لاگ فریم‌های مدباس (${uiState.transactions.size})",
                subtitle = "نمایش فریم خام ارسالی و دریافتی (جدیدترین در بالا)",
                icon = Icons.Outlined.Terminal,
                accentColor = p.cyan,
                trailing = {
                    TextButton(onClick = onClearLog, enabled = uiState.transactions.isNotEmpty()) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("پاک کردن")
                    }
                },
            ) {
                if (uiState.transactions.isEmpty()) {
                    Text(
                        "هنوز تراکنشی ثبت نشده است. با برقراری اتصال، تمام فریم‌های TX/RX در اینجا نمایش داده می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textSecondary,
                    )
                }
            }
        }

        items(uiState.transactions, key = { "${it.timestampMillis}-${it.function.code}-${it.pduAddress}-${it.durationMs}" }) { tx ->
            TerminalTxCard(tx)
        }
    }
}

@Composable
private fun TerminalTxCard(tx: TransactionRecord) {
    val p = LocalScadaPalette.current
    val ok = tx.error == null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.terminalBg)
            .border(1.dp, if (ok) p.panelBorder else p.fault.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LtrText(
                text = "${formatTime(tx.timestampMillis)}  •  ID ${tx.unitId}  •  FC%02X @ %d (×%d)".format(
                    tx.function.code,
                    tx.pduAddress,
                    tx.quantity,
                ),
                style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = p.textPrimary,
            )
            StatusBadge(if (ok) "${tx.durationMs} ms" else "خطا", if (ok) BadgeTone.OK else BadgeTone.FAULT)
        }
        LtrText("TX: ${tx.requestHex}", style = MonoStyle, color = p.cyan)
        tx.responseHex?.let { rx ->
            LtrText("RX: $rx", style = MonoStyle, color = p.ok)
        }
        tx.error?.let { err ->
            Text(err, style = MaterialTheme.typography.bodySmall, color = p.fault)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegisterMapTabContent(uiState: MonitoringUiState) {
    val p = LocalScadaPalette.current
    var query by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<RegisterGroup?>(null) }

    val liveMap: Map<String, Int?> = remember(uiState.snapshot, uiState.configSnapshot) {
        buildMap {
            uiState.snapshot?.readings?.forEach { put(it.definition.key, it.raw) }
            uiState.configSnapshot.readings.forEach { put(it.definition.key, it.raw) }
        }
    }

    val filtered = remember(query, selectedGroup) {
        val q = query.trim().lowercase()
        Bpc02RegisterMap.all.filter { def ->
            val groupOk = selectedGroup == null || def.group == selectedGroup
            val searchOk = q.isEmpty() ||
                PersianLabels.registerLabel(def).lowercase().contains(q) ||
                def.label.lowercase().contains(q) ||
                def.documentedAddress.toString().contains(q)
            groupOk && searchOk
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScadaPanel(
                title = "نقشه متمرکز رجیسترهای DGP BPC02",
                subtitle = "مجموع ${Bpc02RegisterMap.all.size} رجیستر (کویل FC01 و هولدینگ FC03، آفست 0)",
                icon = Icons.AutoMirrored.Outlined.ListAlt,
                accentColor = p.cyan,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("جستجو بر اساس نام یا شماره آدرس (مثلاً 2، 80، فشار، پمپ)…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = selectedGroup == null,
                        onClick = { selectedGroup = null },
                        label = { Text("همه") },
                    )
                    val groups = listOf(
                        RegisterGroup.MEASUREMENT to "اندازه‌گیری",
                        RegisterGroup.PUMP_STATUS to "پمپ‌ها",
                        RegisterGroup.RUN_TIME to "تایمر",
                        RegisterGroup.ALARM to "آلارم",
                        RegisterGroup.DIGITAL_IO to "دیجیتال I/O",
                        RegisterGroup.CONFIGURATION to "سخت‌افزار",
                        RegisterGroup.MANUAL_CONTROL to "کنترل قفل‌شده",
                    )
                    groups.forEach { (g, title) ->
                        FilterChip(
                            selected = selectedGroup == g,
                            onClick = { selectedGroup = if (selectedGroup == g) null else g },
                            label = { Text(title) },
                        )
                    }
                }
            }
        }

        items(filtered, key = { it.key }) { def ->
            val raw = liveMap[def.key]
            RegisterDirectoryCard(RegisterReading(def, raw))
        }
    }
}

@Composable
private fun RegisterDirectoryCard(reading: RegisterReading) {
    val p = LocalScadaPalette.current
    val def = reading.definition
    val locked = def.access == AccessPolicy.CONTROL_LOCKED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.panel)
            .border(1.dp, if (locked) p.warning.copy(alpha = 0.4f) else p.panelBorder, RoundedCornerShape(12.dp))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = PersianLabels.registerLabel(def),
                style = MaterialTheme.typography.titleSmall,
                color = p.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = reading.raw?.let { PersianLabels.formatRegisterValue(def, it) } ?: "—",
                style = MaterialTheme.typography.titleSmall,
                color = if (reading.raw != null) p.cyan else p.textSecondary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LtrText(
                text = "${PersianLabels.tableLabel(def.table)}  •  Dec ${def.documentedAddress} (0x%04X)".format(def.documentedAddress) +
                    (reading.raw?.let { "  •  raw=$it" } ?: ""),
                style = MonoStyle.copy(fontSize = 10.5.sp),
                color = p.textSecondary,
            )
            if (locked) {
                StatusBadge("قفل نوشتن", BadgeTone.WARNING)
            } else {
                val tone = when (def.verification) {
                    Verification.HARDWARE_VERIFIED -> BadgeTone.OK
                    Verification.DOCUMENTED -> BadgeTone.NEUTRAL
                    Verification.AMBIGUOUS -> BadgeTone.WARNING
                }
                StatusBadge(PersianLabels.verificationLabel(def.verification), tone)
            }
        }
    }
}

package com.dgp.boosterpump.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.controller.RegisterReading
import com.dgp.boosterpump.controller.register.AccessPolicy
import com.dgp.boosterpump.controller.register.RegisterDefinition
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.session.WriteResult
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

/** Pending edit state driving one of the three Persian confirmation dialogs. */
private sealed interface PendingEdit {
    data class CoilConfirm(val def: RegisterDefinition, val currentValue: Int?, val targetValue: Int) : PendingEdit
    data class WordNumberEdit(val def: RegisterDefinition, val currentValue: Int?) : PendingEdit
    data class WordEnumEdit(val def: RegisterDefinition, val currentValue: Int?) : PendingEdit
}

/**
 * Persian Industrial Control & Hardware Parameters screen.
 *
 * - Hardware configuration parameters verified on physical BPC02 hardware (FC05 on coils
 *   4096-4101 & 4106-4110; FC06 on words 4175 & 4177-4186) support safe editing with
 *   explicit Persian confirmation dialogs, range validation, and immediate read-back verification.
 * - Protected registers (4173 Control Status, 4195 ModBus Address) remain strictly read-only.
 * - Manual Mode (Addr 99, Coils 50-59, Words 50-51) remains LOCKED because physical bench testing
 *   returned Modbus Exception 0x02/0x03 (awaiting DGP technical clarification).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlAndConfigScreen(
    uiState: MonitoringUiState,
    canWriteConfig: Boolean,
    onReadHardwareConfig: () -> Unit,
    onWriteParameter: (RegisterDefinition, Int) -> Unit,
    onDismissWriteNotice: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val cfgSnap = uiState.configSnapshot
    val isConnected = uiState.connectionState is ConnectionState.Connected
    val isWriting = cfgSnap.writingKey != null
    val canEditNow = canWriteConfig && isConnected && !isWriting

    var pendingEdit by remember { mutableStateOf<PendingEdit?>(null) }

    // Render confirmation/edit dialog when user initiates a verified FC05/FC06 change
    when (val edit = pendingEdit) {
        is PendingEdit.CoilConfirm -> CoilWriteConfirmDialog(
            def = edit.def,
            currentValue = edit.currentValue,
            targetValue = edit.targetValue,
            onDismiss = { pendingEdit = null },
            onConfirm = {
                pendingEdit = null
                onWriteParameter(edit.def, edit.targetValue)
            },
        )
        is PendingEdit.WordNumberEdit -> WordNumberEditDialog(
            def = edit.def,
            currentValue = edit.currentValue,
            onDismiss = { pendingEdit = null },
            onConfirm = { newVal ->
                pendingEdit = null
                onWriteParameter(edit.def, newVal)
            },
        )
        is PendingEdit.WordEnumEdit -> WordEnumEditDialog(
            def = edit.def,
            currentValue = edit.currentValue,
            onDismiss = { pendingEdit = null },
            onConfirm = { newVal ->
                pendingEdit = null
                onWriteParameter(edit.def, newVal)
            },
        )
        null -> Unit
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Verified Hardware Configuration Read & Edit Panel
        item {
            ScadaPanel(
                title = if (canWriteConfig) {
                    "تنظیمات سخت‌افزار کنترلر (FC05 / FC06 تاییدشده)"
                } else {
                    "تنظیمات سخت‌افزار کنترلر (مشاهده فقط‌خواندنی)"
                },
                subtitle = if (canWriteConfig) {
                    "خواندن درخواستی و ویرایش ایمن ۲۲ پارامتر سخت‌افزاری با تایید دومرحله‌ای و بازخوانی فوری"
                } else {
                    "سطح دسترسی فعلی فقط مجاز به خواندن تنظیمات است — ویرایش مخصوص مدیر سیستم و نصاب است"
                },
                icon = Icons.Outlined.Tune,
                accentColor = if (canWriteConfig) p.cyan else p.gold,
                trailing = {
                    Button(
                        onClick = onReadHardwareConfig,
                        enabled = isConnected && !cfgSnap.loading && !isWriting,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        if (cfgSnap.loading) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                        }
                        Text(if (cfgSnap.readings.isEmpty()) "خواندن تنظیمات" else "بروزرسانی")
                    }
                },
            ) {
                if (!canWriteConfig) {
                    NoticeBox(
                        text = "حساب کاربری فعلی دارای دسترسی فقط‌خواندنی است. برای ویرایش پارامترهای سخت‌افزاری (FC05 / FC06) با حساب «مدیر سیستم» یا «نصاب» وارد شوید.",
                        tone = BadgeTone.WARNING,
                    )
                } else {
                    NoticeBox(
                        text = "توابع نوشتن تکی FC05 (کویل) و FC06 (ورد) روی کنترلر BPC02 تست و تایید شده‌اند. " +
                            "هر تغییر پس از تایید شما ارسال شده و بلافاصله برای اطمینان از ذخیره شدن در کنترلر مجدداً خوانده می‌شود.",
                        tone = BadgeTone.INFO,
                    )
                }

                cfgSnap.lastWrite?.let { result ->
                    WriteResultBanner(result = result, onDismiss = onDismissWriteNotice)
                }

                if (cfgSnap.timestampMillis != null) {
                    Text(
                        "آخرین همگام‌سازی تنظیمات: ساعت ${formatTime(cfgSnap.timestampMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.cyan,
                    )
                }
                if (cfgSnap.errors.isNotEmpty()) {
                    NoticeBox(
                        text = "خطا در خواندن برخی بلوک‌ها: ${cfgSnap.errors.take(3).joinToString(" | ")}",
                        tone = BadgeTone.FAULT,
                    )
                }

                if (cfgSnap.readings.isEmpty()) {
                    Text(
                        if (isConnected) {
                            "برای دریافت و بررسی فعال‌سازی پمپ‌ها، Sleep Gap، اتو سرویس و تنظیمات MFI/MFO دکمه «خواندن تنظیمات» را بزنید."
                        } else {
                            "ابتدا به کنترلر (مبدل سمین ES 487) یا حالت شبیه‌سازی متصل شوید تا امکان خواندن تنظیمات فعال شود."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textSecondary,
                    )
                } else {
                    val pumpActivations = cfgSnap.readings.filter { it.definition.documentedAddress in 4096..4101 }
                    val controlBits = cfgSnap.readings.filter { it.definition.documentedAddress in 4106..4110 }
                    val serviceWords = cfgSnap.readings.filter {
                        it.definition.documentedAddress == 4175 || it.definition.documentedAddress in 4177..4179
                    }
                    val mfxWords = cfgSnap.readings.filter { it.definition.documentedAddress in 4180..4186 }
                    val protectedRegs = cfgSnap.readings.filter { it.definition.access == AccessPolicy.READ }

                    // Section 1: Pump Activations (4096..4101, FC05)
                    if (pumpActivations.isNotEmpty()) {
                        ConfigSectionHeader(
                            title = "فعال‌سازی پمپ‌ها در مدار",
                            tag = "کویل ۴۰۹۶–۴۱۰۱ • FC05",
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(p.panelElevated)
                                .border(1.dp, p.panelBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            pumpActivations.forEachIndexed { idx, reading ->
                                if (idx > 0) HorizontalDivider(color = p.panelBorder)
                                CoilSwitchRow(
                                    reading = reading,
                                    enabled = canEditNow,
                                    isWritingThis = cfgSnap.writingKey == reading.definition.key,
                                    onRequestToggle = { target ->
                                        pendingEdit = PendingEdit.CoilConfirm(reading.definition, reading.raw, target)
                                    },
                                )
                            }
                        }
                    }

                    // Section 2: Control & Reserve Auto Coils (4106..4110, FC05)
                    if (controlBits.isNotEmpty()) {
                        ConfigSectionHeader(
                            title = "حفاظت، نوبت‌دهی و رزرو خودکار",
                            tag = "کویل ۴۱۰۶–۴۱۱۰ • FC05",
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(p.panelElevated)
                                .border(1.dp, p.panelBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            controlBits.forEachIndexed { idx, reading ->
                                if (idx > 0) HorizontalDivider(color = p.panelBorder)
                                if (reading.definition.documentedAddress == 4110) {
                                    ReserveTypeRow(
                                        reading = reading,
                                        enabled = canEditNow,
                                        isWritingThis = cfgSnap.writingKey == reading.definition.key,
                                        onSelectType = { target ->
                                            pendingEdit = PendingEdit.CoilConfirm(reading.definition, reading.raw, target)
                                        },
                                    )
                                } else {
                                    CoilSwitchRow(
                                        reading = reading,
                                        enabled = canEditNow,
                                        isWritingThis = cfgSnap.writingKey == reading.definition.key,
                                        onRequestToggle = { target ->
                                            pendingEdit = PendingEdit.CoilConfirm(reading.definition, reading.raw, target)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // Section 3: Sleep Gap & Auto-Service Parameters (4175, 4177..4179, FC06)
                    if (serviceWords.isNotEmpty()) {
                        ConfigSectionHeader(
                            title = "پارامترهای خواب و اتو سرویس",
                            tag = "ورد ۴۱۷۵، ۴۱۷۷–۴۱۷۹ • FC06",
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(p.panelElevated)
                                .border(1.dp, p.panelBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            serviceWords.forEachIndexed { idx, reading ->
                                if (idx > 0) HorizontalDivider(color = p.panelBorder)
                                WordActionRow(
                                    reading = reading,
                                    actionLabel = "ویرایش",
                                    enabled = canEditNow,
                                    isWritingThis = cfgSnap.writingKey == reading.definition.key,
                                    onClickEdit = {
                                        pendingEdit = PendingEdit.WordNumberEdit(reading.definition, reading.raw)
                                    },
                                )
                            }
                        }
                    }

                    // Section 4: Multi-Function Inputs & Outputs MFI/MFO (4180..4186, FC06)
                    if (mfxWords.isNotEmpty()) {
                        ConfigSectionHeader(
                            title = "ورودی و خروجی چندمنظوره (MFI / MFO)",
                            tag = "ورد ۴۱۸۰–۴۱۸۶ • FC06",
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(p.panelElevated)
                                .border(1.dp, p.panelBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            mfxWords.forEachIndexed { idx, reading ->
                                if (idx > 0) HorizontalDivider(color = p.panelBorder)
                                WordActionRow(
                                    reading = reading,
                                    actionLabel = "انتخاب",
                                    enabled = canEditNow,
                                    isWritingThis = cfgSnap.writingKey == reading.definition.key,
                                    onClickEdit = {
                                        pendingEdit = PendingEdit.WordEnumEdit(reading.definition, reading.raw)
                                    },
                                )
                            }
                        }
                    }

                    // Section 5: Protected Read-Only Registers (4173, 4195)
                    if (protectedRegs.isNotEmpty()) {
                        ConfigSectionHeader(
                            title = "رجیسترهای محافظت‌شده (فقط خواندنی)",
                            tag = "قفل ایمنی در اپلیکیشن",
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(p.panelElevated)
                                .border(1.dp, p.warning.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            protectedRegs.forEachIndexed { idx, reading ->
                                if (idx > 0) HorizontalDivider(color = p.panelBorder)
                                ProtectedRegisterRow(reading)
                            }
                        }
                    }
                }
            }
        }

        // 2. Locked Manual Mode Panel (reflects physical bench test findings)
        item {
            ScadaPanel(
                title = "کنترل دستی و خروجی‌های مستقیم (قفل ایمنی)",
                subtitle = "رجیستر ۹۹ (حالت دستی) • کویل ۵۰–۵۹ (رله‌های دستی) • ورد ۵۰–۵۱ (فرکانس آنالوگ)",
                icon = Icons.Outlined.AdminPanelSettings,
                accentColor = p.warning,
                borderColor = p.warning.copy(alpha = 0.55f),
                trailing = { StatusBadge("قفل (در انتظار DGP)", BadgeTone.WARNING) },
            ) {
                NoticeBox(
                    text = "در تست سخت‌افزاری کنترلر BPC02، فرمان ورود به حالت دستی (FC06 آدرس ۹۹ = 1234) پاسخ Echo داد، " +
                        "اما نوشتن روی خروجی دستی (کویل ۵۶ و ورد ۵۰) خطای Modbus 0x02 (آدرس غیرمجاز) و خروج با مقدار 0 خطای 0x03 برگرداند. " +
                        "همچنین توابع FC15 و FC16 پشتیبانی نمی‌شوند. تا زمان تایید پروتکل حالت دستی توسط پشتیبانی DGP (۰۲۱-۷۷۷۹۷۰۰۶)، این بخش برای ایمنی کامل قفل می‌ماند.",
                    tone = BadgeTone.WARNING,
                )

                // Disabled Manual Mode preview panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.55f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.panelElevated)
                        .border(1.dp, p.panelBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = p.warning, modifier = Modifier.size(15.dp))
                                Text("ورود به حالت دستی (Manual Mode)", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
                            }
                            Text(
                                "آدرس ۹۹ — در تست واقعی خروجی‌ها خطای Exception 0x02 / 0x03 برگرداندند",
                                style = MaterialTheme.typography.bodySmall,
                                color = p.textSecondary,
                            )
                        }
                        Switch(checked = false, onCheckedChange = null, enabled = false)
                    }

                    HorizontalDivider(color = p.panelBorder)

                    Text("خروجی‌های دستی رله (کویل ۵۰ تا ۵۹ — قفل‌شده)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (i in 0..9) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(p.panel)
                                    .border(1.dp, p.panelBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = p.standby, modifier = Modifier.size(12.dp))
                                LtrText("OUT $i", style = MonoStyle.copy(fontSize = 11.sp), color = p.textSecondary)
                            }
                        }
                    }

                    HorizontalDivider(color = p.panelBorder)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سرعت آنالوگ درایو ۱ (ورد ۵۰ — ۰ تا ۵۰.۰ هرتز)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                            LtrText("0.0 Hz", style = MonoStyle, color = p.textSecondary)
                        }
                        Slider(value = 0f, onValueChange = {}, enabled = false)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سرعت آنالوگ درایو ۲ (ورد ۵۱ — ۰ تا ۵۰.۰ هرتز)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                            LtrText("0.0 Hz", style = MonoStyle, color = p.textSecondary)
                        }
                        Slider(value = 0f, onValueChange = {}, enabled = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigSectionHeader(title: String, tag: String) {
    val p = LocalScadaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = p.textPrimary,
        )
        LtrText(
            text = tag,
            style = MonoStyle.copy(fontSize = 10.5.sp),
            color = p.cyan,
        )
    }
}

@Composable
private fun WriteResultBanner(result: WriteResult, onDismiss: () -> Unit) {
    val p = LocalScadaPalette.current
    val fg = if (result.success) p.ok else p.fault
    val bg = if (result.success) p.okBg else p.faultBg
    val def = result.definition
    val label = PersianLabels.registerLabel(def)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, fg.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
        color = bg,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (result.success) Icons.Outlined.CheckCircleOutline else Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (result.success) {
                        "تغییر «$label» اعمال و تایید شد (${formatTime(result.timestampMillis)})"
                    } else {
                        "خطا در ثبت «$label» (آدرس ${def.documentedAddress})"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                )
                Text(
                    text = if (result.success) {
                        val confirmed = result.readBackValue?.let { PersianLabels.formatRegisterValue(def, it) } ?: "—"
                        "آدرس ${def.documentedAddress}  •  مقدار تاییدشده با بازخوانی: $confirmed"
                    } else {
                        result.error ?: "خطای نامشخص در ارتباط مدباس"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = fg,
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "بستن", tint = fg, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CoilSwitchRow(
    reading: RegisterReading,
    enabled: Boolean,
    isWritingThis: Boolean,
    onRequestToggle: (Int) -> Unit,
) {
    val p = LocalScadaPalette.current
    val def = reading.definition
    val isOn = reading.raw == 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = PersianLabels.registerLabel(def),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = p.textPrimary,
            )
            LtrText(
                text = "FC05 • Coil ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                style = MonoStyle.copy(fontSize = 10.5.sp),
                color = p.textSecondary,
            )
        }
        Spacer(Modifier.width(10.dp))
        if (isWritingThis) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = p.cyan)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = reading.raw?.let { PersianLabels.formatRegisterValue(def, it) } ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isOn) p.ok else p.textSecondary,
                )
                Switch(
                    checked = isOn,
                    onCheckedChange = { checked -> onRequestToggle(if (checked) 1 else 0) },
                    enabled = enabled && reading.raw != null,
                )
            }
        }
    }
}

@Composable
private fun ReserveTypeRow(
    reading: RegisterReading,
    enabled: Boolean,
    isWritingThis: Boolean,
    onSelectType: (Int) -> Unit,
) {
    val p = LocalScadaPalette.current
    val def = reading.definition

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = PersianLabels.registerLabel(def),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = p.textPrimary,
            )
            LtrText(
                text = "FC05 • Coil ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                style = MonoStyle.copy(fontSize = 10.5.sp),
                color = p.textSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (isWritingThis) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = p.cyan)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = reading.raw == 0,
                    onClick = { if (reading.raw != 0) onSelectType(0) },
                    enabled = enabled && reading.raw != null,
                    label = { Text("باز (NO)", style = MaterialTheme.typography.labelSmall) },
                )
                FilterChip(
                    selected = reading.raw == 1,
                    onClick = { if (reading.raw != 1) onSelectType(1) },
                    enabled = enabled && reading.raw != null,
                    label = { Text("بسته (NC)", style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

@Composable
private fun WordActionRow(
    reading: RegisterReading,
    actionLabel: String,
    enabled: Boolean,
    isWritingThis: Boolean,
    onClickEdit: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val def = reading.definition

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = PersianLabels.registerLabel(def),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = p.textPrimary,
            )
            LtrText(
                text = "FC06 • Word ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                style = MonoStyle.copy(fontSize = 10.5.sp),
                color = p.textSecondary,
            )
            Text(
                text = reading.raw?.let { PersianLabels.formatRegisterValue(def, it) } ?: "—",
                style = MaterialTheme.typography.titleSmall,
                color = if (reading.raw != null) p.cyan else p.textSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (isWritingThis) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = p.cyan)
        } else {
            OutlinedButton(
                onClick = onClickEdit,
                enabled = enabled && reading.raw != null,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                border = BorderStroke(1.dp, p.cyan.copy(alpha = 0.55f)),
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ProtectedRegisterRow(reading: RegisterReading) {
    val p = LocalScadaPalette.current
    val def = reading.definition
    val reason = when (def.documentedAddress) {
        4173 -> "غیرفعال‌سازی باعث توقف کامل سیستم (System Stopped) می‌شود"
        4195 -> "تغییر شناسه Slave از راه دور ارتباط مدباس را فوراً قطع می‌کند"
        else -> "تغییر این رجیستر فقط از طریق پنل دستگاه مجاز است"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = p.warning, modifier = Modifier.size(14.dp))
                Text(
                    text = PersianLabels.registerLabel(def),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = p.textPrimary,
                )
            }
            LtrText(
                text = "Read-Only • Word ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                style = MonoStyle.copy(fontSize = 10.5.sp),
                color = p.textSecondary,
            )
            Text(reason, style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = reading.raw?.let { PersianLabels.formatRegisterValue(def, it) } ?: "—",
                style = MaterialTheme.typography.titleSmall,
                color = p.textPrimary,
            )
            StatusBadge("محافظت‌شده", BadgeTone.WARNING)
        }
    }
}

@Composable
private fun CoilWriteConfirmDialog(
    def: RegisterDefinition,
    currentValue: Int?,
    targetValue: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val currentText = currentValue?.let { PersianLabels.formatRegisterValue(def, it) } ?: "—"
    val targetText = PersianLabels.formatRegisterValue(def, targetValue)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Tune, contentDescription = null, tint = p.warning) },
        title = { Text("تایید تغییر تنظیم سخت‌افزاری", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    PersianLabels.registerLabel(def),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = p.textPrimary,
                )
                LtrText(
                    text = "FC05 (Write Single Coil)  •  Addr ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                    style = MonoStyle.copy(fontSize = 11.sp),
                    color = p.cyan,
                )
                Surface(
                    color = p.panelElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, p.panelBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("مقدار فعلی: $currentText", style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                        Text("مقدار جدید: $targetText", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = p.warning)
                    }
                }
                Text(
                    "آیا از اعمال این تغییر روی کنترلر بوستر پمپ اطمینان دارید؟ پس از ارسال، مقدار جدید بلافاصله بازخوانی و تایید می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textSecondary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = p.warning, contentColor = p.simBannerFg),
            ) {
                Text("تایید و نوشتن (FC05)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}

@Composable
private fun WordNumberEditDialog(
    def: RegisterDefinition,
    currentValue: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val p = LocalScadaPalette.current
    val min = def.minValue ?: 0
    val max = def.maxValue ?: 65535
    var text by remember(def.key, currentValue) { mutableStateOf(currentValue?.toString().orEmpty()) }
    val parsed = text.trim().toIntOrNull()
    val inRange = parsed != null && parsed in min..max
    val changed = parsed != null && parsed != currentValue

    val rangeSummary = buildString {
        append("محدوده مجاز خام: $min تا $max")
        if (def.scale != null && def.scale != 1.0) {
            append("  (${PersianLabels.formatRegisterValue(def, min)} تا ${PersianLabels.formatRegisterValue(def, max)})")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = p.cyan) },
        title = { Text("ویرایش پارامتر سخت‌افزاری", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    PersianLabels.registerLabel(def),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = p.textPrimary,
                )
                LtrText(
                    text = "FC06 (Write Single Register)  •  Addr ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                    style = MonoStyle.copy(fontSize = 11.sp),
                    color = p.cyan,
                )
                Text(rangeSummary, style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                currentValue?.let { cur ->
                    Text(
                        "مقدار فعلی: ${PersianLabels.formatRegisterValue(def, cur)} (خام: $cur)",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textSecondary,
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { input -> text = input.filter { it.isDigit() } },
                    label = { Text("مقدار خام جدید ($min..$max)") },
                    singleLine = true,
                    isError = text.isNotEmpty() && !inRange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        when {
                            text.isEmpty() -> Text("عدد صحیح بین $min تا $max وارد کنید")
                            !inRange -> Text("مقدار باید بین $min تا $max باشد", color = p.fault)
                            else -> Text(
                                "مقدار مهندسی جدید: ${PersianLabels.formatRegisterValue(def, parsed!!)}",
                                color = p.ok,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "توجه: تغییر نادرست پارامترهای سخت‌افزاری می‌تواند عملکرد بوستر پمپ را تحت تاثیر قرار دهد. پس از ارسال، مقدار مجدداً خوانده و تایید می‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.warning,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (inRange) parsed?.let(onConfirm) },
                enabled = inRange && changed,
            ) {
                Text("تایید و نوشتن (FC06)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}

@Composable
private fun WordEnumEditDialog(
    def: RegisterDefinition,
    currentValue: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val p = LocalScadaPalette.current
    val options = remember(def.key) { def.enumLabels.keys.sorted() }
    var selected by remember(def.key, currentValue) {
        mutableIntStateOf(currentValue ?: options.firstOrNull() ?: 0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Tune, contentDescription = null, tint = p.cyan) },
        title = { Text("انتخاب عملکرد ${PersianLabels.registerLabel(def)}", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LtrText(
                    text = "FC06 (Write Single Register)  •  Addr ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                    style = MonoStyle.copy(fontSize = 11.sp),
                    color = p.cyan,
                )
                Spacer(Modifier.height(4.dp))
                options.forEach { code ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = code }
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        RadioButton(selected = selected == code, onClick = { selected = code })
                        Text(
                            text = "$code — ${PersianLabels.enumOptionLabel(def, code)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected == code) p.cyan else p.textPrimary,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "پس از تایید، فرمان FC06 ارسال شده و مقدار ثبت‌شده بلافاصله از کنترلر بازخوانی می‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.warning,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected) },
                enabled = selected != currentValue,
            ) {
                Text("تایید و نوشتن (FC06)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}

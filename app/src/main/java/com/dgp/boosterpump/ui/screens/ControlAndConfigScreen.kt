package com.dgp.boosterpump.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
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
import com.dgp.boosterpump.ui.components.DgpSegmentedControl
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
 * Flagship Persian Hardware Parameters & Safe Control screen.
 * Preserves all FC05/FC06 safety guardrails, 2-step confirmation, read-back confirmation,
 * protected read-only registers (4173, 4195), and locked Manual Mode.
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Hardware Configuration Read & Verified Edit Panel
        item {
            ScadaPanel(
                title = if (canWriteConfig) {
                    "تنظیمات سخت‌افزار کنترلر"
                } else {
                    "تنظیمات سخت‌افزار (فقط خواندنی)"
                },
                subtitle = if (canWriteConfig) {
                    "۲۲ پارامتر تاییدشده FC05 / FC06 با تایید دومرحله‌ای و بازخوانی فوری"
                } else {
                    "سطح دسترسی فعلی فقط مجاز به مشاهده تنظیمات است"
                },
                icon = Icons.Outlined.Tune,
                accentColor = p.gold,
                trailing = {
                    val canRefresh = isConnected && !cfgSnap.loading && !isWriting
                    Surface(
                        onClick = onReadHardwareConfig,
                        enabled = canRefresh,
                        color = if (canRefresh) p.goldSoft else p.panelElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.9.dp, if (canRefresh) p.gold.copy(alpha = 0.5f) else p.panelBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            if (cfgSnap.loading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = p.gold)
                            } else {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    tint = if (canRefresh) p.gold else p.textMuted,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                            Text(
                                text = if (cfgSnap.readings.isEmpty()) "خواندن" else "بروزرسانی",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (canRefresh) p.gold else p.textMuted,
                            )
                        }
                    }
                },
            ) {
                if (!canWriteConfig) {
                    NoticeBox(
                        text = "حساب کاربری فعلی فقط‌خواندنی است. برای ویرایش پارامترهای سخت‌افزاری (FC05 / FC06) با حساب «مدیر سیستم» یا «نصاب» وارد شوید.",
                        tone = BadgeTone.WARNING,
                    )
                } else {
                    NoticeBox(
                        text = "توابع نوشتن تکی FC05 (کویل) و FC06 (ورد) روی سخت‌افزار BPC02 تایید شده‌اند. هر تغییر پس از تایید شما ارسال و بلافاصله بازخوانی می‌شود.",
                        tone = BadgeTone.GOLD,
                    )
                }

                AnimatedVisibility(
                    visible = cfgSnap.lastWrite != null,
                    enter = fadeIn() + slideInVertically { -it / 3 },
                    exit = fadeOut(),
                ) {
                    cfgSnap.lastWrite?.let { result ->
                        WriteResultBanner(result = result, onDismiss = onDismissWriteNotice)
                    }
                }

                if (cfgSnap.timestampMillis != null) {
                    Text(
                        "آخرین همگام‌سازی: ساعت ${formatTime(cfgSnap.timestampMillis)}",
                        style = MaterialTheme.typography.labelSmall,
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(p.panelElevated)
                            .border(0.8.dp, p.panelBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            if (isConnected) {
                                "برای دریافت و بررسی فعال‌سازی پمپ‌ها، Sleep Gap، اتو سرویس و تنظیمات MFI/MFO دکمه «خواندن» را بزنید."
                            } else {
                                "ابتدا به کنترلر (مبدل سمین ES 487) یا حالت شبیه‌سازی متصل شوید تا امکان خواندن تنظیمات فعال شود."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = p.textSecondary,
                        )
                    }
                } else {
                    val pumpActivations = cfgSnap.readings.filter { it.definition.documentedAddress in 4096..4101 }
                    val controlBits = cfgSnap.readings.filter { it.definition.documentedAddress in 4106..4110 }
                    val serviceWords = cfgSnap.readings.filter {
                        it.definition.documentedAddress == 4175 || it.definition.documentedAddress in 4177..4179
                    }
                    val mfxWords = cfgSnap.readings.filter { it.definition.documentedAddress in 4180..4186 }
                    val protectedRegs = cfgSnap.readings.filter { it.definition.access == AccessPolicy.READ }

                    if (pumpActivations.isNotEmpty()) {
                        ConfigSectionHeader("فعال‌سازی پمپ‌ها در مدار", "کویل ۴۰۹۶–۴۱۰۱ • FC05")
                        ConfigGroupBox {
                            pumpActivations.forEachIndexed { idx, reading ->
                                if (idx > 0) HorizontalDivider(color = p.hairline)
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

                    if (controlBits.isNotEmpty()) {
                        ConfigSectionHeader("حفاظت، نوبت‌دهی و رزرو خودکار", "کویل ۴۱۰۶–۴۱۱۰ • FC05")
                        ConfigGroupBox {
                            controlBits.forEachIndexed { idx, reading ->
                                if (idx > 0) HorizontalDivider(color = p.hairline)
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

                    if (serviceWords.isNotEmpty()) {
                        ConfigSectionHeader("پارامترهای خواب و اتو سرویس", "ورد ۴۱۷۵، ۴۱۷۷–۴۱۷۹ • FC06")
                        ConfigGroupBox {
                            serviceWords.forEachIndexed { idx, reading ->
                                if (idx > 0) HorizontalDivider(color = p.hairline)
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

                    if (mfxWords.isNotEmpty()) {
                        ConfigSectionHeader("ورودی و خروجی چندمنظوره (MFI / MFO)", "ورد ۴۱۸۰–۴۱۸۶ • FC06")
                        ConfigGroupBox {
                            mfxWords.forEachIndexed { idx, reading ->
                                if (idx > 0) HorizontalDivider(color = p.hairline)
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

                    if (protectedRegs.isNotEmpty()) {
                        ConfigSectionHeader("رجیسترهای محافظت‌شده (فقط خواندنی)", "قفل ایمنی اپلیکیشن")
                        ConfigGroupBox(borderColor = p.warning.copy(alpha = 0.28f)) {
                            protectedRegs.forEachIndexed { idx, reading ->
                                if (idx > 0) HorizontalDivider(color = p.hairline)
                                ProtectedRegisterRow(reading)
                            }
                        }
                    }
                }
            }
        }

        // 2. Locked Manual Mode Panel
        item {
            ScadaPanel(
                title = "کنترل دستی و خروجی‌های مستقیم",
                subtitle = "رجیستر ۹۹ • کویل ۵۰–۵۹ (رله‌های دستی) • ورد ۵۰–۵۱ (آنالوگ)",
                icon = Icons.Outlined.AdminPanelSettings,
                accentColor = p.warning,
                borderColor = p.warning.copy(alpha = 0.36f),
                trailing = { StatusBadge("قفل ایمنی", BadgeTone.WARNING) },
            ) {
                NoticeBox(
                    text = "در تست سخت‌افزاری BPC02، فرمان آدرس ۹۹ = 1234 پاسخ Echo داد اما نوشتن روی کویل ۵۶ و ورد ۵۰ خطای Modbus 0x02 و خروج با 0 خطای 0x03 برگرداند (FC15/FC16 نیز پشتیبانی نمی‌شوند). تا زمان تایید پروتکل توسط DGP (۰۲۱-۷۷۷۹۷۰۰۶)، این بخش قفل می‌ماند.",
                    tone = BadgeTone.WARNING,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.52f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(p.panelElevated)
                        .border(0.8.dp, p.panelBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
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
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = p.warning, modifier = Modifier.size(14.dp))
                                Text("ورود به حالت دستی (Manual Mode)", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
                            }
                            Text(
                                "آدرس ۹۹ — قفل‌شده به دلیل خطای سخت‌افزاری 0x02 / 0x03",
                                style = MaterialTheme.typography.bodySmall,
                                color = p.textSecondary,
                            )
                        }
                        Switch(checked = false, onCheckedChange = null, enabled = false)
                    }

                    HorizontalDivider(color = p.hairline)

                    Text("خروجی‌های دستی رله (کویل ۵۰ تا ۵۹)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (i in 0..9) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(p.panel)
                                    .border(0.8.dp, p.panelBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = p.standby, modifier = Modifier.size(11.dp))
                                LtrText("OUT $i", style = MonoStyle.copy(fontSize = 10.5.sp), color = p.textSecondary)
                            }
                        }
                    }

                    HorizontalDivider(color = p.hairline)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سرعت آنالوگ درایو ۱ (ورد ۵۰)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                            LtrText("0.0 Hz", style = MonoStyle, color = p.textSecondary)
                        }
                        Slider(value = 0f, onValueChange = {}, enabled = false)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سرعت آنالوگ درایو ۲ (ورد ۵۱)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
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
private fun ConfigGroupBox(
    borderColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val p = LocalScadaPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.panelElevated)
            .border(0.8.dp, borderColor ?: p.panelBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        content()
    }
}

@Composable
private fun ConfigSectionHeader(title: String, tag: String) {
    val p = LocalScadaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = p.textPrimary,
        )
        LtrText(
            text = tag,
            style = MonoStyle.copy(fontSize = 10.sp),
            color = p.gold,
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
            .border(0.9.dp, fg.copy(alpha = 0.42f), RoundedCornerShape(12.dp)),
        color = bg,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                imageVector = if (result.success) Icons.Outlined.CheckCircleOutline else Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(19.dp),
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
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = PersianLabels.registerLabel(def),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = p.textPrimary,
            )
            LtrText(
                text = "FC05 • Coil ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                style = MonoStyle.copy(fontSize = 10.sp),
                color = p.textMuted,
            )
        }
        Spacer(Modifier.width(10.dp))
        if (isWritingThis) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = p.gold)
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = PersianLabels.registerLabel(def),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = p.textPrimary,
                )
                LtrText(
                    text = "FC05 • Coil ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                    style = MonoStyle.copy(fontSize = 10.sp),
                    color = p.textMuted,
                )
            }
            if (isWritingThis) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = p.gold)
            }
        }
        DgpSegmentedControl(
            items = listOf(0, 1),
            selected = reading.raw ?: 0,
            label = { if (it == 0) "باز (NO)" else "بسته (NC)" },
            onSelect = { target -> if (reading.raw != target) onSelectType(target) },
            enabled = enabled && reading.raw != null && !isWritingThis,
            height = 36.dp,
        )
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
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = PersianLabels.registerLabel(def),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = p.textPrimary,
            )
            LtrText(
                text = "FC06 • Word ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                style = MonoStyle.copy(fontSize = 10.sp),
                color = p.textMuted,
            )
            Text(
                text = reading.raw?.let { PersianLabels.formatRegisterValue(def, it) } ?: "—",
                style = MaterialTheme.typography.titleSmall,
                color = if (reading.raw != null) p.cyan else p.textSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (isWritingThis) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = p.gold)
        } else {
            val active = enabled && reading.raw != null
            Surface(
                onClick = onClickEdit,
                enabled = active,
                color = if (active) p.goldSoft else p.panel,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.8.dp, if (active) p.gold.copy(alpha = 0.45f) else p.panelBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = if (active) p.gold else p.textMuted,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) p.gold else p.textMuted,
                    )
                }
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
            .padding(vertical = 9.dp),
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
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = p.textPrimary,
                )
            }
            LtrText(
                text = "Read-Only • Word ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                style = MonoStyle.copy(fontSize = 10.sp),
                color = p.textMuted,
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
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.Tune, contentDescription = null, tint = p.gold) },
        title = { Text("تایید تغییر تنظیم سخت‌افزاری", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    PersianLabels.registerLabel(def),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = p.textPrimary,
                )
                LtrText(
                    text = "FC05 (Write Single Coil)  •  Addr ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                    style = MonoStyle.copy(fontSize = 11.sp),
                    color = p.cyan,
                )
                Surface(
                    color = p.panelElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, p.panelBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("مقدار فعلی: $currentText", style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                        Text("مقدار جدید: $targetText", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = p.gold)
                    }
                }
                Text(
                    "پس از ارسال فرمان FC05، مقدار جدید بلافاصله مجدداً خوانده و راستی‌آزمایی می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textSecondary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.gold, contentColor = Color(0xFF0E0A03)),
            ) {
                Text("تایید و نوشتن (FC05)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = p.textSecondary) }
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
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = p.gold) },
        title = { Text("ویرایش پارامتر سخت‌افزاری", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    PersianLabels.registerLabel(def),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
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
                    shape = RoundedCornerShape(12.dp),
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
                    "پس از ارسال فرمان FC06، مقدار ثبت‌شده بلافاصله از کنترلر بازخوانی و تایید می‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.warning,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (inRange) parsed?.let(onConfirm) },
                enabled = inRange && changed,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.gold, contentColor = Color(0xFF0E0A03)),
            ) {
                Text("تایید و نوشتن (FC06)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = p.textSecondary) }
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
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.Tune, contentDescription = null, tint = p.gold) },
        title = { Text("انتخاب عملکرد ${PersianLabels.registerLabel(def)}", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LtrText(
                    text = "FC06 (Write Single Register)  •  Addr ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                    style = MonoStyle.copy(fontSize = 11.sp),
                    color = p.cyan,
                )
                Spacer(Modifier.height(4.dp))
                options.forEach { code ->
                    val isSel = selected == code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) p.goldSoft else Color.Transparent)
                            .clickable { selected = code }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        RadioButton(selected = isSel, onClick = { selected = code })
                        Text(
                            text = "$code — ${PersianLabels.enumOptionLabel(def, code)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSel) p.gold else p.textPrimary,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "پس از تایید، فرمان FC06 ارسال شده و مقدار ثبت‌شده بلافاصله بازخوانی می‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.warning,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected) },
                enabled = selected != currentValue,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.gold, contentColor = Color(0xFF0E0A03)),
            ) {
                Text("تایید و نوشتن (FC06)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = p.textSecondary) }
        },
    )
}

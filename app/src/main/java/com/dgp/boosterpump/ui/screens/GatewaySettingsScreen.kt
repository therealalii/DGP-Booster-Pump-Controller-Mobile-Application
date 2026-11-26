package com.dgp.boosterpump.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.dgp.boosterpump.auth.OperatorPermissions
import com.dgp.boosterpump.gateway.ConfigValidation
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.ui.PersianLabels
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.DgpGoldButton
import com.dgp.boosterpump.ui.components.DgpOutlinedButton
import com.dgp.boosterpump.ui.components.DgpSegmentedControl
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.ScadaPanel
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.viewmodel.GatewayFormState
import com.dgp.boosterpump.viewmodel.GatewaySettingsViewModel

/**
 * Simplified, user-friendly Converter & Connection Settings screen designed for ordinary users:
 * exposes only Connection Mode (Live / Demo), Converter IP, Port, Device Number, and a collapsed
 * timing section while keeping verified Modbus protocol settings safely configured under the hood.
 */
@Composable
fun GatewaySettingsScreen(
    form: GatewayFormState,
    viewModel: GatewaySettingsViewModel,
    canEditGateway: Boolean,
    canOpenDiagnostics: Boolean,
    canManageOperatorPermissions: Boolean = false,
    operatorPermissions: OperatorPermissions = OperatorPermissions.READ_ONLY_DEFAULT,
    onSavedAndConnected: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenOperatorPermissions: () -> Unit = {},
) {
    val p = LocalScadaPalette.current
    var showAdvancedTiming by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (!canEditGateway) {
            item {
                NoticeBox(
                    text = "تنظیمات اتصال مبدل شبکه مخصوص «مدیر سیستم» است.",
                    tone = BadgeTone.WARNING,
                )
            }
        }

        // 1. Simple Connection Mode (Real Device vs Demo Mode)
        item {
            ScadaPanel(
                title = "حالت اتصال",
                subtitle = "انتخاب بین اتصال واقعی به کنترلر یا حالت نمایشی",
                icon = Icons.Outlined.Wifi,
                accentColor = p.gold,
            ) {
                DgpSegmentedControl(
                    items = DataSourceMode.entries,
                    selected = form.mode,
                    label = { mode ->
                        when (mode) {
                            DataSourceMode.LIVE -> "دستگاه واقعی"
                            DataSourceMode.SIMULATION -> "حالت نمایشی"
                        }
                    },
                    onSelect = viewModel::updateMode,
                    enabled = canEditGateway,
                )

                if (form.mode == DataSourceMode.SIMULATION) {
                    NoticeBox(
                        text = "در حالت نمایشی می‌توانید تمام امکانات برنامه را بدون نیاز به اتصال سخت‌افزاری آزمایش کنید.",
                        tone = BadgeTone.INFO,
                    )
                }
            }
        }

        // 2. Clean Converter Network Details (IP, Port, Device ID only)
        if (form.mode == DataSourceMode.LIVE) {
            item {
                ScadaPanel(
                    title = "مشخصات مبدل شبکه",
                    subtitle = "اطلاعات مورد نیاز برای ارتباط گوشی با کنترلر بوستر پمپ",
                    icon = Icons.Outlined.Router,
                    accentColor = p.cyan,
                ) {
                    // Clean, jargon-free connection path strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.panelElevated)
                            .border(0.8.dp, p.panelBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("اپلیکیشن DGP", style = MaterialTheme.typography.labelSmall, color = p.gold, fontWeight = FontWeight.Bold)
                        Text("←  وای‌فای  →", style = MaterialTheme.typography.labelSmall, color = p.cyan)
                        Text("مبدل شبکه", style = MaterialTheme.typography.labelSmall, color = p.textPrimary, fontWeight = FontWeight.Bold)
                        Text("←  کابل  →", style = MaterialTheme.typography.labelSmall, color = p.ok)
                        Text("کنترلر", style = MaterialTheme.typography.labelSmall, color = p.gold, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = form.host,
                        onValueChange = viewModel::updateHost,
                        readOnly = !canEditGateway,
                        label = { Text("IP Address") },
                        placeholder = { Text("192.168.1.74") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = ConfigValidation.Field.HOST in form.errors,
                        supportingText = form.errors[ConfigValidation.Field.HOST]?.let { err ->
                            { Text(PersianLabels.validationError(ConfigValidation.Field.HOST, err)) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = form.portText,
                            onValueChange = viewModel::updatePort,
                            readOnly = !canEditGateway,
                            label = { Text("Port") },
                            supportingText = {
                                Text(
                                    form.errors[ConfigValidation.Field.PORT]?.let {
                                        PersianLabels.validationError(ConfigValidation.Field.PORT, it)
                                    } ?: "Default: 4001 / 502",
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            isError = ConfigValidation.Field.PORT in form.errors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = form.unitIdText,
                            onValueChange = viewModel::updateUnitId,
                            readOnly = !canEditGateway,
                            label = { Text("ModBus Address") },
                            supportingText = {
                                Text(
                                    form.errors[ConfigValidation.Field.UNIT_ID]?.let {
                                        PersianLabels.validationError(ConfigValidation.Field.UNIT_ID, it)
                                    } ?: "Default: 1",
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            isError = ConfigValidation.Field.UNIT_ID in form.errors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    NoticeBox(
                        text = "برای اتصال، گوشی را به شبکه وای‌فای متصل به مبدل وصل کرده و آدرس IP و Port مبدل را وارد کنید.",
                        tone = BadgeTone.INFO,
                    )
                }
            }
        }

        // 3. Collapsed Optional Timing Section (keeps screen clean for everyday users)
        item {
            Surface(
                onClick = { showAdvancedTiming = !showAdvancedTiming },
                color = p.panel,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.9.dp, p.panelBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Outlined.Timer, contentDescription = null, tint = p.cyan, modifier = Modifier.size(19.dp))
                            Column {
                                Text(
                                    "Timing Settings",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = p.textPrimary,
                                )
                                Text(
                                    "بازه بروزرسانی اطلاعات و زمان انتظار پاسخ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = p.textSecondary,
                                )
                            }
                        }
                        Icon(
                            imageVector = if (showAdvancedTiming) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = p.gold,
                        )
                    }

                    AnimatedVisibility(visible = showAdvancedTiming) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = form.pollIntervalText,
                                    onValueChange = viewModel::updatePollInterval,
                                    readOnly = !canEditGateway,
                                    label = { Text("Poll Interval (ms)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    isError = ConfigValidation.Field.POLL_INTERVAL in form.errors,
                                    supportingText = form.errors[ConfigValidation.Field.POLL_INTERVAL]?.let { e ->
                                        { Text(PersianLabels.validationError(ConfigValidation.Field.POLL_INTERVAL, e)) }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                )
                                if (form.mode == DataSourceMode.LIVE) {
                                    OutlinedTextField(
                                        value = form.responseTimeoutText,
                                        onValueChange = viewModel::updateResponseTimeout,
                                        readOnly = !canEditGateway,
                                        label = { Text("Response Timeout (ms)") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        isError = ConfigValidation.Field.RESPONSE_TIMEOUT in form.errors,
                                        supportingText = form.errors[ConfigValidation.Field.RESPONSE_TIMEOUT]?.let { e ->
                                            { Text(PersianLabels.validationError(ConfigValidation.Field.RESPONSE_TIMEOUT, e)) }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Save / Save & Connect Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                form.statusMessage?.let { msg ->
                    val faMsg = if (msg.contains("Connecting")) "تنظیمات ذخیره شد — در حال اتصال…" else "تنظیمات با موفقیت ذخیره شد."
                    NoticeBox(text = faMsg, tone = BadgeTone.OK)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DgpGoldButton(
                        text = "ذخیره و اتصال",
                        icon = Icons.Outlined.Check,
                        enabled = canEditGateway,
                        onClick = { viewModel.save(andConnect = true, onConnected = onSavedAndConnected) },
                        height = 48.dp,
                        modifier = Modifier.weight(1.25f),
                    )
                    DgpOutlinedButton(
                        text = "ذخیره",
                        icon = Icons.Outlined.Save,
                        enabled = canEditGateway,
                        onClick = { viewModel.save(andConnect = false) },
                        height = 48.dp,
                        modifier = Modifier.weight(0.75f),
                    )
                }
            }
        }

        // 5. Admin-Only Operator Access Control («دسترسی های اپراتور»)
        if (canManageOperatorPermissions) {
            item {
                OperatorPermissionsAdminCard(
                    permissions = operatorPermissions,
                    onClick = onOpenOperatorPermissions,
                )
            }
        }

        // 6. Technical Diagnostics Entry (Admin or Operator granted diagnostics permission)
        if (canOpenDiagnostics) {
            item {
                Surface(
                    onClick = onOpenDiagnostics,
                    color = p.panel,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.8.dp, p.panelBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Build, contentDescription = null, tint = p.gold, modifier = Modifier.size(18.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    "ابزار عیب‌یابی فنی ارتباط",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = p.textPrimary,
                                )
                                Text(
                                    if (canManageOperatorPermissions) {
                                        "مخصوص مدیر سیستم و پشتیبان فنی"
                                    } else {
                                        "تست خواندن Modbus، لاگ فریم‌ها و نقشه رجیستر"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = p.textSecondary,
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = p.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

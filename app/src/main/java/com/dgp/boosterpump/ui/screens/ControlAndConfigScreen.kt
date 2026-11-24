package com.dgp.boosterpump.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WaterDrop
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.auth.UserRole
import com.dgp.boosterpump.controller.RegisterReading
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.RegisterDefinition
import com.dgp.boosterpump.controller.register.VendorTable
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.session.WriteResult
import com.dgp.boosterpump.ui.PersianLabels
import com.dgp.boosterpump.ui.components.AnimatedImpellerIcon
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.DgpGoldButton
import com.dgp.boosterpump.ui.components.DgpPillChip
import com.dgp.boosterpump.ui.components.DgpSegmentedControl
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.PumpPowerToggle
import com.dgp.boosterpump.ui.components.ScadaPanel
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.ui.components.formatTime
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.viewmodel.MonitoringUiState
import java.util.Locale
import kotlin.math.roundToInt

private val DialogGoldBg = Color(0xFFD09629)
private val DialogGoldInk = Color(0xFF0A0702)

/** Pending edit state driving one of the Persian confirmation dialogs. */
private sealed interface PendingEdit {
    data class SwitchConfirm(val def: RegisterDefinition, val currentValue: Int?, val targetValue: Int) : PendingEdit
    data class NumberEdit(val def: RegisterDefinition, val currentValue: Int?) : PendingEdit
    data class EnumEdit(val def: RegisterDefinition, val currentValue: Int?) : PendingEdit
    data class ControlModeConfirm(val currentValue: Int?, val targetValue: Int) : PendingEdit
    data object ResetErrorsConfirm : PendingEdit
}

/** One page inside a physical DGP BPC02 hardware menu category. */
private data class ControllerMenuPage(
    val number: Int,
    val title: String,
    val registerKeys: List<String>,
)

/**
 * Physical DGP BPC02 controller menu categories mirroring the hardware LCD menu tree:
 *  1. Control Phase (2 pages)
 *  2. Floater (1 page)
 *  3. Sensor (2 pages)
 *  4. Control (13 pages)
 *  5. MFx — Multi-Function I/O & Analog Outputs (3 pages)
 *  6. Device & Network Info (1 page)
 */
private enum class ControllerCategory(
    val index: Int,
    val title: String,
    val icon: ImageVector,
    val pages: List<ControllerMenuPage>,
) {
    CONTROL_PHASE(
        index = 1,
        title = "کنترل فاز",
        icon = Icons.Outlined.Bolt,
        pages = listOf(
            ControllerMenuPage(
                number = 1,
                title = "ولتاژ و نوع برق",
                registerKeys = listOf(
                    "control_phase_activation",
                    "control_phase_type",
                    "control_phase_min_voltage",
                    "control_phase_max_voltage",
                ),
            ),
            ControllerMenuPage(
                number = 2,
                title = "تاخیر و حساسیت",
                registerKeys = listOf(
                    "control_phase_delay",
                    "control_phase_sensitivity",
                ),
            ),
        ),
    ),
    FLOATER(
        index = 2,
        title = "فلوتر مخزن",
        icon = Icons.Outlined.WaterDrop,
        pages = listOf(
            ControllerMenuPage(
                number = 1,
                title = "تنظیمات فلوتر",
                registerKeys = listOf(
                    "floater_type",
                    "floater_trigger_level",
                    "floater_delay",
                ),
            ),
        ),
    ),
    SENSOR(
        index = 3,
        title = "سنسور فشار",
        icon = Icons.Outlined.Sensors,
        pages = listOf(
            ControllerMenuPage(
                number = 1,
                title = "نوع و رنج سنسور",
                registerKeys = listOf(
                    "pressure_sensor_input_type",
                    "pressure_sensor_output_type",
                ),
            ),
            ControllerMenuPage(
                number = 2,
                title = "حداکثر فشار و کالیبراسیون",
                registerKeys = listOf(
                    "max_pressure_value",
                    "sensor_calibration_value",
                ),
            ),
        ),
    ),
    CONTROL(
        index = 4,
        title = "کنترل پمپ‌ها",
        icon = Icons.Outlined.Speed,
        pages = listOf(
            ControllerMenuPage(
                number = 1,
                title = "تعداد پمپ و درایو",
                registerKeys = listOf("number_of_pump", "number_of_variable_pump"),
            ),
            ControllerMenuPage(
                number = 2,
                title = "مد کاری (سیم‌بندی)",
                registerKeys = listOf("wiring_mode"),
            ),
            ControllerMenuPage(
                number = 3,
                title = "سرویس پمپ‌ها و درایو",
                registerKeys = listOf(
                    Bpc02RegisterMap.DRIVE_ACTIVATION,
                    "pump_1_activation",
                    "pump_2_activation",
                    "pump_3_activation",
                    "pump_4_activation",
                    "pump_5_activation",
                    "pump_6_activation",
                ),
            ),
            ControllerMenuPage(
                number = 4,
                title = "حالت کنترل و فشار پله‌ای",
                registerKeys = listOf(
                    Bpc02RegisterMap.CONTROL_STATUS,
                    "control_direction",
                    Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT,
                    "pump_1_on_delay",
                    "pump_1_off_pressure",
                    "pump_1_off_delay",
                    "pump_2_on_pressure",
                    "pump_2_on_delay",
                    "pump_2_off_pressure",
                    "pump_2_off_delay",
                    "pump_3_on_pressure",
                    "pump_3_on_delay",
                    "pump_3_off_pressure",
                    "pump_3_off_delay",
                    "pump_4_on_pressure",
                    "pump_4_on_delay",
                    "pump_4_off_pressure",
                    "pump_4_off_delay",
                ),
            ),
            ControllerMenuPage(
                number = 5,
                title = "افزودن و خروج پمپ",
                registerKeys = listOf(
                    "constant_pump_percent",
                    "add_delay",
                    "remove_delay",
                    "mode3_switch_time",
                ),
            ),
            ControllerMenuPage(
                number = 6,
                title = "حالت خواب (Sleep)",
                registerKeys = listOf(
                    "sleep_frequency",
                    "sleep_off_delay",
                    "sleep_gap",
                ),
            ),
            ControllerMenuPage(
                number = 7,
                title = "بیداری و فرکانس‌ها",
                registerKeys = listOf(
                    "wakeup_differential",
                    "wakeup_on_delay",
                    "start_frequency",
                    "stop_frequency",
                ),
            ),
            ControllerMenuPage(
                number = 8,
                title = "جابه‌جایی و پمپ جاکی",
                registerKeys = listOf(
                    "off_changing_activation",
                    "jockey_type",
                ),
            ),
            ControllerMenuPage(
                number = 9,
                title = "تغییر نوبت زمانی",
                registerKeys = listOf(
                    "time_changing_activation",
                    "time_changing_time",
                ),
            ),
            ControllerMenuPage(
                number = 10,
                title = "رزرو خودکار پمپ",
                registerKeys = listOf(
                    "reserve_auto_activation",
                    "reserve_auto_type",
                ),
            ),
            ControllerMenuPage(
                number = 11,
                title = "حفاظت تمام‌بار",
                registerKeys = listOf(
                    "full_load_status",
                    "full_load_pressure",
                    "full_load_time",
                ),
            ),
            ControllerMenuPage(
                number = 12,
                title = "اتو سرویس",
                registerKeys = listOf(
                    "auto_service_off_time",
                    "auto_service_on_time",
                    "auto_service_interval",
                ),
            ),
            ControllerMenuPage(
                number = 13,
                title = "تنظیمات PID",
                registerKeys = listOf(
                    "pid_p_value",
                    "pid_i_value",
                    "pid_d_value",
                ),
            ),
        ),
    ),
    MFX(
        index = 5,
        title = "ورودی و خروجی",
        icon = Icons.Outlined.DeveloperBoard,
        pages = listOf(
            ControllerMenuPage(
                number = 1,
                title = "ورودی‌های چندمنظوره",
                registerKeys = listOf("mfi_1_type", "mfi_2_type", "mfi_3_type", "mfi_4_type"),
            ),
            ControllerMenuPage(
                number = 2,
                title = "خروجی‌های چندمنظوره",
                registerKeys = listOf("mfo_1_type", "mfo_2_type", "mfo_3_type", "mfo_4_type"),
            ),
            ControllerMenuPage(
                number = 3,
                title = "خروجی‌های آنالوگ",
                registerKeys = listOf("ao1_type", "ao2_type", "ao1_gain", "ao2_gain"),
            ),
        ),
    ),
    DEVICE(
        index = 6,
        title = "اطلاعات دستگاه",
        icon = Icons.Outlined.Info,
        pages = listOf(
            ControllerMenuPage(
                number = 1,
                title = "مشخصات و شبکه دستگاه",
                registerKeys = listOf("serial_number", "lcd_contrast", "modbus_address"),
            ),
        ),
    ),
}

/**
 * Operational Control & Controller-Matched Settings screen organized around the exact physical
 * DGP BPC02 menu categories and pagination, enforcing two-tier role permissions (Administrator
 * vs Operator) with clean market-appropriate Persian terminology and zero Modbus developer clutter.
 */
@Composable
fun ControlAndConfigScreen(
    uiState: MonitoringUiState,
    userRole: UserRole,
    onReadHardwareConfig: () -> Unit,
    onWriteParameter: (RegisterDefinition, Int) -> Unit,
    onResetPumpErrors: () -> Unit,
    onDismissWriteNotice: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val cfgSnap = uiState.configSnapshot
    val liveSnap = uiState.snapshot
    val isConnected = uiState.connectionState is ConnectionState.Connected
    val isWriting = cfgSnap.writingKey != null

    var pendingEdit by remember { mutableStateOf<PendingEdit?>(null) }
    var selectedCategory by remember { mutableStateOf(ControllerCategory.CONTROL) }
    var pageIndex by remember { mutableIntStateOf(0) }

    // Automatically load controller settings when connected and empty
    LaunchedEffect(isConnected) {
        if (isConnected && cfgSnap.readings.isEmpty() && !cfgSnap.loading && cfgSnap.errors.isEmpty()) {
            onReadHardwareConfig()
        }
    }

    // Build unified value lookup combining configuration readings + freshest live telemetry
    val valueByKey: Map<String, Int?> = remember(cfgSnap.readings, liveSnap) {
        buildMap {
            cfgSnap.readings.forEach { put(it.definition.key, it.raw) }
            liveSnap?.readings?.forEach { if (it.raw != null) put(it.definition.key, it.raw) }
        }
    }

    when (val edit = pendingEdit) {
        is PendingEdit.SwitchConfirm -> SwitchWriteConfirmDialog(
            def = edit.def,
            currentValue = edit.currentValue,
            targetValue = edit.targetValue,
            onDismiss = { pendingEdit = null },
            onConfirm = {
                pendingEdit = null
                onWriteParameter(edit.def, edit.targetValue)
            },
        )
        is PendingEdit.NumberEdit -> NumberEditDialog(
            def = edit.def,
            currentValue = edit.currentValue,
            onDismiss = { pendingEdit = null },
            onConfirm = { newVal ->
                pendingEdit = null
                onWriteParameter(edit.def, newVal)
            },
        )
        is PendingEdit.EnumEdit -> EnumEditDialog(
            def = edit.def,
            currentValue = edit.currentValue,
            onDismiss = { pendingEdit = null },
            onConfirm = { newVal ->
                pendingEdit = null
                onWriteParameter(edit.def, newVal)
            },
        )
        is PendingEdit.ControlModeConfirm -> ControlModeConfirmDialog(
            currentValue = edit.currentValue,
            targetValue = edit.targetValue,
            onDismiss = { pendingEdit = null },
            onConfirm = {
                pendingEdit = null
                onWriteParameter(Bpc02RegisterMap.controlStatusDef, edit.targetValue)
            },
        )
        PendingEdit.ResetErrorsConfirm -> ResetErrorsConfirmDialog(
            onDismiss = { pendingEdit = null },
            onConfirm = {
                pendingEdit = null
                onResetPumpErrors()
            },
        )
        null -> Unit
    }

    val liveSetpointRaw = valueByKey[Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT]
    val liveControlStatus = liveSnap?.controlStatus ?: valueByKey[Bpc02RegisterMap.CONTROL_STATUS]
    val liveDriveEnabled = liveSnap?.driveEnabled?.let { if (it) 1 else 0 } ?: valueByKey[Bpc02RegisterMap.DRIVE_ACTIVATION]
    val visiblePumpNumbers = remember(liveSnap) {
        liveSnap?.visiblePumps?.map { it.number } ?: (1..4).toList()
    }
    val hasDrivers = (liveSnap?.variablePumpCount ?: 1) > 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 0. Write Result Notification Banner
        if (cfgSnap.lastWrite != null) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { -it / 3 },
                    exit = fadeOut(),
                ) {
                    WriteResultBanner(result = cfgSnap.lastWrite, onDismiss = onDismissWriteNotice)
                }
            }
        }

        // 1. Quick Daily Operational Controls (Pressure Setpoint, Pump Service ON/OFF, Error Reset, Control Mode)
        item {
            val canEditControlMode = userRole.canWriteRegister(Bpc02RegisterMap.controlStatusDef) && isConnected && !isWriting
            val canEditSetpoint = userRole.canWriteRegister(Bpc02RegisterMap.pressureSetpointDef) && isConnected && !isWriting
            val canResetErrors = userRole.canOperateBasicControls && isConnected && !isWriting

            ScadaPanel(
                title = "کنترل سریع ایستگاه",
                subtitle = if (userRole == UserRole.ADMIN) {
                    "فشار تنظیم‌شده، حالت کارکرد، رفع خطا و روشن/خاموش کردن پمپ‌ها"
                } else {
                    "تنظیم فشار، رفع خطا و روشن/خاموش کردن پمپ‌ها برای بهره‌برداری روزانه"
                },
                icon = Icons.Outlined.PowerSettingsNew,
                accentColor = p.gold,
                trailing = {
                    if (isWriting) {
                        StatusBadge("در حال ثبت…", BadgeTone.GOLD, pulsing = true)
                    } else if (userRole == UserRole.OPERATOR) {
                        StatusBadge("دسترسی اپراتور", BadgeTone.INFO)
                    }
                },
            ) {
                // System Pressure Setpoint (editable by both Admin & Operator)
                ParameterRow(
                    reading = RegisterReading(Bpc02RegisterMap.pressureSetpointDef, liveSetpointRaw),
                    actionLabel = "تغییر فشار",
                    canEdit = canEditSetpoint,
                    isAdminRestricted = false,
                    isWritingThis = cfgSnap.writingKey == Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT,
                    onClickEdit = {
                        pendingEdit = PendingEdit.NumberEdit(Bpc02RegisterMap.pressureSetpointDef, liveSetpointRaw)
                    },
                )

                HorizontalDivider(color = p.hairline)

                // Controller Operating Status (Disable / PID / ON-OFF — Admin only editable)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("حالت عملکرد کنترلر", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
                            if (!userRole.canEditAdvancedSettings) {
                                Text("تغییر حالت عملکرد مخصوص مدیر سیستم است", style = MaterialTheme.typography.labelSmall, color = p.textMuted)
                            }
                        }
                        if (cfgSnap.writingKey == Bpc02RegisterMap.CONTROL_STATUS) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = p.gold)
                        } else {
                            StatusBadge(
                                PersianLabels.controlStatusShort(liveControlStatus),
                                when (liveControlStatus) {
                                    1, 2 -> BadgeTone.OK
                                    0 -> BadgeTone.WARNING
                                    else -> BadgeTone.NEUTRAL
                                },
                            )
                        }
                    }
                    if (userRole.canEditAdvancedSettings) {
                        DgpSegmentedControl(
                            items = listOf(0, 1, 2),
                            selected = liveControlStatus ?: -1,
                            label = { mode ->
                                when (mode) {
                                    0 -> "خاموش"
                                    1 -> "دورمتغیر (PID)"
                                    else -> "پله‌ای (ON/OFF)"
                                }
                            },
                            onSelect = { target ->
                                if (liveControlStatus != target) {
                                    pendingEdit = PendingEdit.ControlModeConfirm(liveControlStatus, target)
                                }
                            },
                            enabled = canEditControlMode && liveControlStatus != null,
                            height = 40.dp,
                        )
                    }
                }

                HorizontalDivider(color = p.hairline)

                // Reset Pump Errors (both Admin & Operator)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("رفع خطای پمپ‌ها", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
                        Text(
                            "پاک‌سازی خطاهای ثبت‌شده پس از برطرف شدن مشکل",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.textSecondary,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    if (cfgSnap.writingKey == Bpc02RegisterMap.MULTI_FUNCTION_REGISTER) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = p.gold)
                    } else {
                        DgpGoldButton(
                            text = "رفع خطا",
                            icon = Icons.Outlined.RestartAlt,
                            enabled = canResetErrors,
                            onClick = { pendingEdit = PendingEdit.ResetErrorsConfirm },
                            height = 40.dp,
                        )
                    }
                }

                HorizontalDivider(color = p.hairline)

                // Quick Pump & Drive Service ON/OFF toggles with redesigned Pump Icon & PumpPowerToggle
                Text(
                    "روشن / خاموش کردن پمپ‌ها (حالت سرویس)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = p.textPrimary,
                )
                ConfigGroupBox {
                    if (hasDrivers) {
                        val canEditDrive = userRole.canWriteRegister(Bpc02RegisterMap.driveActivationDef) && isConnected && !isWriting
                        PumpServiceRow(
                            title = "درایو (اینورتر)",
                            subtitle = if (liveDriveEnabled == 0) "غیرفعال در مدار (S)" else "فعال در مدار",
                            isOn = liveDriveEnabled?.let { it == 1 },
                            running = liveSnap?.drStatus,
                            failed = liveSnap?.driveFailed,
                            showPumpIcon = false,
                            enabled = canEditDrive,
                            isWritingThis = cfgSnap.writingKey == Bpc02RegisterMap.DRIVE_ACTIVATION,
                            onRequestToggle = { targetOn ->
                                pendingEdit = PendingEdit.SwitchConfirm(
                                    Bpc02RegisterMap.driveActivationDef,
                                    liveDriveEnabled,
                                    if (targetOn) 1 else 0,
                                )
                            },
                        )
                    }
                    visiblePumpNumbers.forEachIndexed { idx, pumpNum ->
                        if (hasDrivers || idx > 0) HorizontalDivider(color = p.hairline)
                        val def = Bpc02RegisterMap.pumpActivationDefs[pumpNum - 1]
                        val pumpState = liveSnap?.pumps?.firstOrNull { it.number == pumpNum }
                        val rawVal = pumpState?.enabled?.let { if (it) 1 else 0 } ?: valueByKey[def.key]
                        val canEditPump = userRole.canWriteRegister(def) && isConnected && !isWriting
                        val isDisabled = rawVal == 0
                        val isRunning = !isDisabled && pumpState?.running == true
                        PumpServiceRow(
                            title = "پمپ $pumpNum",
                            subtitle = when {
                                pumpState?.failed == true -> "دارای خطا"
                                isDisabled -> "خاموش — نمایش S روی کنترلر"
                                isRunning -> "در حال کار"
                                rawVal == 1 -> "روشن و آماده به کار"
                                else -> "وضعیت نامشخص"
                            },
                            isOn = rawVal?.let { it == 1 },
                            running = isRunning,
                            failed = pumpState?.failed,
                            showPumpIcon = true,
                            enabled = canEditPump,
                            isWritingThis = cfgSnap.writingKey == def.key,
                            onRequestToggle = { targetOn ->
                                pendingEdit = PendingEdit.SwitchConfirm(def, rawVal, if (targetOn) 1 else 0)
                            },
                        )
                    }
                }
            }
        }

        // 2. Controller-Based Menu Navigation & Exact Pagination (matching DGP BPC02 LCD menu tree)
        item {
            val pages = selectedCategory.pages
            val safePageIndex = pageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
            val currentPage = pages[safePageIndex]

            ScadaPanel(
                title = "منوی تنظیمات کنترلر",
                subtitle = "دسته‌بندی و صفحه‌بندی مطابق منوی دستگاه DGP BPC02",
                icon = Icons.Outlined.Tune,
                accentColor = p.cyan,
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
                                text = "بروزرسانی",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (canRefresh) p.gold else p.textMuted,
                            )
                        }
                    }
                },
            ) {
                if (!userRole.canEditAdvancedSettings) {
                    NoticeBox(
                        text = "در سطح دسترسی اپراتور، تنظیمات پیشرفته کنترلر فقط قابل مشاهده است. برای ویرایش پارامترهای فنی با حساب «مدیر سیستم» وارد شوید.",
                        tone = BadgeTone.INFO,
                    )
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
                        text = "دریافت برخی تنظیمات با تاخیر مواجه شد؛ دکمه «بروزرسانی» را بزنید.",
                        tone = BadgeTone.WARNING,
                    )
                }

                // 6-Category Horizontal Controller Menu Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ControllerCategory.entries.forEach { category ->
                        val isSelected = category == selectedCategory
                        DgpPillChip(
                            label = "${category.index}. ${category.title}",
                            selected = isSelected,
                            onClick = {
                                if (selectedCategory != category) {
                                    selectedCategory = category
                                    pageIndex = 0
                                }
                            },
                            accent = p.gold,
                        )
                    }
                }

                // Controller Page Navigator Bar (dedicated full-width page title + clean < قبلی | صفحه X از Y | بعدی >)
                if (pages.size > 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(p.panelElevated)
                            .border(0.8.dp, p.gold.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Dedicated full-width page title so Persian titles NEVER truncate or collide with buttons
                        Text(
                            text = currentPage.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = p.textPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Deterministic LTR control bar: [< قبلی]   صفحه ۳ از ۱۳   [بعدی >]
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                PageArrowButton(
                                    icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                    label = "قبلی",
                                    iconBeforeLabel = true,
                                    enabled = safePageIndex > 0,
                                    onClick = { pageIndex = (safePageIndex - 1).coerceAtLeast(0) },
                                )
                                Text(
                                    text = "صفحه ${currentPage.number} از ${pages.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = p.gold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                                PageArrowButton(
                                    icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    label = "بعدی",
                                    iconBeforeLabel = false,
                                    enabled = safePageIndex < pages.lastIndex,
                                    onClick = { pageIndex = (safePageIndex + 1).coerceAtMost(pages.lastIndex) },
                                )
                            }
                        }

                        // Quick page jump strip synchronized left-to-right (1..N) with < قبلی and بعدی >
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                pages.forEachIndexed { idx, pg ->
                                    DgpPillChip(
                                        label = "${pg.number}",
                                        selected = idx == safePageIndex,
                                        onClick = { pageIndex = idx },
                                        accent = p.cyan,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = currentPage.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = p.gold,
                    )
                }

                if (!isConnected && cfgSnap.readings.isEmpty() && liveSnap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(p.panelElevated)
                            .border(0.8.dp, p.panelBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            "ابتدا به کنترلر متصل شوید تا تنظیمات دستگاه به‌صورت خودکار بارگذاری شوند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = p.textSecondary,
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = selectedCategory to safePageIndex,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                        label = "controller_menu_page",
                    ) { (cat, pIdx) ->
                        val page = cat.pages[pIdx.coerceIn(0, cat.pages.lastIndex)]
                        // Filter out hidden pumps 5..6 on Control Page 3 when station only has 1..4 pumps
                        val keysToShow = page.registerKeys.filter { key ->
                            if (key == Bpc02RegisterMap.DRIVE_ACTIVATION) {
                                hasDrivers
                            } else if (key.startsWith("pump_") && key.endsWith("_activation")) {
                                val num = key.removePrefix("pump_").removeSuffix("_activation").toIntOrNull()
                                num == null || num in visiblePumpNumbers
                            } else {
                                true
                            }
                        }
                        ConfigGroupBox {
                            keysToShow.forEachIndexed { idx, key ->
                                if (idx > 0) HorizontalDivider(color = p.hairline)
                                val def = Bpc02RegisterMap.byKey(key)
                                val raw = valueByKey[key]
                                val reading = RegisterReading(def, raw)
                                val roleCanWrite = userRole.canWriteRegister(def)
                                val canEditParam = roleCanWrite && isConnected && !isWriting
                                val isAdminRestricted = def.isWritable && !roleCanWrite

                                when {
                                    def.table == VendorTable.BIT && def.isWritable -> {
                                        val isPump = key.startsWith("pump_") && key.endsWith("_activation")
                                        val pNum = if (isPump) key.removePrefix("pump_").removeSuffix("_activation").toIntOrNull() else null
                                        val pState = pNum?.let { n -> liveSnap?.pumps?.firstOrNull { it.number == n } }
                                        PumpServiceRow(
                                            title = PersianLabels.registerLabel(def),
                                            subtitle = if (raw == 0) "خاموش (S)" else if (raw == 1) "روشن در مدار" else "—",
                                            isOn = raw?.let { it == 1 },
                                            running = if (raw == 0) false else pState?.running,
                                            failed = pState?.failed,
                                            showPumpIcon = isPump,
                                            enabled = canEditParam,
                                            isWritingThis = cfgSnap.writingKey == key,
                                            onRequestToggle = { targetOn ->
                                                pendingEdit = PendingEdit.SwitchConfirm(def, raw, if (targetOn) 1 else 0)
                                            },
                                        )
                                    }
                                    def.key == Bpc02RegisterMap.CONTROL_STATUS -> {
                                        ParameterRow(
                                            reading = reading,
                                            actionLabel = "تغییر حالت",
                                            canEdit = canEditParam,
                                            isAdminRestricted = isAdminRestricted,
                                            isWritingThis = cfgSnap.writingKey == key,
                                            onClickEdit = {
                                                pendingEdit = PendingEdit.EnumEdit(def, raw)
                                            },
                                        )
                                    }
                                    def.isWritable -> {
                                        val isEnum = def.enumLabels.isNotEmpty()
                                        ParameterRow(
                                            reading = reading,
                                            actionLabel = if (isEnum) "انتخاب" else "ویرایش",
                                            canEdit = canEditParam,
                                            isAdminRestricted = isAdminRestricted,
                                            isWritingThis = cfgSnap.writingKey == key,
                                            onClickEdit = {
                                                pendingEdit = if (isEnum) {
                                                    PendingEdit.EnumEdit(def, raw)
                                                } else {
                                                    PendingEdit.NumberEdit(def, raw)
                                                }
                                            },
                                        )
                                    }
                                    else -> {
                                        ReadOnlyParameterRow(reading = reading)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageArrowButton(
    icon: ImageVector,
    label: String,
    iconBeforeLabel: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val p = LocalScadaPalette.current
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (enabled) p.panel else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = if (enabled) BorderStroke(0.8.dp, p.gold.copy(alpha = 0.35f)) else BorderStroke(0.8.dp, p.panelBorder.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (iconBeforeLabel) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (enabled) p.gold else p.textMuted.copy(alpha = 0.45f),
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) p.textPrimary else p.textMuted.copy(alpha = 0.45f),
                softWrap = false,
            )
            if (!iconBeforeLabel) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (enabled) p.gold else p.textMuted.copy(alpha = 0.45f),
                    modifier = Modifier.size(18.dp),
                )
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
                        "تغییر «$label» با موفقیت ثبت شد"
                    } else {
                        "ثبت «$label» انجام نشد"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                )
                Text(
                    text = if (result.success) {
                        val confirmed = result.readBackValue?.let { PersianLabels.formatRegisterValue(def, it) } ?: "—"
                        "مقدار جدید: $confirmed  •  ساعت ${formatTime(result.timestampMillis)}"
                    } else {
                        "لطفاً اتصال به کنترلر را بررسی کرده و دوباره تلاش کنید."
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
private fun PumpServiceRow(
    title: String,
    subtitle: String,
    isOn: Boolean?,
    running: Boolean?,
    failed: Boolean?,
    showPumpIcon: Boolean,
    enabled: Boolean,
    isWritingThis: Boolean,
    onRequestToggle: (Boolean) -> Unit,
) {
    val p = LocalScadaPalette.current
    val isDisabled = isOn == false
    val isReady = isOn == true && running != true && failed != true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showPumpIcon) {
                AnimatedImpellerIcon(
                    running = running == true && !isDisabled,
                    failed = failed,
                    disabled = isDisabled,
                    ready = isReady,
                    size = 38.dp,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = p.textPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        failed == true -> p.fault
                        isDisabled -> p.warning
                        running == true -> p.ok
                        else -> p.textSecondary
                    },
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        if (isWritingThis) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = p.gold)
        } else {
            PumpPowerToggle(
                isOn = isOn,
                enabled = enabled,
                onToggle = onRequestToggle,
            )
        }
    }
}

@Composable
private fun ParameterRow(
    reading: RegisterReading,
    actionLabel: String,
    canEdit: Boolean,
    isAdminRestricted: Boolean,
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
            Text(
                text = reading.raw?.let { PersianLabels.formatRegisterValue(def, it) } ?: "—",
                style = MaterialTheme.typography.titleSmall,
                color = if (reading.raw != null) p.cyan else p.textSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        when {
            isWritingThis -> {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = p.gold)
            }
            isAdminRestricted -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(p.panel)
                        .border(0.8.dp, p.panelBorder, RoundedCornerShape(9.dp))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = p.textMuted, modifier = Modifier.size(12.dp))
                    Text("مخصوص مدیر", style = MaterialTheme.typography.labelSmall, color = p.textMuted)
                }
            }
            else -> {
                val active = canEdit && reading.raw != null
                Surface(
                    onClick = onClickEdit,
                    enabled = active,
                    color = if (active) p.goldSoft else p.panel,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.8.dp, if (active) p.gold.copy(alpha = 0.50f) else p.panelBorder),
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
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (active) p.gold else p.textMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyParameterRow(reading: RegisterReading) {
    val p = LocalScadaPalette.current
    val def = reading.definition

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = PersianLabels.registerLabel(def),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = p.textPrimary,
            )
            Text(
                text = "فقط نمایش",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = p.textMuted,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = reading.raw?.let { PersianLabels.formatRegisterValue(def, it) } ?: "—",
            style = MaterialTheme.typography.titleSmall,
            color = if (reading.raw != null) p.textPrimary else p.textMuted,
        )
    }
}

@Composable
private fun ControlModeConfirmDialog(
    currentValue: Int?,
    targetValue: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val def = Bpc02RegisterMap.controlStatusDef
    val currentText = currentValue?.let { PersianLabels.enumOptionLabel(def, it) } ?: "—"
    val targetText = PersianLabels.enumOptionLabel(def, targetValue)
    val isDisabling = targetValue == 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Icon(
                Icons.Outlined.PowerSettingsNew,
                contentDescription = null,
                tint = if (isDisabling) p.fault else p.gold,
            )
        },
        title = { Text("تایید تغییر حالت عملکرد", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Surface(
                    color = p.panelElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, p.panelBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("حالت فعلی: $currentText", style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                        Text(
                            "حالت جدید: $targetText",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDisabling) p.fault else p.gold,
                        )
                    }
                }
                if (isDisabling) {
                    NoticeBox(
                        text = "توجه: انتخاب حالت «خاموش» باعث توقف کامل ایستگاه بوستر پمپ و قطع تامین فشار می‌شود.",
                        tone = BadgeTone.FAULT,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDisabling) p.fault else DialogGoldBg,
                    contentColor = if (isDisabling) Color.White else DialogGoldInk,
                ),
            ) {
                Text("تایید و ثبت", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = p.textSecondary) }
        },
    )
}

@Composable
private fun ResetErrorsConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val p = LocalScadaPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = p.gold) },
        title = { Text("تایید رفع خطای پمپ‌ها", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Text(
                "فرمان رفع خطای پمپ‌ها به کنترلر ارسال می‌شود. پیش از تایید، از برطرف شدن عامل خطا اطمینان حاصل کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = p.textSecondary,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DialogGoldBg, contentColor = DialogGoldInk),
            ) {
                Text("تایید و رفع خطا", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = p.textSecondary) }
        },
    )
}

@Composable
private fun SwitchWriteConfirmDialog(
    def: RegisterDefinition,
    currentValue: Int?,
    targetValue: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val currentText = currentValue?.let { if (it == 1) "روشن" else "خاموش (S)" } ?: "—"
    val targetText = if (targetValue == 1) "روشن" else "خاموش (S)"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null, tint = p.gold) },
        title = { Text("تایید تغییر وضعیت", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    PersianLabels.registerLabel(def),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = p.textPrimary,
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
                        Text("وضعیت فعلی: $currentText", style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                        Text(
                            "وضعیت جدید: $targetText",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (targetValue == 1) p.ok else p.warning,
                        )
                    }
                }
                Text(
                    "تغییر وضعیت به کنترلر ارسال و بلافاصله روی دستگاه اعمال می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textSecondary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DialogGoldBg, contentColor = DialogGoldInk),
            ) {
                Text("تایید و ثبت", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = p.textSecondary) }
        },
    )
}

@Composable
private fun NumberEditDialog(
    def: RegisterDefinition,
    currentValue: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val p = LocalScadaPalette.current
    val min = def.minValue ?: 0
    val max = def.maxValue ?: 65535
    val scale = def.scale ?: 1.0
    val usesDecimalInput = scale < 1.0

    val initialText = remember(def.key, currentValue) {
        if (currentValue == null) {
            ""
        } else if (usesDecimalInput) {
            val decimals = if (scale >= 0.1) 1 else 2
            "%.${decimals}f".format(Locale.US, currentValue * scale)
        } else {
            currentValue.toString()
        }
    }
    var text by remember(def.key, currentValue) { mutableStateOf(initialText) }

    val parsedRaw: Int? = remember(text, scale) {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty()) return@remember null
        if (usesDecimalInput) {
            normalized.toDoubleOrNull()?.let { (it / scale).roundToInt() }
        } else {
            normalized.toIntOrNull()
        }
    }
    val inRange = parsedRaw != null && parsedRaw in min..max
    val changed = parsedRaw != null && parsedRaw != currentValue

    val rangeSummary = "بازه مجاز: ${PersianLabels.formatRegisterValue(def, min)} تا ${PersianLabels.formatRegisterValue(def, max)}"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = p.gold) },
        title = { Text("ویرایش تنظیم", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    PersianLabels.registerLabel(def),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = p.textPrimary,
                )
                currentValue?.let { cur ->
                    Text(
                        "مقدار فعلی: ${PersianLabels.formatRegisterValue(def, cur)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textSecondary,
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        text = if (usesDecimalInput) {
                            input.filter { it.isDigit() || it == '.' || it == ',' }
                        } else {
                            input.filter { it.isDigit() }
                        }
                    },
                    label = { Text("مقدار جدید") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = text.isNotEmpty() && !inRange,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (usesDecimalInput) KeyboardType.Decimal else KeyboardType.Number,
                    ),
                    supportingText = {
                        when {
                            text.isEmpty() -> Text(rangeSummary)
                            !inRange -> Text("خارج از بازه مجاز ($rangeSummary)", color = p.fault)
                            else -> Text(
                                "مقدار جدید: ${PersianLabels.formatRegisterValue(def, parsedRaw!!)}",
                                color = p.ok,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (inRange) parsedRaw?.let(onConfirm) },
                enabled = inRange && changed,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DialogGoldBg, contentColor = DialogGoldInk),
            ) {
                Text("تایید و ثبت", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = p.textSecondary) }
        },
    )
}

@Composable
private fun EnumEditDialog(
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
        title = { Text(PersianLabels.registerLabel(def), style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            text = PersianLabels.enumOptionLabel(def, code),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSel) p.gold else p.textPrimary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected) },
                enabled = selected != currentValue,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DialogGoldBg, contentColor = DialogGoldInk),
            ) {
                Text("تایید و ثبت", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = p.textSecondary) }
        },
    )
}

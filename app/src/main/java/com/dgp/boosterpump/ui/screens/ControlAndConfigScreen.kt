package com.dgp.boosterpump.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Input
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.dgp.boosterpump.auth.OperatorPermissions
import com.dgp.boosterpump.auth.UserRole
import com.dgp.boosterpump.config.ConfigImportValidationResult
import com.dgp.boosterpump.config.ControllerConfigTransfer
import com.dgp.boosterpump.controller.Bpc02Catalog
import com.dgp.boosterpump.controller.ControllerModel
import com.dgp.boosterpump.controller.ControllerSnapshot
import com.dgp.boosterpump.controller.RegisterReading
import com.dgp.boosterpump.controller.SystemErrorBit2
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.RegisterDefinition
import com.dgp.boosterpump.controller.register.VendorTable
import com.dgp.boosterpump.session.ConfigImportProgress
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.session.WriteResult
import com.dgp.boosterpump.ui.components.AnimatedImpellerIcon
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.DgpGoldButton
import com.dgp.boosterpump.ui.components.DgpOutlinedButton
import com.dgp.boosterpump.ui.components.DgpSegmentedControl
import com.dgp.boosterpump.ui.components.LtrText
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.ParameterHelpBottomSheet
import com.dgp.boosterpump.ui.components.ParameterHelpIcon
import com.dgp.boosterpump.ui.components.PlcLedBead
import com.dgp.boosterpump.ui.components.PumpPowerToggle
import com.dgp.boosterpump.ui.components.ScadaPanel
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.ui.components.formatTime
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.ui.theme.MonoStyle
import com.dgp.boosterpump.viewmodel.MonitoringUiState
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

private val DialogGoldBg = Color(0xFFD09629)
private val DialogGoldInk = Color(0xFF0A0702)

/** Pending edit state driving one of the confirmation/edit dialogs. */
private sealed interface PendingEdit {
    data class SwitchConfirm(val def: RegisterDefinition, val currentValue: Int?, val targetValue: Int) : PendingEdit
    data class NumberEdit(val def: RegisterDefinition, val currentValue: Int?) : PendingEdit
    data class EnumEdit(val def: RegisterDefinition, val currentValue: Int?) : PendingEdit
    data class ControlModeConfirm(val currentValue: Int?, val targetValue: Int) : PendingEdit
    data object ResetErrorsConfirm : PendingEdit
}

private data class PreparedExportPayload(
    val fileName: String,
    val jsonContent: String,
    val parameterCount: Int,
    val modelLabel: String,
    val wiringSummary: String,
)

private fun mainMenuIcon(section: Bpc02Catalog.MainMenuSection): ImageVector = when (section) {
    Bpc02Catalog.MainMenuSection.SYSTEM -> Icons.Outlined.Settings
    Bpc02Catalog.MainMenuSection.INPUT -> Icons.AutoMirrored.Outlined.Input
    Bpc02Catalog.MainMenuSection.TIME_DATE -> Icons.Outlined.Schedule
    Bpc02Catalog.MainMenuSection.HARDWARE -> Icons.Outlined.Memory
}

private fun categoryIcon(category: Bpc02Catalog.MenuCategory): ImageVector = when (category) {
    Bpc02Catalog.MenuCategory.CONTROL_PHASE -> Icons.Outlined.Bolt
    Bpc02Catalog.MenuCategory.FLOATER -> Icons.Outlined.WaterDrop
    Bpc02Catalog.MenuCategory.SENSOR -> Icons.Outlined.Sensors
    Bpc02Catalog.MenuCategory.CONTROL -> Icons.Outlined.Speed
    Bpc02Catalog.MenuCategory.MFX -> Icons.Outlined.DeveloperBoard
    Bpc02Catalog.MenuCategory.SERIAL_NUMBER -> Icons.Outlined.Tag
    Bpc02Catalog.MenuCategory.NETWORK -> Icons.Outlined.SettingsEthernet
    Bpc02Catalog.MenuCategory.RESET_TO_FACTORY -> Icons.Outlined.RestartAlt
}

/**
 * Operational Control, Exact Physical Controller Menu (1.System, 2.Input, 3.Time/Date,
 * 4.Hardware with categories 1–8 and mode-aware ON/OFF & PID pages), and Portable JSON
 * Configuration Transfer (Import/Export).
 */
@Composable
fun ControlAndConfigScreen(
    uiState: MonitoringUiState,
    userRole: UserRole,
    operatorPermissions: OperatorPermissions = OperatorPermissions.READ_ONLY_DEFAULT,
    onReadHardwareConfig: () -> Unit,
    onWriteParameter: (RegisterDefinition, Int) -> Unit,
    onApplyImportedConfig: (List<Pair<RegisterDefinition, Int>>) -> Unit,
    onDismissImportProgress: () -> Unit,
    onResetPumpErrors: () -> Unit,
    onDismissWriteNotice: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val context = LocalContext.current
    val effectivePermissions = userRole.effectivePermissions(operatorPermissions)
    val cfgSnap = uiState.configSnapshot
    val liveSnap = uiState.snapshot
    val isConnected = uiState.connectionState is ConnectionState.Connected
    val isImporting = cfgSnap.importProgress?.applying == true
    val isWriting = cfgSnap.writingKey != null || isImporting
    val activeModel = liveSnap?.model ?: uiState.activeConfig?.controllerModel ?: uiState.savedConfig.controllerModel

    var pendingEdit by remember { mutableStateOf<PendingEdit?>(null) }
    var activeHelp by remember { mutableStateOf<Pair<Bpc02Catalog.ParameterHelp, String?>?>(null) }
    var expandedMainSection by remember { mutableStateOf<Bpc02Catalog.MainMenuSection?>(Bpc02Catalog.MainMenuSection.HARDWARE) }
    var expandedCategory by remember { mutableStateOf<Bpc02Catalog.MenuCategory?>(null) }
    var expandedPageNumber by remember { mutableStateOf<Int?>(null) }
    var showAllControlPages by remember { mutableStateOf(false) }

    // Import/Export workflow states
    var preparedExport by remember { mutableStateOf<PreparedExportPayload?>(null) }
    var importPreview by remember { mutableStateOf<ConfigImportValidationResult?>(null) }
    var transferNotice by remember { mutableStateOf<Pair<String, BadgeTone>?>(null) }

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

    // Android Storage Access Framework (SAF): Save exported JSON to user-chosen location
    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val payload = preparedExport
        if (uri != null && payload != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.jsonContent.toByteArray(Charsets.UTF_8))
                } ?: error("Cannot open output stream")
            }.onSuccess {
                preparedExport = null
                transferNotice = "فایل تنظیمات (${payload.fileName}) با موفقیت ذخیره شد." to BadgeTone.OK
            }.onFailure { err ->
                transferNotice = "ذخیره فایل ناموفق بود: ${err.message ?: "خطای دسترسی"}" to BadgeTone.FAULT
            }
        }
    }

    // Android SAF: Open JSON configuration file for strict validation & preview
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.size > ControllerConfigTransfer.MAX_IMPORT_BYTES) {
                        error("حجم فایل بیش از حد مجاز (۵۱۲ کیلوبایت) است.")
                    }
                    String(bytes, Charsets.UTF_8)
                } ?: error("امکان خواندن فایل انتخاب‌شده وجود ندارد.")
            }.onSuccess { text ->
                transferNotice = null
                importPreview = ControllerConfigTransfer.parseAndValidate(
                    jsonText = text,
                    targetModel = activeModel,
                    currentValuesByKey = valueByKey,
                    userRole = userRole,
                    operatorPermissions = operatorPermissions,
                )
            }.onFailure { err ->
                transferNotice = "خواندن فایل ناموفق بود: ${err.message ?: "خطای ناشناخته"}" to BadgeTone.FAULT
            }
        }
    }

    fun shareExportedJson(payload: PreparedExportPayload) {
        runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outFile = File(exportDir, payload.fileName)
            outFile.writeText(payload.jsonContent, Charsets.UTF_8)
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile,
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "DGP ${payload.modelLabel} Configuration")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Configuration File"))
        }.onSuccess {
            preparedExport = null
            transferNotice = "فایل آماده اشتراک‌گذاری شد (${payload.fileName})." to BadgeTone.INFO
        }.onFailure { err ->
            transferNotice = "اشتراک‌گذاری فایل ناموفق بود: ${err.message ?: "خطا"}" to BadgeTone.FAULT
        }
    }

    // Interactive Persian Help Bottom Sheet
    activeHelp?.let { (help, curDisplay) ->
        ParameterHelpBottomSheet(
            help = help,
            currentValue = curDisplay,
            onDismiss = { activeHelp = null },
        )
    }

    // Export Options Dialog (Save to Device / Share)
    preparedExport?.let { payload ->
        ExportConfigDialog(
            payload = payload,
            onSaveFile = { saveDocumentLauncher.launch(payload.fileName) },
            onShareFile = { shareExportedJson(payload) },
            onDismiss = { preparedExport = null },
        )
    }

    // Import Validation & Diff Confirmation Dialog
    importPreview?.let { result ->
        ImportConfigPreviewDialog(
            result = result,
            isConnected = isConnected,
            isWriting = isWriting,
            targetModel = activeModel,
            onConfirmApply = {
                val changes = result.changesToApply.map { it.definition to it.targetValue }
                importPreview = null
                if (changes.isNotEmpty()) {
                    onApplyImportedConfig(changes)
                } else {
                    transferNotice = "تمام مقادیر قابل ویرایش این فایل با تنظیمات فعلی کنترلر یکسان هستند." to BadgeTone.INFO
                }
            },
            onDismiss = { importPreview = null },
        )
    }

    // Parameter Write Confirmation / Edit Dialogs
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
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // 0A. Write Result Notification Banner
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

        // 0B. Live Configuration Import Progress / Completion Banner
        if (cfgSnap.importProgress != null) {
            item {
                ImportProgressBanner(
                    progress = cfgSnap.importProgress,
                    onDismiss = onDismissImportProgress,
                )
            }
        }

        // 0C. File Export / Read Notice Banner
        transferNotice?.let { (msg, tone) ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    NoticeBox(text = msg, tone = tone, modifier = Modifier.weight(1f))
                    IconButton(onClick = { transferNotice = null }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "بستن", tint = p.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // 1. Quick Operational Controls (Setpoint, Control Status, Reset Pump Error, Pump/Drive Service)
        item {
            val roleCanEditControlMode = userRole.canWriteRegister(Bpc02RegisterMap.controlStatusDef, operatorPermissions)
            val roleCanEditSetpoint = userRole.canWriteRegister(Bpc02RegisterMap.pressureSetpointDef, operatorPermissions)
            val roleCanResetErrors = effectivePermissions.canResetPumpErrors
            val canEditControlMode = roleCanEditControlMode && isConnected && !isWriting
            val canEditSetpoint = roleCanEditSetpoint && isConnected && !isWriting
            val canResetErrors = roleCanResetErrors && isConnected && !isWriting

            ScadaPanel(
                title = "Quick Control",
                subtitle = when {
                    userRole == UserRole.ADMIN -> "تنظیم سریع Setpoint، حالت عملکرد، رفع خطا و سرویس پمپ‌ها"
                    effectivePermissions.hasAnyDailyControlWrite -> "کنترل‌های فعال‌شده توسط مدیر سیستم قابل ویرایش هستند"
                    else -> "حالت فقط خواندنی — برای اعمال تغییرات، مجوز باید توسط مدیر سیستم فعال شود"
                },
                icon = Icons.Outlined.PowerSettingsNew,
                accentColor = p.gold,
                trailing = {
                    if (isWriting) {
                        StatusBadge("Writing…", BadgeTone.GOLD, pulsing = true)
                    } else if (userRole == UserRole.OPERATOR) {
                        if (effectivePermissions.hasAnyWritePermission) {
                            StatusBadge("Operator", BadgeTone.INFO)
                        } else {
                            StatusBadge("Read-Only", BadgeTone.NEUTRAL)
                        }
                    }
                },
            ) {
                // System Pressure Setpoint
                ParameterRow(
                    reading = RegisterReading(Bpc02RegisterMap.pressureSetpointDef, liveSetpointRaw),
                    actionLabel = "Edit",
                    canEdit = canEditSetpoint,
                    isAdminRestricted = !roleCanEditSetpoint,
                    isWritingThis = cfgSnap.writingKey == Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT,
                    onClickEdit = {
                        pendingEdit = PendingEdit.NumberEdit(Bpc02RegisterMap.pressureSetpointDef, liveSetpointRaw)
                    },
                    onClickHelp = {
                        val cur = liveSetpointRaw?.let { Bpc02Catalog.formatValue(Bpc02RegisterMap.pressureSetpointDef, it) }
                        activeHelp = Bpc02Catalog.helpFor(Bpc02RegisterMap.pressureSetpointDef) to cur
                    },
                )

                HorizontalDivider(color = p.hairline)

                // Control Status (Disable / PID / ON/OFF)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                LtrText("Control Status", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
                                ParameterHelpIcon(
                                    onClick = {
                                        activeHelp = Bpc02Catalog.helpFor(Bpc02RegisterMap.controlStatusDef) to
                                            Bpc02Catalog.controlStatusLabel(liveControlStatus)
                                    },
                                )
                            }
                            if (!roleCanEditControlMode) {
                                Text("تغییر حالت عملکرد نیازمند مجوز مدیر سیستم است", style = MaterialTheme.typography.labelSmall, color = p.textMuted)
                            }
                        }
                        if (cfgSnap.writingKey == Bpc02RegisterMap.CONTROL_STATUS) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = p.gold)
                        } else {
                            StatusBadge(
                                Bpc02Catalog.controlStatusLabel(liveControlStatus),
                                when (liveControlStatus) {
                                    1, 2 -> BadgeTone.OK
                                    0 -> BadgeTone.WARNING
                                    else -> BadgeTone.NEUTRAL
                                },
                            )
                        }
                    }
                    if (roleCanEditControlMode) {
                        DgpSegmentedControl(
                            items = listOf(0, 1, 2),
                            selected = liveControlStatus ?: -1,
                            label = { mode -> Bpc02Catalog.controlStatusLabel(mode) },
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

                // Reset Pump Error command (Word 99 = 25)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            LtrText("Reset Pump Error", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
                            ParameterHelpIcon(
                                onClick = { activeHelp = Bpc02Catalog.conceptHelp("reset_pump_error") to null },
                            )
                        }
                        Text(
                            if (roleCanResetErrors) {
                                "پاک‌سازی خطاهای ثبت‌شده پس از برطرف شدن عامل خطا"
                            } else {
                                "ارسال فرمان رفع خطا نیازمند مجوز مدیر سیستم است"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (roleCanResetErrors) p.textSecondary else p.textMuted,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    when {
                        cfgSnap.writingKey == Bpc02RegisterMap.MULTI_FUNCTION_REGISTER -> {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = p.gold)
                        }
                        !roleCanResetErrors -> {
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
                                LtrText("Admin", style = MaterialTheme.typography.labelSmall, color = p.textMuted)
                            }
                        }
                        else -> {
                            DgpGoldButton(
                                text = "Reset",
                                icon = Icons.Outlined.RestartAlt,
                                enabled = canResetErrors,
                                onClick = { pendingEdit = PendingEdit.ResetErrorsConfirm },
                                height = 40.dp,
                            )
                        }
                    }
                }

                HorizontalDivider(color = p.hairline)

                // Quick Pump & Drive Service ON/OFF toggles
                LtrText(
                    "Status (Pump & Drive Service)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = p.textPrimary,
                )
                ConfigGroupBox {
                    if (hasDrivers) {
                        val roleCanEditDrive = userRole.canWriteRegister(Bpc02RegisterMap.driveActivationDef, operatorPermissions)
                        val canEditDrive = roleCanEditDrive && isConnected && !isWriting
                        PumpServiceRow(
                            title = "Drive",
                            subtitle = if (liveDriveEnabled == 0) "Disable — Service (S)" else "Enable",
                            isOn = liveDriveEnabled?.let { it == 1 },
                            running = liveSnap?.drStatus,
                            failed = liveSnap?.driveFailed,
                            showPumpIcon = false,
                            enabled = canEditDrive,
                            isRestricted = !roleCanEditDrive,
                            isWritingThis = cfgSnap.writingKey == Bpc02RegisterMap.DRIVE_ACTIVATION,
                            onRequestToggle = { targetOn ->
                                pendingEdit = PendingEdit.SwitchConfirm(
                                    Bpc02RegisterMap.driveActivationDef,
                                    liveDriveEnabled,
                                    if (targetOn) 1 else 0,
                                )
                            },
                            onClickHelp = {
                                val cur = liveDriveEnabled?.let { if (it == 1) "Enable" else "Disable" }
                                activeHelp = Bpc02Catalog.helpFor(Bpc02RegisterMap.driveActivationDef) to cur
                            },
                        )
                    }
                    visiblePumpNumbers.forEachIndexed { idx, pumpNum ->
                        if (hasDrivers || idx > 0) HorizontalDivider(color = p.hairline)
                        val def = Bpc02RegisterMap.pumpActivationDefs[pumpNum - 1]
                        val pumpState = liveSnap?.pumps?.firstOrNull { it.number == pumpNum }
                        val rawVal = pumpState?.enabled?.let { if (it) 1 else 0 } ?: valueByKey[def.key]
                        val roleCanEditPump = userRole.canWriteRegister(def, operatorPermissions)
                        val canEditPump = roleCanEditPump && isConnected && !isWriting
                        val isDisabled = rawVal == 0
                        val isRunning = !isDisabled && pumpState?.running == true
                        PumpServiceRow(
                            title = "Pump $pumpNum",
                            subtitle = when {
                                pumpState?.failed == true -> "Fault"
                                isDisabled -> "Disable — Service (S)"
                                isRunning -> "Running"
                                rawVal == 1 -> "Enable — Standby"
                                else -> "Unknown"
                            },
                            isOn = rawVal?.let { it == 1 },
                            running = isRunning,
                            failed = pumpState?.failed,
                            showPumpIcon = true,
                            enabled = canEditPump,
                            isRestricted = !roleCanEditPump,
                            isWritingThis = cfgSnap.writingKey == def.key,
                            onRequestToggle = { targetOn ->
                                pendingEdit = PendingEdit.SwitchConfirm(def, rawVal, if (targetOn) 1 else 0)
                            },
                            onClickHelp = {
                                val cur = rawVal?.let { if (it == 1) "Enable" else "Disable" }
                                activeHelp = Bpc02Catalog.helpFor(def) to cur
                            },
                        )
                    }
                }
            }
        }

        // 2. Official BPC-02 Controller Menu (Exact 1.System, 2.Input, 3.Time/Date, 4.Hardware [1..8] from controller LCD)
        val canSeeHardwareMenu = effectivePermissions.canViewHardwareMenu || effectivePermissions.hasAnyHardwareMenuWrite
        if (!canSeeHardwareMenu) {
            item {
                ScadaPanel(
                    title = "Controller Menu (BPC-02)",
                    subtitle = "ساختار منوی تنظیمات کنترلر BPC-02",
                    icon = Icons.Outlined.Tune,
                    accentColor = p.cyan,
                ) {
                    NoticeBox(
                        text = "مشاهده منوی تنظیمات کنترلر برای حساب اپراتور توسط مدیر سیستم محدود شده است.",
                        tone = BadgeTone.INFO,
                    )
                }
            }
        } else {
            item {
                ScadaPanel(
                    title = "Controller Menu (BPC-02)",
                    subtitle = "ساختار و ترتیب دقیق منوی کنترلر — روی هر بخش و آیکون ؟ برای راهنمای فارسی ضربه بزنید",
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
                                LtrText(
                                    text = "Refresh",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (canRefresh) p.gold else p.textMuted,
                                )
                            }
                        }
                    },
                ) {
                    if (userRole == UserRole.OPERATOR) {
                        val anyMenuWritable = Bpc02RegisterMap.configuration.any {
                            userRole.canWriteRegister(it, operatorPermissions)
                        }
                        NoticeBox(
                            text = if (anyMenuWritable) {
                                "پارامترهای دارای دکمه Edit/Select توسط مدیر سیستم برای حساب اپراتور فعال شده‌اند؛ سایر موارد فقط خواندنی هستند."
                            } else {
                                "در سطح دسترسی فعلی اپراتور، منوی تنظیمات کنترلر فقط قابل مشاهده (Read-Only) است."
                            },
                            tone = BadgeTone.INFO,
                        )
                    }

                    if (!isConnected && cfgSnap.readings.isEmpty() && liveSnap == null) {
                        NoticeBox(
                            text = "برای مشاهده مقادیر واقعی دستگاه، ابتدا به کنترلر متصل شوید.",
                            tone = BadgeTone.WARNING,
                        )
                    } else if (cfgSnap.timestampMillis != null) {
                        LtrText(
                            "Last Sync: ${formatTime(cfgSnap.timestampMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.cyan,
                        )
                    }

                    if (cfgSnap.errors.isNotEmpty()) {
                        NoticeBox(
                            text = "دریافت برخی تنظیمات با تاخیر مواجه شد؛ دکمه Refresh را بزنید.",
                            tone = BadgeTone.WARNING,
                        )
                    }

                    // Top-Level Controller Menu matching physical LCD (`photo_6`):
                    // 1. System  |  2. Input  |  3. Time/Date  |  4. Hardware
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Bpc02Catalog.MainMenuSection.entries.forEach { mainSection ->
                            val isSectionOpen = expandedMainSection == mainSection
                            val secChevronAngle by animateFloatAsState(
                                targetValue = if (isSectionOpen) 180f else 0f,
                                animationSpec = tween(220),
                                label = "main_sec_chevron_${mainSection.index}",
                            )
                            val isHardware = mainSection == Bpc02Catalog.MainMenuSection.HARDWARE
                            val secAccent = if (isHardware) p.gold else p.cyan
                            val secBorder = if (isSectionOpen) secAccent.copy(alpha = 0.52f) else p.panelBorder

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSectionOpen && p.isDark) Color(0xFF151C2B) else p.panelElevated)
                                    .border(1.dp, secBorder, RoundedCornerShape(16.dp)),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedMainSection = if (isSectionOpen) null else mainSection
                                        }
                                        .padding(horizontal = 13.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSectionOpen) secAccent.copy(alpha = 0.16f) else p.panel)
                                            .border(
                                                0.9.dp,
                                                if (isSectionOpen) secAccent.copy(alpha = 0.58f) else p.panelBorder,
                                                RoundedCornerShape(10.dp),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "${mainSection.index}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSectionOpen) secAccent else p.textPrimary,
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(
                                                imageVector = mainMenuIcon(mainSection),
                                                contentDescription = null,
                                                tint = if (isSectionOpen) secAccent else p.textSecondary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            LtrText(
                                                text = mainSection.title,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSectionOpen) secAccent else p.textPrimary,
                                            )
                                        }
                                        LtrText(
                                            text = mainSection.subtitleEn,
                                            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 15.sp),
                                            color = p.textSecondary,
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Outlined.ExpandMore,
                                        contentDescription = if (isSectionOpen) "Collapse" else "Expand",
                                        tint = if (isSectionOpen) secAccent else p.textSecondary,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .rotate(secChevronAngle),
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isSectionOpen,
                                    enter = expandVertically(tween(240)) + fadeIn(tween(200)),
                                    exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 10.dp, end = 10.dp, bottom = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(9.dp),
                                    ) {
                                        HorizontalDivider(color = p.hairline)

                                        when (mainSection) {
                                            Bpc02Catalog.MainMenuSection.SYSTEM -> {
                                                SystemMenuSectionBody(
                                                    contrastRaw = valueByKey["lcd_contrast"],
                                                    onRequestHelp = { def, raw ->
                                                        val cur = raw?.let { Bpc02Catalog.formatValue(def, it) }
                                                        activeHelp = Bpc02Catalog.helpFor(def) to cur
                                                    },
                                                )
                                            }
                                            Bpc02Catalog.MainMenuSection.INPUT -> {
                                                InputMenuSectionBody(
                                                    snapshot = liveSnap,
                                                    onRequestDriveInputHelp = {
                                                        val cur = when (liveSnap?.drStatus) {
                                                            true -> "ON"
                                                            false -> "OFF"
                                                            null -> null
                                                        }
                                                        activeHelp = Bpc02Catalog.conceptHelp("dr_status") to cur
                                                    },
                                                )
                                            }
                                            Bpc02Catalog.MainMenuSection.TIME_DATE -> {
                                                TimeDateMenuSectionBody(snapshot = liveSnap)
                                            }
                                            Bpc02Catalog.MainMenuSection.HARDWARE -> {
                                                // Exact 8 Physical Hardware Categories (`photo_9` & `photo_10`)
                                                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                                    Bpc02Catalog.MenuCategory.entries.forEach { category ->
                                                        val isCategoryOpen = expandedCategory == category
                                                        val catChevronAngle by animateFloatAsState(
                                                            targetValue = if (isCategoryOpen) 180f else 0f,
                                                            animationSpec = tween(220),
                                                            label = "cat_chevron_${category.index}",
                                                        )
                                                        val borderColor = if (isCategoryOpen) p.gold.copy(alpha = 0.55f) else p.panelBorder
                                                        val headerBg = if (isCategoryOpen && p.isDark) Color(0xFF1A2233) else p.panel

                                                        val effectivePages = remember(
                                                            category,
                                                            liveControlStatus,
                                                            showAllControlPages,
                                                            visiblePumpNumbers,
                                                        ) {
                                                            if (category != Bpc02Catalog.MenuCategory.CONTROL) {
                                                                category.pages
                                                            } else {
                                                                category.pages.filter { page ->
                                                                    when {
                                                                        showAllControlPages -> true
                                                                        liveControlStatus == 2 -> when (page.controlMode) {
                                                                            Bpc02Catalog.ControlPageMode.ALWAYS -> true
                                                                            Bpc02Catalog.ControlPageMode.ON_OFF_ONLY -> {
                                                                                val pumpNum = page.title.removePrefix("Pump ").toIntOrNull()
                                                                                val maxVisible = (visiblePumpNumbers.maxOrNull() ?: 4).coerceAtMost(4)
                                                                                pumpNum == null || pumpNum <= maxVisible
                                                                            }
                                                                            Bpc02Catalog.ControlPageMode.PID_ONLY -> false
                                                                        }
                                                                        liveControlStatus == 1 ->
                                                                            page.controlMode != Bpc02Catalog.ControlPageMode.ON_OFF_ONLY
                                                                        else -> true
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(14.dp))
                                                                .background(headerBg)
                                                                .border(0.9.dp, borderColor, RoundedCornerShape(14.dp)),
                                                        ) {
                                                            // Category Row Header (1..8, Exact Official English Title)
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable {
                                                                        if (isCategoryOpen) {
                                                                            expandedCategory = null
                                                                            expandedPageNumber = null
                                                                        } else {
                                                                            expandedCategory = category
                                                                            expandedPageNumber = if (effectivePages.size == 1) {
                                                                                effectivePages.first().number
                                                                            } else {
                                                                                null
                                                                            }
                                                                        }
                                                                    }
                                                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(34.dp)
                                                                        .clip(RoundedCornerShape(10.dp))
                                                                        .background(if (isCategoryOpen) p.goldSoft else p.panelElevated)
                                                                        .border(
                                                                            width = 0.85.dp,
                                                                            color = if (isCategoryOpen) p.gold.copy(alpha = 0.60f) else p.panelBorder,
                                                                            shape = RoundedCornerShape(10.dp),
                                                                        ),
                                                                    contentAlignment = Alignment.Center,
                                                                ) {
                                                                    Text(
                                                                        text = "${category.index}",
                                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                                        color = if (isCategoryOpen) p.gold else p.textPrimary,
                                                                    )
                                                                }

                                                                Column(
                                                                    modifier = Modifier.weight(1f),
                                                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                                                ) {
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = categoryIcon(category),
                                                                            contentDescription = null,
                                                                            tint = if (isCategoryOpen) p.gold else p.cyan,
                                                                            modifier = Modifier.size(15.dp),
                                                                        )
                                                                        LtrText(
                                                                            text = category.title,
                                                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                                            color = if (isCategoryOpen) p.gold else p.textPrimary,
                                                                        )
                                                                    }
                                                                    LtrText(
                                                                        text = category.subtitleEn,
                                                                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 15.sp),
                                                                        color = p.textSecondary,
                                                                    )
                                                                }

                                                                Column(
                                                                    horizontalAlignment = Alignment.End,
                                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                                ) {
                                                                    Surface(
                                                                        color = if (isCategoryOpen) p.goldSoft else p.panelElevated,
                                                                        shape = RoundedCornerShape(999.dp),
                                                                        border = BorderStroke(
                                                                            0.7.dp,
                                                                            if (isCategoryOpen) p.gold.copy(alpha = 0.45f) else p.panelBorder,
                                                                        ),
                                                                    ) {
                                                                        val pageLabel = when {
                                                                            category == Bpc02Catalog.MenuCategory.RESET_TO_FACTORY -> "Keypad"
                                                                            effectivePages.size == 1 -> "1 Page"
                                                                            else -> "${effectivePages.size} Pages"
                                                                        }
                                                                        LtrText(
                                                                            text = pageLabel,
                                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.SemiBold,
                                                                            ),
                                                                            color = if (isCategoryOpen) p.gold else p.textSecondary,
                                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                                        )
                                                                    }
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.ExpandMore,
                                                                        contentDescription = if (isCategoryOpen) "Collapse" else "Expand",
                                                                        tint = if (isCategoryOpen) p.gold else p.textSecondary,
                                                                        modifier = Modifier
                                                                            .size(19.dp)
                                                                            .rotate(catChevronAngle),
                                                                    )
                                                                }
                                                            }

                                                            // Expandable Category Body
                                                            AnimatedVisibility(
                                                                visible = isCategoryOpen,
                                                                enter = expandVertically(tween(240)) + fadeIn(tween(200)),
                                                                exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                                                            ) {
                                                                Column(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
                                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                                ) {
                                                                    HorizontalDivider(color = p.hairline)

                                                                    if (category == Bpc02Catalog.MenuCategory.RESET_TO_FACTORY) {
                                                                        ResetFactorySectionBody(
                                                                            onRequestHelp = {
                                                                                activeHelp = Bpc02Catalog.conceptHelp("reset_to_factory") to null
                                                                            },
                                                                        )
                                                                    } else if (effectivePages.size == 1) {
                                                                        ControllerPageParameters(
                                                                            page = effectivePages.first(),
                                                                            valueByKey = valueByKey,
                                                                            liveControlStatus = liveControlStatus,
                                                                            userRole = userRole,
                                                                            operatorPermissions = operatorPermissions,
                                                                            liveSnap = liveSnap,
                                                                            writingKey = cfgSnap.writingKey,
                                                                            isConnected = isConnected,
                                                                            isWriting = isWriting,
                                                                            hasDrivers = hasDrivers,
                                                                            visiblePumpNumbers = visiblePumpNumbers,
                                                                            onRequestPendingEdit = { pendingEdit = it },
                                                                            onRequestHelp = { def, raw ->
                                                                                val cur = raw?.let { Bpc02Catalog.formatValue(def, it) }
                                                                                activeHelp = Bpc02Catalog.helpFor(def) to cur
                                                                            },
                                                                        )
                                                                    } else {
                                                                        // Mode awareness bar inside `4. Control` (PID vs ON/OFF pages)
                                                                        if (category == Bpc02Catalog.MenuCategory.CONTROL) {
                                                                            Row(
                                                                                modifier = Modifier
                                                                                    .fillMaxWidth()
                                                                                    .clip(RoundedCornerShape(10.dp))
                                                                                    .background(p.panelElevated)
                                                                                    .border(0.8.dp, p.panelBorder, RoundedCornerShape(10.dp))
                                                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                                                verticalAlignment = Alignment.CenterVertically,
                                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                            ) {
                                                                                LtrText(
                                                                                    text = "Mode: ${Bpc02Catalog.controlStatusLabel(liveControlStatus)}",
                                                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                                                    color = p.gold,
                                                                                )
                                                                                if (liveControlStatus == 1 || liveControlStatus == 2) {
                                                                                    Surface(
                                                                                        onClick = { showAllControlPages = !showAllControlPages },
                                                                                        color = if (showAllControlPages) p.cyanSoft else p.panel,
                                                                                        shape = RoundedCornerShape(8.dp),
                                                                                        border = BorderStroke(
                                                                                            0.8.dp,
                                                                                            if (showAllControlPages) p.cyan.copy(alpha = 0.55f) else p.panelBorder,
                                                                                        ),
                                                                                    ) {
                                                                                        Text(
                                                                                            text = if (showAllControlPages) "فقط مد فعلی" else "نمایش همه صفحات",
                                                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                                                            color = if (showAllControlPages) p.cyan else p.textSecondary,
                                                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                                                        )
                                                                                    }
                                                                                }
                                                                            }
                                                                        }

                                                                        effectivePages.forEachIndexed { pageIdx, page ->
                                                                            val displayNum = pageIdx + 1
                                                                            val isPageOpen = expandedPageNumber == page.number
                                                                            val pageChevronAngle by animateFloatAsState(
                                                                                targetValue = if (isPageOpen) 180f else 0f,
                                                                                animationSpec = tween(200),
                                                                                label = "page_chevron_${category.index}_${page.number}",
                                                                            )
                                                                            val hasEditableForUser = remember(page, userRole, operatorPermissions) {
                                                                                page.displayRegisterKeys.any { key ->
                                                                                    userRole.canWriteRegister(Bpc02RegisterMap.byKey(key), operatorPermissions)
                                                                                }
                                                                            }

                                                                            Column(
                                                                                modifier = Modifier
                                                                                    .fillMaxWidth()
                                                                                    .clip(RoundedCornerShape(12.dp))
                                                                                    .background(p.panelElevated)
                                                                                    .border(
                                                                                        width = 0.85.dp,
                                                                                        color = if (isPageOpen) p.cyan.copy(alpha = 0.55f) else p.panelBorder,
                                                                                        shape = RoundedCornerShape(12.dp),
                                                                                    ),
                                                                            ) {
                                                                                Row(
                                                                                    modifier = Modifier
                                                                                        .fillMaxWidth()
                                                                                        .clickable {
                                                                                            expandedPageNumber = if (isPageOpen) null else page.number
                                                                                        }
                                                                                        .padding(horizontal = 11.dp, vertical = 10.dp),
                                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                                                ) {
                                                                                    Box(
                                                                                        modifier = Modifier
                                                                                            .size(28.dp)
                                                                                            .clip(RoundedCornerShape(8.dp))
                                                                                            .background(if (isPageOpen) p.cyanSoft else p.panel)
                                                                                            .border(
                                                                                                0.8.dp,
                                                                                                if (isPageOpen) p.cyan.copy(alpha = 0.55f) else p.panelBorder,
                                                                                                RoundedCornerShape(8.dp),
                                                                                            ),
                                                                                        contentAlignment = Alignment.Center,
                                                                                    ) {
                                                                                        Text(
                                                                                            text = "$displayNum",
                                                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                                                            color = if (isPageOpen) p.cyan else p.textSecondary,
                                                                                        )
                                                                                    }

                                                                                    Column(
                                                                                        modifier = Modifier.weight(1f),
                                                                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                                                                    ) {
                                                                                        LtrText(
                                                                                            text = page.title,
                                                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                                                fontWeight = if (isPageOpen) FontWeight.Bold else FontWeight.SemiBold,
                                                                                            ),
                                                                                            color = if (isPageOpen) p.cyan else p.textPrimary,
                                                                                        )
                                                                                        Row(
                                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                                        ) {
                                                                                            LtrText(
                                                                                                text = if (hasEditableForUser) "Editable" else "Read-Only",
                                                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                                                color = if (hasEditableForUser) p.gold.copy(alpha = 0.88f) else p.textMuted,
                                                                                            )
                                                                                            if (page.controlMode == Bpc02Catalog.ControlPageMode.ON_OFF_ONLY) {
                                                                                                LtrText(
                                                                                                    text = "• ON/OFF",
                                                                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                                                                        fontSize = 10.sp,
                                                                                                        fontWeight = FontWeight.Bold,
                                                                                                    ),
                                                                                                    color = p.cyan,
                                                                                                )
                                                                                            } else if (page.controlMode == Bpc02Catalog.ControlPageMode.PID_ONLY) {
                                                                                                LtrText(
                                                                                                    text = "• PID",
                                                                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                                                                        fontSize = 10.sp,
                                                                                                        fontWeight = FontWeight.Bold,
                                                                                                    ),
                                                                                                    color = p.ok,
                                                                                                )
                                                                                            }
                                                                                        }
                                                                                    }

                                                                                    Icon(
                                                                                        imageVector = Icons.Outlined.ExpandMore,
                                                                                        contentDescription = if (isPageOpen) "Collapse" else "Expand",
                                                                                        tint = if (isPageOpen) p.cyan else p.textSecondary,
                                                                                        modifier = Modifier
                                                                                            .size(19.dp)
                                                                                            .rotate(pageChevronAngle),
                                                                                    )
                                                                                }

                                                                                AnimatedVisibility(
                                                                                    visible = isPageOpen,
                                                                                    enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                                                                                    exit = shrinkVertically(tween(180)) + fadeOut(tween(140)),
                                                                                ) {
                                                                                    Column(
                                                                                        modifier = Modifier
                                                                                            .fillMaxWidth()
                                                                                            .padding(start = 9.dp, end = 9.dp, bottom = 10.dp),
                                                                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                                                                    ) {
                                                                                        HorizontalDivider(color = p.hairline)

                                                                                        ControllerPageParameters(
                                                                                            page = page,
                                                                                            valueByKey = valueByKey,
                                                                                            liveControlStatus = liveControlStatus,
                                                                                            userRole = userRole,
                                                                                            operatorPermissions = operatorPermissions,
                                                                                            liveSnap = liveSnap,
                                                                                            writingKey = cfgSnap.writingKey,
                                                                                            isConnected = isConnected,
                                                                                            isWriting = isWriting,
                                                                                            hasDrivers = hasDrivers,
                                                                                            visiblePumpNumbers = visiblePumpNumbers,
                                                                                            onRequestPendingEdit = { pendingEdit = it },
                                                                                            onRequestHelp = { def, raw ->
                                                                                                val cur = raw?.let { Bpc02Catalog.formatValue(def, it) }
                                                                                                activeHelp = Bpc02Catalog.helpFor(def) to cur
                                                                                            },
                                                                                        )

                                                                                        // Sequential page stepper (< Prev | X / Y | Next >)
                                                                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                                                            Row(
                                                                                                modifier = Modifier
                                                                                                    .fillMaxWidth()
                                                                                                    .padding(top = 4.dp),
                                                                                                verticalAlignment = Alignment.CenterVertically,
                                                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                                            ) {
                                                                                                PageArrowButton(
                                                                                                    icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                                                                                    label = "Prev",
                                                                                                    iconBeforeLabel = true,
                                                                                                    enabled = pageIdx > 0,
                                                                                                    onClick = {
                                                                                                        if (pageIdx > 0) {
                                                                                                            expandedPageNumber = effectivePages[pageIdx - 1].number
                                                                                                        }
                                                                                                    },
                                                                                                )
                                                                                                Text(
                                                                                                    text = "$displayNum / ${effectivePages.size}",
                                                                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                                                    color = p.gold,
                                                                                                    textAlign = TextAlign.Center,
                                                                                                )
                                                                                                PageArrowButton(
                                                                                                    icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                                                                                    label = "Next",
                                                                                                    iconBeforeLabel = false,
                                                                                                    enabled = pageIdx < effectivePages.lastIndex,
                                                                                                    onClick = {
                                                                                                        if (pageIdx < effectivePages.lastIndex) {
                                                                                                            expandedPageNumber = effectivePages[pageIdx + 1].number
                                                                                                        }
                                                                                                    },
                                                                                                )
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
            }
        }

        // 3. Portable Configuration Backup (Import / Export JSON) — last option under Hardware Menu
        item {
            val loadedParamCount = valueByKey.count { (k, v) ->
                v != null && Bpc02RegisterMap.configuration.any { it.key == k }
            }
            val roleCanExport = effectivePermissions.canExportConfig
            val roleCanImport = effectivePermissions.canImportConfig
            val canExport = roleCanExport && loadedParamCount > 0 && !cfgSnap.loading
            val canImport = roleCanImport && isConnected && !isWriting

            ScadaPanel(
                title = "Configuration Import / Export",
                subtitle = "ذخیره، اشتراک‌گذاری و بازیابی تنظیمات کنترلر با فایل استاندارد JSON",
                icon = Icons.Outlined.UploadFile,
                accentColor = p.gold,
                trailing = {
                    ParameterHelpIcon(
                        onClick = {
                            activeHelp = Bpc02Catalog.conceptHelp("config_transfer") to
                                "${activeModel.label} • $loadedParamCount Params"
                        },
                    )
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DgpGoldButton(
                        text = "Export JSON",
                        icon = Icons.Outlined.Download,
                        enabled = canExport,
                        onClick = {
                            val now = System.currentTimeMillis()
                            val json = ControllerConfigTransfer.exportToJson(
                                model = activeModel,
                                configReadings = cfgSnap.readings,
                                liveSnapshot = liveSnap,
                                timestampMillis = now,
                            )
                            val modeTxt = Bpc02Catalog.wiringModeLabel(liveSnap?.wiringMode ?: valueByKey["wiring_mode"])
                            val pumpTxt = (liveSnap?.configuredPumpCount ?: valueByKey["number_of_pump"])?.let { "$it Pumps" } ?: "—"
                            preparedExport = PreparedExportPayload(
                                fileName = ControllerConfigTransfer.suggestedFileName(activeModel, now),
                                jsonContent = json,
                                parameterCount = loadedParamCount,
                                modelLabel = activeModel.label,
                                wiringSummary = "$modeTxt • $pumpTxt",
                            )
                        },
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                    )
                    DgpOutlinedButton(
                        text = "Import JSON",
                        icon = Icons.Outlined.FileOpen,
                        accentColor = p.cyan,
                        enabled = canImport,
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                    )
                }

                when {
                    !roleCanExport && !roleCanImport -> NoticeBox(
                        text = "خروجی گرفتن (Export) و وارد کردن فایل تنظیمات (Import) نیازمند مجوز مدیر سیستم است.",
                        tone = BadgeTone.INFO,
                    )
                    !roleCanImport -> NoticeBox(
                        text = "وارد کردن فایل تنظیمات (Import) نیازمند مجوز مدیر سیستم است؛ خروجی گرفتن (Export) پس از دریافت تنظیمات دستگاه فعال است.",
                        tone = BadgeTone.INFO,
                    )
                    !roleCanExport -> NoticeBox(
                        text = "خروجی گرفتن (Export) برای این حساب غیرفعال است؛ وارد کردن فایل تنظیمات (Import) با مجوزهای فعال مجاز است.",
                        tone = BadgeTone.INFO,
                    )
                    loadedParamCount == 0 -> NoticeBox(
                        text = "برای خروجی گرفتن از کل تنظیمات، ابتدا به کنترلر متصل شده و دکمه Refresh را بزنید.",
                        tone = BadgeTone.WARNING,
                    )
                    else -> Text(
                        text = "فایل خروجی شامل تمام $loadedParamCount پارامتر خوانده‌شده از ${activeModel.label} (به همراه نسخه فرمت و مدل دستگاه) است.",
                        style = MaterialTheme.typography.labelSmall,
                        color = p.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemMenuSectionBody(
    contrastRaw: Int?,
    onRequestHelp: (RegisterDefinition, Int?) -> Unit,
) {
    val p = LocalScadaPalette.current
    val contrastDef = remember { Bpc02RegisterMap.byKey("lcd_contrast") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.panel)
            .border(0.85.dp, p.panelBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        // Exact order from physical controller `photo_5` (1. System):
        // Password Setting, Blight, Contrast, Beep On Click, Beep On Error
        LcdOnlyMenuRow(title = "Password Setting", value = "Keypad Only")
        HorizontalDivider(color = p.hairline)
        LcdOnlyMenuRow(title = "Blight", value = "Keypad Only")
        HorizontalDivider(color = p.hairline)
        ReadOnlyParameterRow(
            reading = RegisterReading(contrastDef, contrastRaw),
            onClickHelp = { onRequestHelp(contrastDef, contrastRaw) },
        )
        HorizontalDivider(color = p.hairline)
        LcdOnlyMenuRow(title = "Beep On Click", value = "Keypad Only")
        HorizontalDivider(color = p.hairline)
        LcdOnlyMenuRow(title = "Beep On Error", value = "Keypad Only")
    }
    Text(
        text = "مقدار Contrast به‌صورت زنده از کنترلر خوانده می‌شود؛ سایر گزینه‌های بخش System صرفاً روی نمایشگر خود دستگاه تنظیم می‌شوند.",
        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
        color = p.textSecondary,
    )
}

@Composable
private fun InputMenuSectionBody(
    snapshot: ControllerSnapshot?,
    onRequestDriveInputHelp: () -> Unit,
) {
    val p = LocalScadaPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.panel)
            .border(0.85.dp, p.panelBorder, RoundedCornerShape(12.dp))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Exact physical `Inputs` LCD layout (`photo_11`): Drive | PR1..4 | MFI1..4
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LtrText(
                    text = "Inputs (Live Mirror)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = p.cyan,
                )
                ParameterHelpIcon(onClick = onRequestDriveInputHelp)
            }
            StatusBadge(
                text = if (snapshot?.drStatus == true) "Drive: ON" else if (snapshot?.drStatus == false) "Drive: OFF" else "Drive: —",
                tone = if (snapshot?.drStatus == true) BadgeTone.OK else BadgeTone.NEUTRAL,
            )
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (rowIdx in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Col 1: Drive (row 0 only, matching photo_11)
                        if (rowIdx == 0) {
                            MiniLcdInputPill(
                                label = "Drive",
                                active = snapshot?.drStatus,
                                activeColor = p.ok,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        // Col 2: PR1..PR4
                        MiniLcdInputPill(
                            label = "PR${rowIdx + 1}",
                            active = snapshot?.prInputs?.getOrNull(rowIdx),
                            activeColor = p.cyan,
                            modifier = Modifier.weight(1f),
                        )
                        // Col 3: MFI1..MFI4
                        MiniLcdInputPill(
                            label = "MFI${rowIdx + 1}",
                            active = snapshot?.mfiInputs?.getOrNull(rowIdx),
                            activeColor = p.gold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniLcdInputPill(
    label: String,
    active: Boolean?,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    val p = LocalScadaPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (active == true) activeColor.copy(alpha = 0.14f) else p.panelElevated)
            .border(0.8.dp, if (active == true) activeColor.copy(alpha = 0.52f) else p.panelBorder, RoundedCornerShape(9.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PlcLedBead(active = active, size = 8.dp, activeColor = activeColor)
        Text(
            text = label,
            style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            color = if (active == true) activeColor else p.textPrimary,
        )
    }
}

@Composable
private fun TimeDateMenuSectionBody(snapshot: ControllerSnapshot?) {
    val p = LocalScadaPalette.current
    val batteryFault = snapshot?.activeSystemErrors2?.contains(SystemErrorBit2.BATTERY_VOLTAGE) == true
    val hasWord2 = snapshot?.systemErrorWord2 != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.panel)
            .border(0.85.dp, p.panelBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LtrText("VBat (Backup Battery)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = p.textPrimary)
            StatusBadge(
                text = when {
                    !hasWord2 -> "Unknown"
                    batteryFault -> "Low Voltage!"
                    else -> "Normal"
                },
                tone = when {
                    !hasWord2 -> BadgeTone.NEUTRAL
                    batteryFault -> BadgeTone.FAULT
                    else -> BadgeTone.OK
                },
                pulsing = batteryFault,
            )
        }
        HorizontalDivider(color = p.hairline)
        LcdOnlyMenuRow(title = "Time", value = "LCD Local")
        HorizontalDivider(color = p.hairline)
        LcdOnlyMenuRow(title = "Date", value = "LCD Local")
    }
    Text(
        text = "ساعت و تاریخ داخلی کنترلر (Time/Date) در نقشه ModBus ارائه نشده و مستقیماً از نمایشگر دستگاه تنظیم می‌شود؛ سلامت باتری پشتیبان (VBat) به‌صورت خودکار پایش می‌گردد.",
        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
        color = p.textSecondary,
    )
}

@Composable
private fun ResetFactorySectionBody(onRequestHelp: () -> Unit) {
    val p = LocalScadaPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.warningBg)
            .border(0.85.dp, p.warning.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = p.warning, modifier = Modifier.size(16.dp))
                LtrText(
                    text = "Reset to Factory — Keypad Only",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = p.warning,
                )
            }
            ParameterHelpIcon(onClick = onRequestHelp)
        }
        Text(
            text = "به دلایل ایمنی، دستور بازگشت به تنظیمات کارخانه از طریق ModBus غیرفعال است و تنها به‌صورت حضوری از صفحه‌کلید روی کنترلر (Menu › 4. Hardware › 8. Reset to Factory) قابل انجام است.",
            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 17.sp),
            color = p.textPrimary,
        )
    }
}

@Composable
private fun LcdOnlyMenuRow(title: String, value: String) {
    val p = LocalScadaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LtrText(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = p.textPrimary,
        )
        LtrText(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = p.textMuted,
        )
    }
}

@Composable
private fun ControllerPageParameters(
    page: Bpc02Catalog.MenuPage,
    valueByKey: Map<String, Int?>,
    liveControlStatus: Int?,
    userRole: UserRole,
    operatorPermissions: OperatorPermissions,
    liveSnap: ControllerSnapshot?,
    writingKey: String?,
    isConnected: Boolean,
    isWriting: Boolean,
    hasDrivers: Boolean,
    visiblePumpNumbers: List<Int>,
    onRequestPendingEdit: (PendingEdit) -> Unit,
    onRequestHelp: (RegisterDefinition, Int?) -> Unit,
) {
    val p = LocalScadaPalette.current
    val keysToShow = page.displayRegisterKeys.filter { key ->
        if (key == Bpc02RegisterMap.DRIVE_ACTIVATION) {
            hasDrivers
        } else if (key.startsWith("pump_") && key.endsWith("_activation")) {
            val num = key.removePrefix("pump_").removeSuffix("_activation").toIntOrNull()
            num == null || num in visiblePumpNumbers
        } else {
            true
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        keysToShow.forEachIndexed { idx, key ->
            if (idx > 0) HorizontalDivider(color = p.hairline)
            val def = Bpc02RegisterMap.byKey(key)
            val raw = valueByKey[key]
            val reading = RegisterReading(def, raw)
            val roleCanWrite = userRole.canWriteRegister(def, operatorPermissions)
            val canEditParam = roleCanWrite && isConnected && !isWriting
            val isAdminRestricted = def.isWritable && !roleCanWrite

            // Contextual exact LCD label: on `Pump 1` page Word 4096 is shown as "On Pressure" (`photo_2`, `photo_7`)
            val labelOverride = when {
                def.key == Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT && page.title == "Pump 1" -> "On Pressure"
                def.key == Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT && liveControlStatus == 2 -> "On Pressure (P1)"
                else -> null
            }

            when {
                def.table == VendorTable.BIT && def.isWritable -> {
                    val isPump = key.startsWith("pump_") && key.endsWith("_activation")
                    val pNum = if (isPump) key.removePrefix("pump_").removeSuffix("_activation").toIntOrNull() else null
                    val pState = pNum?.let { n -> liveSnap?.pumps?.firstOrNull { it.number == n } }
                    PumpServiceRow(
                        title = Bpc02Catalog.lcdParameterName(def),
                        subtitle = if (raw == 0) "Disable (S)" else if (raw == 1) "Enable" else "—",
                        isOn = raw?.let { it == 1 },
                        running = if (raw == 0) false else pState?.running,
                        failed = pState?.failed,
                        showPumpIcon = isPump,
                        enabled = canEditParam,
                        isRestricted = isAdminRestricted,
                        isWritingThis = writingKey == key,
                        onRequestToggle = { targetOn ->
                            onRequestPendingEdit(PendingEdit.SwitchConfirm(def, raw, if (targetOn) 1 else 0))
                        },
                        onClickHelp = { onRequestHelp(def, raw) },
                    )
                }
                // Inline radio-option selector matching `photo_4` (1. Status : Disable / PID / ON/OFF)
                def.key == Bpc02RegisterMap.CONTROL_STATUS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                LtrText(
                                    text = "1. Status",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = p.textPrimary,
                                )
                                ParameterHelpIcon(onClick = { onRequestHelp(def, raw) })
                            }
                            if (writingKey == key) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = p.gold)
                            } else {
                                StatusBadge(
                                    text = Bpc02Catalog.controlStatusLabel(raw),
                                    tone = when (raw) {
                                        1, 2 -> BadgeTone.OK
                                        0 -> BadgeTone.WARNING
                                        else -> BadgeTone.NEUTRAL
                                    },
                                )
                            }
                        }
                        DgpSegmentedControl(
                            items = listOf(0, 1, 2),
                            selected = raw ?: -1,
                            label = { mode -> Bpc02Catalog.controlStatusLabel(mode) },
                            onSelect = { target ->
                                if (raw != target) {
                                    onRequestPendingEdit(PendingEdit.ControlModeConfirm(raw, target))
                                }
                            },
                            enabled = canEditParam && raw != null,
                            height = 38.dp,
                        )
                    }
                }
                def.isWritable -> {
                    val isEnum = def.enumLabels.isNotEmpty()
                    ParameterRow(
                        reading = reading,
                        actionLabel = if (isEnum) "Select" else "Edit",
                        canEdit = canEditParam,
                        isAdminRestricted = isAdminRestricted,
                        isWritingThis = writingKey == key,
                        onClickEdit = {
                            onRequestPendingEdit(
                                if (isEnum) {
                                    PendingEdit.EnumEdit(def, raw)
                                } else {
                                    PendingEdit.NumberEdit(def, raw)
                                },
                            )
                        },
                        onClickHelp = { onRequestHelp(def, raw) },
                        labelOverride = labelOverride,
                    )
                }
                else -> {
                    ReadOnlyParameterRow(
                        reading = reading,
                        onClickHelp = { onRequestHelp(def, raw) },
                        labelOverride = labelOverride,
                    )
                }
            }
        }

        // Physical `PID` screen live monitor footer mirroring `photo_3` (Slp Fq, Counter, Pressure Bar)
        if (page.title == "PID") {
            val slpFqRaw = valueByKey["sleep_frequency"]
            val counterRaw = valueByKey["sleep_off_delay"]
            val slpFqTxt = slpFqRaw?.let { Bpc02Catalog.formatValue(Bpc02RegisterMap.byKey("sleep_frequency"), it) } ?: "—"
            val counterTxt = counterRaw?.let { Bpc02Catalog.formatValue(Bpc02RegisterMap.byKey("sleep_off_delay"), it) } ?: "—"
            val pressureTxt = liveSnap?.pressureBar?.let { "%.2f Bar".format(Locale.US, it) } ?: "— Bar"
            HorizontalDivider(color = p.hairline, modifier = Modifier.padding(vertical = 4.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(p.panel)
                        .border(0.8.dp, p.cyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Slp Fq", style = MaterialTheme.typography.labelSmall, color = p.textMuted)
                        Text(slpFqTxt, style = MonoStyle.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold), color = p.cyan)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Counter", style = MaterialTheme.typography.labelSmall, color = p.textMuted)
                        Text(counterTxt, style = MonoStyle.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold), color = p.gold)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Pressure", style = MaterialTheme.typography.labelSmall, color = p.textMuted)
                        Text(pressureTxt, style = MonoStyle.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold), color = p.ok)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportConfigDialog(
    payload: PreparedExportPayload,
    onSaveFile: () -> Unit,
    onShareFile: () -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalScadaPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.Download, contentDescription = null, tint = p.gold) },
        title = { LtrText("Export Configuration (JSON)", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = p.panelElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.8.dp, p.panelBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        LtrText(payload.fileName, style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = p.gold)
                        LtrText("Model: ${payload.modelLabel}  •  Schema v${ControllerConfigTransfer.SCHEMA_VERSION}", style = MaterialTheme.typography.labelSmall, color = p.cyan)
                        LtrText("Station: ${payload.wiringSummary}  •  ${payload.parameterCount} Parameters", style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
                    }
                }
                Text(
                    "می‌توانید فایل JSON تنظیمات را روی حافظه دستگاه ذخیره کرده یا مستقیماً برای استفاده در پروژه‌های دیگر به اشتراک بگذارید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textSecondary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DgpGoldButton(
                        text = "Save to Device (ذخیره فایل)",
                        icon = Icons.Outlined.Save,
                        onClick = onSaveFile,
                        height = 44.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DgpOutlinedButton(
                        text = "Share File (اشتراک‌گذاری)",
                        icon = Icons.Outlined.Share,
                        accentColor = p.cyan,
                        onClick = onShareFile,
                        height = 44.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بستن", color = p.textSecondary) }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImportConfigPreviewDialog(
    result: ConfigImportValidationResult,
    isConnected: Boolean,
    isWriting: Boolean,
    targetModel: ControllerModel,
    onConfirmApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val hasErrors = result.errors.isNotEmpty()
    val hasWarnings = result.warnings.isNotEmpty()
    val changes = result.changesToApply
    val canApplyNow = result.canApply && isConnected && !isWriting

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Icon(
                imageVector = if (hasErrors) Icons.Outlined.ErrorOutline else Icons.Outlined.UploadFile,
                contentDescription = null,
                tint = if (hasErrors) p.fault else p.gold,
            )
        },
        title = {
            LtrText(
                if (hasErrors) "Import Validation Failed" else "Import Configuration Preview",
                style = MaterialTheme.typography.titleMedium,
                color = if (hasErrors) p.fault else p.textPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // File Metadata Summary
                result.metadata?.let { meta ->
                    Surface(
                        color = p.panelElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, p.panelBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LtrText(
                                "File Model: ${meta.controllerModelName}  •  Target: ${targetModel.name}  •  v${meta.schemaVersion}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (meta.controllerModelName.equals(targetModel.name, ignoreCase = true)) p.cyan else p.fault,
                            )
                            val archParts = buildList {
                                meta.wiringMode?.let { add(Bpc02Catalog.wiringModeLabel(it)) }
                                meta.numberOfPumps?.let { add("$it Pumps") }
                                meta.numberOfDrives?.let { add(Bpc02Catalog.driverCountLabel(it)) }
                            }
                            if (archParts.isNotEmpty()) {
                                LtrText(archParts.joinToString(" • "), style = MaterialTheme.typography.labelSmall, color = p.gold)
                            }
                            meta.exportedAt?.let {
                                LtrText("Exported: $it", style = MonoStyle.copy(fontSize = 10.sp), color = p.textSecondary)
                            }
                        }
                    }
                }

                // Summary Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatusBadge("${changes.size} Changes", if (changes.isNotEmpty()) BadgeTone.GOLD else BadgeTone.NEUTRAL)
                    if (result.unchangedWritableCount > 0) {
                        StatusBadge("${result.unchangedWritableCount} Same", BadgeTone.INFO)
                    }
                    if (result.readOnlyEntries.isNotEmpty()) {
                        StatusBadge("${result.readOnlyEntries.size} Read-Only", BadgeTone.NEUTRAL)
                    }
                    if (hasErrors) {
                        StatusBadge("${result.errors.size} Error(s)", BadgeTone.FAULT)
                    }
                }

                // Blocking Validation Errors (Strict — zero partial writes allowed!)
                if (hasErrors) {
                    NoticeBox(
                        text = "به دلیل وجود خطاهای اعتبارسنجی زیر، اعمال فایل متوقف شد و هیچ تغییری روی کنترلر انجام نمی‌شود:",
                        tone = BadgeTone.FAULT,
                    )
                    result.errors.forEach { err ->
                        Surface(
                            color = p.faultBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.7.dp, p.fault.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                LtrText(err.title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = p.fault)
                                Text(err.detailFa, style = MaterialTheme.typography.labelSmall, color = p.fault)
                            }
                        }
                    }
                }

                // Hardware Compatibility Warnings (e.g. Mode or Pump Count differs on Read-Only registers)
                if (hasWarnings) {
                    result.warnings.forEach { warn ->
                        Surface(
                            color = p.warningBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.7.dp, p.warning.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = p.warning, modifier = Modifier.size(14.dp))
                                    LtrText(warn.title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = p.warning)
                                }
                                Text(warn.detailFa, style = MaterialTheme.typography.labelSmall, color = p.warning)
                            }
                        }
                    }
                }

                // Diff Preview of Writable Changes to Apply
                if (!hasErrors) {
                    if (changes.isEmpty()) {
                        NoticeBox(
                            text = "تمام پارامترهای قابل ویرایش در فایل با مقادیر فعلی کنترلر برابر هستند و نیازی به بازنویسی نیست.",
                            tone = BadgeTone.OK,
                        )
                    } else {
                        Text(
                            "تغییراتی که پس از تایید روی کنترلر اعمال می‌شوند:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = p.textPrimary,
                        )
                        Surface(
                            color = p.panelElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.8.dp, p.panelBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                changes.forEachIndexed { idx, ch ->
                                    if (idx > 0) HorizontalDivider(color = p.hairline)
                                    val oldTxt = ch.currentValue?.let { Bpc02Catalog.formatValue(ch.definition, it) } ?: "—"
                                    val newTxt = Bpc02Catalog.formatValue(ch.definition, ch.targetValue)
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        LtrText(
                                            text = Bpc02Catalog.fullParameterName(ch.definition),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = p.textPrimary,
                                        )
                                        LtrText(
                                            text = "$oldTxt  →  $newTxt",
                                            style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                            color = p.gold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (result.readOnlyEntries.isNotEmpty()) {
                        Text(
                            "${result.readOnlyEntries.size} پارامتر فقط‌خواندنی (مانند Wiring Mode، Main، Sensor Type و MFx) صرفاً جهت مقایسه نگهداری شده و بازنویسی نمی‌شوند.",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.textMuted,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!hasErrors && changes.isNotEmpty()) {
                Button(
                    onClick = onConfirmApply,
                    enabled = canApplyNow,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DialogGoldBg, contentColor = DialogGoldInk),
                ) {
                    Text("تایید و اعمال (${changes.size})", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = p.textSecondary) }
        },
    )
}

@Composable
private fun ImportProgressBanner(
    progress: ConfigImportProgress,
    onDismiss: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val toneColor = when {
        progress.applying -> p.gold
        progress.allSucceeded -> p.ok
        progress.appliedCount > 0 -> p.warning
        else -> p.fault
    }
    val bgColor = when {
        progress.applying -> p.goldSoft
        progress.allSucceeded -> p.okBg
        progress.appliedCount > 0 -> p.warningBg
        else -> p.faultBg
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.9.dp, toneColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
        color = bgColor,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (progress.applying) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = p.gold)
                    } else {
                        Icon(
                            imageVector = if (progress.allSucceeded) Icons.Outlined.CheckCircleOutline else Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = toneColor,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = when {
                                progress.applying -> "در حال اعمال تنظیمات (${progress.processedCount} / ${progress.totalCount})…"
                                progress.allSucceeded -> "اعمال فایل تنظیمات با موفقیت کامل انجام شد (${progress.appliedCount} پارامتر)"
                                else -> "اعمال تنظیمات به پایان رسید (${progress.appliedCount} از ${progress.totalCount} موفق)"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = toneColor,
                        )
                        progress.currentParameterName?.let { activeName ->
                            LtrText("Writing: $activeName", style = MaterialTheme.typography.labelSmall, color = toneColor)
                        }
                    }
                }
                if (!progress.applying) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "بستن", tint = toneColor, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (progress.applying && progress.totalCount > 0) {
                LinearProgressIndicator(
                    progress = { (progress.processedCount.toFloat() / progress.totalCount.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = p.gold,
                    trackColor = p.panel,
                )
            }

            if (progress.failedDetails.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    progress.failedDetails.take(4).forEach { fail ->
                        LtrText("• $fail", style = MaterialTheme.typography.labelSmall, color = p.fault)
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
    val label = Bpc02Catalog.fullParameterName(def)

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
                        "تغییر $label با موفقیت ثبت شد"
                    } else {
                        "ثبت $label انجام نشد"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                )
                Text(
                    text = if (result.success) {
                        val confirmed = result.readBackValue?.let { Bpc02Catalog.formatValue(def, it) } ?: "—"
                        "New Value: $confirmed  •  ${formatTime(result.timestampMillis)}"
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
    isRestricted: Boolean = false,
    isWritingThis: Boolean,
    onRequestToggle: (Boolean) -> Unit,
    onClickHelp: () -> Unit,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    LtrText(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = p.textPrimary,
                    )
                    ParameterHelpIcon(onClick = onClickHelp)
                }
                LtrText(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        failed == true -> p.fault
                        isDisabled -> p.warning
                        running == true -> p.ok
                        else -> p.textSecondary
                    },
                )
                if (isRestricted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = p.textMuted,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = "فقط خواندنی",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = p.textMuted,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        if (isWritingThis) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = p.gold)
        } else {
            PumpPowerToggle(
                isOn = isOn,
                enabled = enabled,
                onToggle = onRequestToggle,
                onLabel = "ON",
                offLabel = "OFF",
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
    onClickHelp: () -> Unit,
    labelOverride: String? = null,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                LtrText(
                    text = labelOverride ?: Bpc02Catalog.lcdParameterName(def),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = p.textPrimary,
                )
                ParameterHelpIcon(onClick = onClickHelp)
            }
            LtrText(
                text = reading.raw?.let { Bpc02Catalog.formatValue(def, it) } ?: "—",
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
                    LtrText("Admin", style = MaterialTheme.typography.labelSmall, color = p.textMuted)
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
                        LtrText(
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
private fun ReadOnlyParameterRow(
    reading: RegisterReading,
    onClickHelp: () -> Unit,
    labelOverride: String? = null,
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
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                LtrText(
                    text = labelOverride ?: Bpc02Catalog.lcdParameterName(def),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = p.textPrimary,
                )
                ParameterHelpIcon(onClick = onClickHelp)
            }
            LtrText(
                text = "Read-Only",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = p.textMuted,
            )
        }
        Spacer(Modifier.width(8.dp))
        LtrText(
            text = reading.raw?.let { Bpc02Catalog.formatValue(def, it) } ?: "—",
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
    val currentText = Bpc02Catalog.controlStatusLabel(currentValue)
    val targetText = Bpc02Catalog.controlStatusLabel(targetValue)
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
        title = { LtrText("Control Status", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Surface(
                    color = p.panelElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, p.panelBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LtrText("Current: $currentText", style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                        LtrText(
                            "New: $targetText",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDisabling) p.fault else p.gold,
                        )
                    }
                }
                if (isDisabling) {
                    NoticeBox(
                        text = "توجه: انتخاب حالت Disable باعث توقف کامل ایستگاه بوستر پمپ و قطع تامین فشار می‌شود.",
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
        title = { LtrText("Reset Pump Error", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
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
    val currentText = currentValue?.let { if (it == 1) "Enable" else "Disable (S)" } ?: "—"
    val targetText = if (targetValue == 1) "Enable" else "Disable (S)"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null, tint = p.gold) },
        title = { LtrText(Bpc02Catalog.fullParameterName(def), style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
                        LtrText("Current: $currentText", style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                        LtrText(
                            "New: $targetText",
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
    val rangeSummary = "Range: ${Bpc02Catalog.formatValue(def, min)} – ${Bpc02Catalog.formatValue(def, max)}"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = p.gold) },
        title = { LtrText(Bpc02Catalog.fullParameterName(def), style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                currentValue?.let { cur ->
                    LtrText(
                        "Current: ${Bpc02Catalog.formatValue(def, cur)}",
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
                    label = { Text("New Value") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = text.isNotEmpty() && !inRange,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (usesDecimalInput) KeyboardType.Decimal else KeyboardType.Number,
                    ),
                    supportingText = {
                        when {
                            text.isEmpty() -> LtrText(rangeSummary)
                            !inRange -> LtrText("Out of range ($rangeSummary)", color = p.fault)
                            else -> LtrText(
                                "New: ${Bpc02Catalog.formatValue(def, parsedRaw!!)}",
                                color = p.ok,
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
        title = { LtrText(Bpc02Catalog.fullParameterName(def), style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
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
                        LtrText(
                            text = Bpc02Catalog.enumOptionLabel(def, code),
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

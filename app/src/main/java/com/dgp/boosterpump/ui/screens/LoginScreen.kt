package com.dgp.boosterpump.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.auth.AppUser
import com.dgp.boosterpump.auth.AuthRepository
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.LtrText
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.theme.MonoStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val LoginObsidian = Color(0xFF03050A)
private val LoginCardBg = Color(0xFF0B101C)
private val LoginFieldBg = Color(0xFF101829)
private val LoginBorder = Color(0xFF22314D)
private val LoginGold = Color(0xFFE8B84B)
private val LoginGoldBright = Color(0xFFFCE08B)
private val LoginCyan = Color(0xFF22D3EE)

/**
 * Luxury obsidian-black Persian RTL login screen featuring the dark DGP logo prominently
 * framed at the top with subtle animated orbital rings, carbon-glass login card, shake-on-error
 * feedback, and role-based offline authentication.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    onAuthenticated: (AppUser) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val entranceAlpha = remember { Animatable(0f) }
    val shakeOffset = remember { Animatable(0f) }

    val infinite = rememberInfiniteTransition(label = "login_ambient")
    val outerOrbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing)),
        label = "login_outer_orbit",
    )
    val innerOrbit by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(7_200, easing = LinearEasing)),
        label = "login_inner_orbit",
    )
    val particlePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing)),
        label = "login_particles",
    )
    val breathGlow by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(2_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "login_breath",
    )
    val shimmerSweep by infinite.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(3_400, easing = FastOutSlowInEasing)),
        label = "login_shimmer",
    )

    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
    }

    fun triggerErrorShake(message: String) {
        errorMessage = message
        scope.launch {
            val sequence = listOf(-14f, 14f, -10f, 10f, -5f, 5f, 0f)
            for (target in sequence) {
                shakeOffset.animateTo(target, tween(45))
            }
        }
    }

    fun submitLogin() {
        if (isSubmitting) return
        focusManager.clearFocus()
        val u = username.trim()
        if (u.isEmpty() || password.isEmpty()) {
            triggerErrorShake("لطفاً نام کاربری و رمز عبور را وارد کنید.")
            return
        }
        isSubmitting = true
        errorMessage = null
        scope.launch {
            delay(320)
            val user = AuthRepository.authenticate(u, password)
            if (user != null) {
                onAuthenticated(user)
            } else {
                isSubmitting = false
                triggerErrorShake("نام کاربری یا رمز عبور نادرست است. دسترسی رد شد.")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginObsidian),
    ) {
        ObsidianScadaBackdrop(
            particlePhase = particlePhase,
            breathGlow = breathGlow,
            spotlightYFraction = 0.21f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp)
                .alpha(entranceAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // Prominent dark obsidian DGP logo on top with animated gold & cyan rings
            DgpAnimatedCrest(
                size = 186.dp,
                logoSize = 136.dp,
                outerAngle = outerOrbit,
                innerAngle = innerOrbit,
                sonarPhase = 0f,
                breathGlow = breathGlow,
                shimmerSweep = shimmerSweep,
                showSonar = false,
            )

            // Brand Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "ورود به پنل کنترل بوستر پمپ",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                    color = Color(0xFFF8FAFC),
                    textAlign = TextAlign.Center,
                )
                LtrText(
                    text = "DIGITAL GOSTAR PARSIAN  •  BPC02 SCADA",
                    style = MonoStyle.copy(fontSize = 10.5.sp, letterSpacing = 0.8.sp),
                    color = LoginGold.copy(alpha = 0.90f),
                )
            }

            // Luxury Carbon-Glass Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LoginCardBg.copy(alpha = 0.95f)),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            LoginGold.copy(alpha = 0.48f),
                            LoginBorder,
                            LoginCyan.copy(alpha = 0.35f),
                        ),
                    ),
                ),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Metallic gold-to-cyan top accent stripe
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(LoginGold, LoginGoldBright, LoginCyan),
                                ),
                            ),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = null,
                                tint = LoginGold,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "احراز هویت اپراتور و نصاب",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFF1F5F9),
                            )
                        }

                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoginGold,
                            unfocusedBorderColor = LoginBorder,
                            focusedLabelColor = LoginGold,
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedTextColor = Color(0xFFF8FAFC),
                            unfocusedTextColor = Color(0xFFF1F5F9),
                            cursorColor = LoginGold,
                            focusedContainerColor = LoginFieldBg,
                            unfocusedContainerColor = LoginFieldBg.copy(alpha = 0.78f),
                            focusedLeadingIconColor = LoginGold,
                            unfocusedLeadingIconColor = Color(0xFF94A3B8),
                        )

                        // Username input (LTR text entry inside RTL Persian label)
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text("نام کاربری") },
                            placeholder = {
                                LtrText(
                                    "admin / nasab / service / operator",
                                    style = MonoStyle.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF64748B),
                                )
                            },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                            singleLine = true,
                            enabled = !isSubmitting,
                            colors = fieldColors,
                            shape = RoundedCornerShape(13.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Password input with visibility toggle
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text("رمز عبور") },
                            placeholder = { Text("رمز عبور اختصاصی را وارد کنید", color = Color(0xFF64748B)) },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "مخفی کردن رمز" else "نمایش رمز",
                                        tint = Color(0xFF94A3B8),
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            singleLine = true,
                            enabled = !isSubmitting,
                            colors = fieldColors,
                            shape = RoundedCornerShape(13.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { submitLogin() }),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut(),
                        ) {
                            errorMessage?.let { msg ->
                                NoticeBox(text = msg, tone = BadgeTone.FAULT)
                            }
                        }

                        // Primary Gold-to-Cyan Gradient Sign-in Button
                        Button(
                            onClick = ::submitLogin,
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(LoginGold, Color(0xFFDDA53A), LoginCyan),
                                    ),
                                ),
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color(0xFF04070E),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "در حال بررسی دسترسی…",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF04070E),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Login,
                                    contentDescription = null,
                                    tint = Color(0xFF04070E),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "ورود به سامانه SCADA",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF04070E),
                                )
                            }
                        }
                    }
                }
            }

            // Supported role level pills (informational, no passwords shown)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "سطوح دسترسی تعریف‌شده در سامانه:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RolePreviewPill("مدیر سیستم", LoginGold)
                    RolePreviewPill("نصاب و راه‌انداز", LoginCyan)
                    RolePreviewPill("تکنسین سرویس", Color(0xFF34D399))
                    RolePreviewPill("اپراتور", Color(0xFF94A3B8))
                    RolePreviewPill("مهمان (دمو)", Color(0xFF94A3B8))
                }
            }

            Spacer(Modifier.height(6.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = "DGP BPC02 • Samin ES 487 Gateway • Offline Secure Auth",
                    style = MonoStyle.copy(fontSize = 10.sp),
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RolePreviewPill(label: String, accent: Color) {
    Surface(
        color = Color(0xFF0D1424),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(999.dp)),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

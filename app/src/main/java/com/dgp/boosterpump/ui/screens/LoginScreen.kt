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
import com.dgp.boosterpump.ui.components.DgpGoldButton
import com.dgp.boosterpump.ui.components.LtrText
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.theme.MonoStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val AuthBg = Color(0xFF06080D)
private val AuthCard = Color(0xFF0E121B)
private val AuthField = Color(0xFF141A26)
private val AuthBorder = Color(0x1CFFFFFF)
private val AuthHighlight = Color(0x2EFFFFFF)
private val AuthGold = Color(0xFFE2B85E)
private val AuthGoldBright = Color(0xFFF5D68A)

/**
 * Refined flagship obsidian & brushed champagne-gold Persian login screen.
 * Showcases the beveled DGP medallion prominently at the top above a glass-bevel login card.
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
    val cardOffsetY = remember { Animatable(24f) }
    val shakeOffset = remember { Animatable(0f) }

    val infinite = rememberInfiniteTransition(label = "login_luxury")
    val outerOrbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing)),
        label = "login_bezel",
    )
    val tracerOrbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "login_tracer",
    )
    val breath by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "login_breath",
    )
    val shimmer by infinite.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(tween(4_200, easing = FastOutSlowInEasing)),
        label = "login_shimmer",
    )

    LaunchedEffect(Unit) {
        launch { entranceAlpha.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
        launch { cardOffsetY.animateTo(0f, tween(560, easing = FastOutSlowInEasing)) }
    }

    fun triggerErrorShake(message: String) {
        errorMessage = message
        scope.launch {
            val sequence = listOf(-12f, 12f, -8f, 8f, -4f, 4f, 0f)
            for (target in sequence) {
                shakeOffset.animateTo(target, tween(42))
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
                triggerErrorShake("نام کاربری یا رمز عبور نادرست است. دسترسی تایید نشد.")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthBg),
    ) {
        ObsidianBrandBackdrop(
            breathGlow = breath,
            spotlightYFraction = 0.19f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 22.dp)
                .alpha(entranceAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.height(6.dp))

            // Prominent refined brushed-gold DGP crest at the top
            DgpAnimatedCrest(
                size = 184.dp,
                logoSize = 152.dp,
                outerAngle = outerOrbit,
                innerAngle = tracerOrbit,
                breathGlow = breath,
                shimmerSweep = shimmer,
                activeTracer = true,
            )

            // Editorial Persian & brand heading
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "ورود به پنل کنترل بوستر پمپ",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color(0xFFF4F6FA),
                    textAlign = TextAlign.Center,
                )
                LtrText(
                    text = "DIGITAL GOSTAR PARSIAN  •  BPC02",
                    style = MonoStyle.copy(fontSize = 10.sp, letterSpacing = 1.1.sp),
                    color = AuthGold.copy(alpha = 0.88f),
                )
            }

            // Glass-Bevel Obsidian Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), cardOffsetY.value.roundToInt()) },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = AuthCard.copy(alpha = 0.96f)),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            AuthGold.copy(alpha = 0.36f),
                            AuthHighlight,
                            AuthBorder,
                        ),
                    ),
                ),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Subtle champagne-gold top hairline
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        AuthGold.copy(alpha = 0.65f),
                                        AuthGoldBright.copy(alpha = 0.85f),
                                        AuthGold.copy(alpha = 0.65f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(15.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF241D10), RoundedCornerShape(9.dp))
                                    .border(0.8.dp, AuthGold.copy(alpha = 0.32f), RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VerifiedUser,
                                    contentDescription = null,
                                    tint = AuthGold,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "ورود به حساب کاربری",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFF4F6FA),
                                )
                                Text(
                                    text = "ورود امن و آفلاین بدون نیاز به اینترنت",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF8390A6),
                                )
                            }
                        }

                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuthGold,
                            unfocusedBorderColor = AuthBorder,
                            focusedLabelColor = AuthGold,
                            unfocusedLabelColor = Color(0xFF8B98AE),
                            focusedTextColor = Color(0xFFF4F6FA),
                            unfocusedTextColor = Color(0xFFE8EDF5),
                            cursorColor = AuthGold,
                            focusedContainerColor = AuthField,
                            unfocusedContainerColor = AuthField.copy(alpha = 0.72f),
                            focusedLeadingIconColor = AuthGold,
                            unfocusedLeadingIconColor = Color(0xFF7D8A9F),
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text("نام کاربری") },
                            placeholder = {
                                LtrText(
                                    "admin  /  operator",
                                    style = MonoStyle.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF5A657A),
                                )
                            },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(19.dp)) },
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

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text("رمز عبور") },
                            placeholder = { Text("رمز عبور را وارد کنید", color = Color(0xFF5A657A)) },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(19.dp)) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "مخفی کردن رمز" else "نمایش رمز",
                                        tint = Color(0xFF8B98AE),
                                        modifier = Modifier.size(19.dp),
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
                            enter = fadeIn(tween(220)) + slideInVertically { -it / 3 },
                            exit = fadeOut(tween(180)),
                        ) {
                            errorMessage?.let { msg ->
                                NoticeBox(text = msg, tone = BadgeTone.FAULT)
                            }
                        }

                        if (isSubmitting) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .background(Color(0xFF241D10), RoundedCornerShape(13.dp))
                                    .border(0.9.dp, AuthGold.copy(alpha = 0.45f), RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(19.dp),
                                        strokeWidth = 2.2.dp,
                                        color = AuthGold,
                                    )
                                    Text(
                                        text = "در حال تایید دسترسی…",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = AuthGold,
                                    )
                                }
                            }
                        } else {
                            DgpGoldButton(
                                text = "ورود به سامانه",
                                icon = Icons.AutoMirrored.Outlined.Login,
                                onClick = ::submitLogin,
                                height = 50.dp,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            // Two-tier access hierarchy pills (no credentials displayed on screen)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "سطوح دسترسی سامانه",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF5D687D),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RoleBadgeChip("مدیر سیستم (دسترسی کامل)", AuthGold)
                    RoleBadgeChip("اپراتور (فقط خواندنی / قابل تنظیم)", Color(0xFF38BDF8))
                }
            }

            Spacer(Modifier.height(4.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = "DIGITAL GOSTAR PARSIAN  •  BOOSTER PUMP CONTROL",
                    style = MonoStyle.copy(fontSize = 9.5.sp, letterSpacing = 0.8.sp),
                    color = Color(0xFF475266),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RoleBadgeChip(label: String, accent: Color) {
    Surface(
        color = Color(0xFF0E121B),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.28f)),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = accent.copy(alpha = 0.92f),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.license.FighterLicenseStatus
import com.example.data.model.UserProfile
import com.example.data.payment.ProPlanOption
import com.example.data.payment.YooKassaConfig
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGoldText
import com.example.ui.theme.TacticalRedText
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTealText
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Каптёрка PRO": status on top (days ring when active, demo days / price
 * otherwise), what PRO gives, three steps of how payment works, one big pay
 * button, then "I paid — check". Entering an existing key and restoring a
 * paid licence are folded below so the main path stays simple.
 */
@Composable
fun PersonalLicenseDialog(
    profile: UserProfile?,
    licenseStatus: FighterLicenseStatus,
    yooKassaConfig: YooKassaConfig,
    issuedPaymentKey: String? = null,
    paymentAwaitingConfirmation: Boolean = false,
    onResetPendingPayment: () -> Unit = {},
    onPayYooKassaClick: () -> Unit = {},
    onPayYooKassaPlanClick: ((Int) -> Unit)? = null,
    onActivateLicenseKey: (String) -> Unit,
    onTestPaymentConfirm: () -> Unit,
    onRestoreSavedLicense: () -> Unit = {},
    onRestoreFromCloud: (email: String, callsign: String) -> Unit = { _, _ -> },
    @Suppress("UNUSED_PARAMETER") onSaveYooKassaSettings: (shopId: String, secretKey: String, isTestMode: Boolean, priceRubles: Int) -> Unit = { _, _, _, _ -> },
    onResendEmailKey: (String) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onResetLicense: () -> Unit = {},
    onDismiss: () -> Unit,
    profileMode: com.example.data.model.WorkspaceProfileMode = com.example.data.model.WorkspaceProfileMode.MILITARY
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var selectedPlan by remember { mutableStateOf(ProPlanOption.DAYS_30) }
    var paymentStartedLocal by remember(paymentAwaitingConfirmation) { mutableStateOf(paymentAwaitingConfirmation) }
    val paymentStarted = paymentAwaitingConfirmation || paymentStartedLocal
    var verifying by remember { mutableStateOf(false) }
    var showKeyEntry by remember { mutableStateOf(false) }
    var showRestore by remember { mutableStateOf(false) }
    var enteredKey by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    val isPro = licenseStatus.isProActive
    val justPaidKey = issuedPaymentKey?.takeIf { it.isNotBlank() }
    val activeKey = justPaidKey ?: licenseStatus.licenseKey.takeIf { it.isNotBlank() }
    val email = profile?.email?.trim().orEmpty()
    val callsign = profile?.callsign?.trim().orEmpty()
    fun planPrice(plan: ProPlanOption): Int =
        if (plan == ProPlanOption.DAYS_30 && yooKassaConfig.priceRubles > 0) yooKassaConfig.priceRubles else plan.priceRubles
    val price = planPrice(selectedPlan)
    val startPay: () -> Unit = {
        onPayYooKassaPlanClick?.invoke(selectedPlan.days) ?: onPayYooKassaClick()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Animated3DDialogContainer(
            modifier = Modifier.fillMaxWidth(0.94f),
            cornerRadius = 28.dp,
            accentBorder = if (isPro || justPaidKey != null) SageGreenPrimary else Color(0xFFE0A63A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
            // HERO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            if (isPro || justPaidKey != null) listOf(Color(0xFF0B6A4F), Color(0xFF0A4F60))
                            else listOf(Color(0xFF0B5A44), Color(0xFF0A4D5C))
                        )
                    )
                    .padding(20.dp)
            ) {
                IconOrb3DButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Закрыть",
                    tint = Color.White,
                    onClick = onDismiss,
                    size = 34.dp,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    when {
                        justPaidKey != null -> {
                            AnimatedCheck(key = justPaidKey, color = Color.White, size = 72.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Оплата прошла!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Лицензия PRO активирована", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                        }
                        isPro -> {
                            DaysRing(days = licenseStatus.daysRemaining)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("PRO активна", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Text("до ${licenseStatus.expiresAtDateFormatted}", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                        }
                        else -> {
                            PulsingBadge()
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Каптёрка PRO", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                if (licenseStatus.isDemoActive) {
                                    "Демо: осталось ${licenseStatus.demoDaysLeft} дн. • далее 1 склад и 15 позиций"
                                } else {
                                    "Базовый режим: 1 склад, до 15 позиций"
                                },
                                color = if (licenseStatus.isDemoActive) Color.White.copy(alpha = 0.88f) else Color(0xFFFFC2B3),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("$price ₽", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("за ${selectedPlan.title}", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(18.dp)) {
                // Active key
                if (activeKey != null) {
                    Text("Ваш ключ", color = TacticalTextMuted, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(TacticalSurfaceLight)
                            .clickable {
                                clipboard.setText(AnnotatedString(activeKey))
                                copied = true
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(activeKey, color = SageGreenBright, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Копировать", tint = TacticalTextMuted, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        if (copied) "✓ Скопировано" else "Ключ также отправлен на почту ${email.ifBlank { "" }}".trim(),
                        color = if (copied) SageGreenBright else TacticalTextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (justPaidKey != null) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SageGreenPrimary, contentColor = Color.White)
                    ) { Text("Начать работу с PRO", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                } else {
                    // What PRO gives
                    Text("Что даёт PRO", color = TacticalTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Benefit(
                        Icons.Rounded.Description,
                        "Отчёты Excel (.xlsx)",
                        if (profileMode.isMilitary) "Формы № 8 и № 18, акт списания и сводные"
                        else "Оборотная ТМЦ, М-11 / ТОРГ-13, МБ-7 и акт МБ-8"
                    )
                    Benefit(
                        Icons.Rounded.Sync,
                        "Безлимит складов и синхронизация",
                        if (profileMode.isMilitary) "неограниченно точек, позиций, заявок и QR-подключение"
                        else "неограниченно складов, позиций, заявок и QR-подключение"
                    )
                    Benefit(Icons.Rounded.Shield, "Лицензия хранится на сервере", "не пропадёт при переустановке и смене телефона")
                    Benefit(Icons.Rounded.MarkEmailRead, "Ключ на Email • самозанятый (НПД)", "оплата через ЮKassa (СБП / МИР), чек «Мой налог»")

                    Spacer(modifier = Modifier.height(16.dp))
                    if (!paymentStarted) {
                        Text("Выберите срок PRO", color = TacticalTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProPlanOption.values().forEach { plan ->
                                val isSelected = plan == selectedPlan
                                val optPrice = planPrice(plan)
                                val formattedPrice = "%,d ₽".format(java.util.Locale("ru"), optPrice).replace(',', ' ')
                                val subBadge = plan.badge.ifEmpty { "${(optPrice / plan.days).coerceAtLeast(1)} ₽/день" }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(88.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) Color(0xFFE0A63A).copy(alpha = 0.16f) else TacticalSurfaceLight
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFFE0A63A) else TacticalBorderSubtle,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { selectedPlan = plan }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = plan.title,
                                            color = if (isSelected) TacticalGoldText else TacticalTextSecondary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = formattedPrice,
                                            color = TacticalTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = subBadge,
                                            color = if (plan.badge.isNotEmpty()) SageGreenBright else TacticalTextMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(if (isPro) "Продлить на ${selectedPlan.title}" else "Как оплатить", color = TacticalTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Step(1, "Нажмите «Оплатить» — откроется ЮKassa")
                        Step(2, "Оплатите через СБП, картой МИР или SberPay")
                        Step(3, "Вернитесь сюда и нажмите «Я оплатил»")
                        Spacer(modifier = Modifier.height(14.dp))
                        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            Text(
                                "Перед оплатой укажите корректный Email в разделе «Ещё → Профиль → Изменить данные» — туда придёт ключ лицензии.",
                                color = TacticalRedText,
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }
                        Volumetric3DButton(
                            text = "Оплатить $price ₽",
                            onClick = { startPay() },
                            modifier = Modifier.fillMaxWidth().testTag("yookassa_pay_button"),
                            height = 56.dp,
                            cornerRadius = 18.dp,
                            gradientStart = Color(0xFFF59E0B),
                            gradientEnd = Color(0xFFD97706),
                            extrusionColor = Color(0xFF92400E),
                            contentColor = Color(0xFF1C1405)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(TacticalSurfaceLight)
                                .border(1.dp, TacticalGoldText.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Ждём оплату", color = TacticalGoldText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Завершите оплату в банке, вернитесь в приложение и нажмите кнопку ниже.",
                                color = TacticalTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (!verifying) {
                                        verifying = true
                                        onTestPaymentConfirm()
                                        scope.launch { delay(2500); verifying = false }
                                    }
                                },
                                enabled = !verifying,
                                modifier = Modifier.fillMaxWidth().height(54.dp).testTag("verify_payment_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SageGreenPrimary, contentColor = Color.White)
                            ) {
                                if (verifying) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Проверяем платёж…", fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Я оплатил — проверить", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = startPay) {
                                    Text("Открыть оплату снова", color = TacticalGoldText, fontSize = 12.5.sp)
                                }
                                TextButton(onClick = {
                                    paymentStartedLocal = false
                                    onResetPendingPayment()
                                }) {
                                    Text("Выбрать другой срок", color = TacticalTextMuted, fontSize = 12.5.sp)
                                }
                            }
                        }
                    }
                    Text(
                        "Платёж обрабатывает ЮKassa. Данные карты приложение не видит.",
                        color = TacticalTextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Have a key
                FoldHeader(Icons.Rounded.Key, "У меня уже есть ключ", showKeyEntry) { showKeyEntry = !showKeyEntry }
                AnimatedVisibility(visible = showKeyEntry, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = enteredKey,
                            onValueChange = { enteredKey = it.uppercase().take(24) },
                            placeholder = { Text("KAPT-XXXX-XXXX-XXXX", color = TacticalTextMuted, fontFamily = FontFamily.Monospace) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = TacticalTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SageGreenBright, unfocusedBorderColor = TacticalBorderSubtle)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onActivateLicenseKey(enteredKey.trim())
                                enteredKey = ""
                            },
                            enabled = enteredKey.trim().length >= 19,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SageGreenPrimary, contentColor = Color.White)
                        ) { Text("Активировать ключ", fontWeight = FontWeight.Bold) }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Restore
                FoldHeader(Icons.Rounded.PhoneAndroid, "Сменили телефон или потеряли ключ", showRestore) { showRestore = !showRestore }
                AnimatedVisibility(visible = showRestore, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RestoreRow(Icons.Rounded.CloudDownload, "Найти оплату по почте", if (email.isNotBlank()) email else "укажите почту в профиле", enabled = email.isNotBlank()) {
                            onRestoreFromCloud(email, callsign)
                        }
                        RestoreRow(Icons.Rounded.Key, "Из памяти этого телефона", "если ключ уже вводили здесь") { onRestoreSavedLicense() }
                        if (isPro && email.isNotBlank()) {
                            RestoreRow(Icons.Rounded.MarkEmailRead, "Выслать ключ на почту", email) { onResendEmailKey(email) }
                        }
                        RestoreRow(Icons.Rounded.WorkspacePremium, "Написать в поддержку", "Telegram-бот @kapterka_help_bot") {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.SUPPORT_BOT)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
private fun DaysRing(days: Int) {
    val totalDays = when {
        days > 90 -> 365f
        days > 30 -> 90f
        else -> 30f
    }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(days) { sweep.animateTo((days.coerceAtLeast(0) / totalDays).coerceIn(0f, 1f), tween(1100, easing = FastOutSlowInEasing)) }
    Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(96.dp)) {
            val stroke = 9.dp.toPx()
            drawArc(Color.White.copy(alpha = 0.2f), -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke))
            drawArc(Color.White, -90f, 360f * sweep.value, false, style = Stroke(stroke, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$days", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("дней", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun PulsingBadge() {
    val t = rememberInfiniteTransition(label = "badge")
    val s by t.animateFloat(1f, 1.08f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s")
    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer { scaleX = s; scaleY = s }
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Color(0xFFFFD27A), modifier = Modifier.size(38.dp))
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(SageGreenPrimary, TacticalTealText))),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = TacticalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TacticalTextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(26.dp).clip(CircleShape).background(TacticalGoldText.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) { Text("$n", color = TacticalGoldText, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, color = TacticalTextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun FoldHeader(icon: ImageVector, title: String, open: Boolean, onClick: () -> Unit) {
    val rot by androidx.compose.animation.core.animateFloatAsState(if (open) 180f else 0f, label = "fold")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TacticalSurfaceLight)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = SageGreenBright, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, color = TacticalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = TacticalTextMuted, modifier = Modifier.graphicsLayer { rotationZ = rot })
    }
}

@Composable
private fun RestoreRow(icon: ImageVector, title: String, subtitle: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) SageGreenBright else TacticalRedText, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = TacticalTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TacticalTextMuted, fontSize = 11.sp)
        }
    }
}

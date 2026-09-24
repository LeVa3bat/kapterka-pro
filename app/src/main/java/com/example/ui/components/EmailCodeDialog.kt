package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.auth.EmailCodeResult
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBorder
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
 * Registration step: a 6-digit code is e-mailed, the user types it into six
 * large cells. If the server is unreachable the user may continue
 * unconfirmed instead of being locked out.
 */
@Composable
fun EmailCodeDialog(
    email: String,
    onSendCode: suspend () -> EmailCodeResult,
    onVerifyCode: suspend (String) -> EmailCodeResult,
    onVerified: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Отправляем код…") }
    var isError by remember { mutableStateOf(false) }
    var canSkip by remember { mutableStateOf(false) }
    var resendIn by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }

    suspend fun send() {
        busy = true
        val r = onSendCode()
        busy = false
        message = r.message
        isError = !r.success
        canSkip = canSkip || r.canSkip
        resendIn = if (r.success) 60 else r.retryAfterSeconds
    }

    fun verify(value: String) {
        if (value.length != 6 || busy) return
        scope.launch {
            busy = true
            val r = onVerifyCode(value)
            busy = false
            if (r.success) {
                onVerified()
            } else {
                message = r.message
                isError = true
                canSkip = canSkip || r.canSkip
                code = ""
            }
        }
    }

    LaunchedEffect(Unit) {
        send()
        runCatching { focus.requestFocus() }
    }
    LaunchedEffect(resendIn) {
        if (resendIn > 0) {
            delay(1_000)
            resendIn -= 1
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(TacticalSurface)
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(SageGreenPrimary, TacticalTealText))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("Подтвердите почту", color = TacticalTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Мы отправили 6-значный код на\n$email",
                color = TacticalTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))

            // Six cells backed by one invisible text field.
            BasicTextField(
                value = code,
                onValueChange = { v ->
                    val digits = v.filter { it.isDigit() }.take(6)
                    code = digits
                    if (digits.length == 6) verify(digits)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.focusRequester(focus),
                decorationBox = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(6) { i ->
                            val filled = i < code.length
                            val active = i == code.length
                            val border by animateColorAsState(
                                when {
                                    isError && code.isEmpty() -> TacticalRedText
                                    active -> SageGreenBright
                                    filled -> SageGreenPrimary
                                    else -> TacticalBorder
                                },
                                label = "cell"
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 42.dp, height = 52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(TacticalSurfaceLight)
                                    .border(if (active) 2.dp else 1.dp, border, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = code.getOrNull(i)?.toString() ?: "",
                                    color = TacticalTextPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))
            if (busy) {
                CircularProgressIndicator(color = SageGreenBright, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    text = message,
                    color = if (isError) TacticalRedText else TacticalTextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { verify(code) },
                enabled = code.length == 6 && !busy,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SageGreenPrimary, contentColor = Color.White)
            ) {
                Text("Подтвердить", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = { scope.launch { send() } },
                enabled = resendIn == 0 && !busy
            ) {
                Text(
                    text = if (resendIn > 0) "Отправить снова через $resendIn с" else "Отправить код снова",
                    color = if (resendIn > 0) TacticalTextMuted else SageGreenBright,
                    fontSize = 13.sp
                )
            }
            if (canSkip) {
                Text(
                    text = "Продолжить без подтверждения",
                    color = TacticalTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSkip() }
                        .padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}

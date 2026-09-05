package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserProfile
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary

@Composable
fun UnitKeySyncDialog(

    profile: UserProfile?,
    onRegenerateKey: () -> Unit,
    onUpdateUnitKey: (String) -> Unit = {},
    onForceSync: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val unitKey = profile?.unitKey ?: "kapt_59e13b"
    val unitName = profile?.unitName ?: "1-е Подразделение"
    var manualKeyInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SageGreenPrimary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF162B1F))
                                .border(1.dp, SageGreenPrimary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Код подразделения",
                                tint = SageGreenBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "КОД ПОДКЛЮЧЕНИЯ",
                                color = SageGreenBright,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = unitName,
                                color = TacticalTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = TacticalTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // LARGE PROMINENT TEXT CODE BOX
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0D1711))
                        .border(1.5.dp, SageGreenBright, RoundedCornerShape(10.dp))
                        .clickable {
                            copyToClipboard(context, unitKey)
                        }
                        .padding(vertical = 18.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "СЕКРЕТНЫЙ КОД РОТЫ / ВЗВОДА",
                            color = TacticalGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = unitKey,
                            color = SageGreenBright,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Нажмите для копирования",
                            color = TacticalTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1-TAP BIG COPY BUTTON
                Button(
                    onClick = {
                        copyToClipboard(context, unitKey)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("copy_unit_code_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreenPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Скопировать код",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Скопировать " + "Код подразделения".lowercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detailed Instruction Note
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalBg)
                        .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Инструкция",
                            tint = SageGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Как подключить других пользователей:",
                            color = TacticalTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Передайте этот код старшине, командиру или бойцам.\n2. На другом телефоне при первом запуске нажать «Вход в подразделение».\n3. Ввести данный код — все склады, остатки и заявки синхронизируются автоматически.",
                        color = TacticalTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ввести чужой код подразделения
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalSurfaceLight)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "ПОДКЛЮЧИТЬСЯ К ДРУГОМУ ПОДРАЗДЕЛЕНИЮ",
                        color = TacticalTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = manualKeyInput,
                            onValueChange = { manualKeyInput = it },
                            placeholder = { Text("Код (напр. kapt_abc123)", fontSize = 11.sp, color = TacticalTextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp,
                                color = TacticalTextPrimary,
                                fontFamily = FontFamily.Monospace
                            ),
                            shape = RoundedCornerShape(6.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SageGreenBright,
                                unfocusedBorderColor = TacticalBorderSubtle
                            )
                        )
                        Button(
                            onClick = {
                                if (manualKeyInput.isNotBlank()) {
                                    onUpdateUnitKey(manualKeyInput.trim())
                                    onDismiss()
                                }
                            },
                            enabled = manualKeyInput.trim().length >= 4,
                            modifier = Modifier.height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageGreenPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Подключиться", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom actions: Regenerate code & Sync
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRegenerateKey,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalSurfaceLight,
                            contentColor = TacticalTextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Новый код",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сменить код", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            onForceSync()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3D2B),
                            contentColor = SageGreenBright
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Синхронизировать",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Синхр. базы", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("UnitCode", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$text скопирован в буфер", Toast.LENGTH_SHORT).show()
}

package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserProfile
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalRed
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTealText
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary
import com.example.util.UnitQr
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * "Подразделение": the unit key as a big QR code (another phone scans it to
 * join), copy/share buttons, joining another unit by typing or scanning its
 * key, and the tools for phones whose data diverged.
 */
@Composable
fun UnitKeySyncDialog(
    profile: UserProfile?,
    onRegenerateKey: () -> Unit,
    onUpdateUnitKey: (String) -> Unit = {},
    onForceSync: () -> Unit,
    onMakeReference: () -> Unit = {},
    onLoadFromCloud: () -> Unit = {},
    isSyncPaused: Boolean = false,
    onPauseSync: () -> Unit = {},
    onResumeSync: () -> Unit = {},
    onDeleteCloudData: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val unitKey = profile?.unitKey?.trim().orEmpty()
    val unitName = profile?.unitName?.takeIf { it.isNotBlank() } ?: "Подразделение"
    var manualKeyInput by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<String?>(null) }
    val scanLauncher = rememberQrScanLauncher { invite ->
        manualKeyInput = invite.unitKey
        Toast.makeText(
            context,
            if (invite.unitName.isNotBlank()) "QR распознан: «${invite.unitName}». Нажмите «Подключить»." else "QR распознан. Нажмите «Подключить».",
            Toast.LENGTH_LONG
        ).show()
    }

    confirmAction?.let { action ->
        SyncConfirmDialog(
            action = action,
            onConfirm = {
                when (action) {
                    "reference" -> onMakeReference()
                    "load" -> onLoadFromCloud()
                    "regenerate" -> onRegenerateKey()
                    "pause" -> onPauseSync()
                    "delete_cloud" -> onDeleteCloudData()
                }
                onDismiss()
            },
            onDismiss = { confirmAction = null }
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(26.dp))
                .background(TacticalSurface)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(SageGreenPrimary, TacticalTealText))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Подразделение", color = TacticalTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text(unitName, color = TacticalTextSecondary, fontSize = 13.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Закрыть", tint = TacticalTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QR + key
            if (unitKey.isNotBlank()) {
                UnitQrCard(unitKey = unitKey, unitName = unitName)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(Icons.Rounded.ContentCopy, "Копировать", Modifier.weight(1f)) {
                        copyToClipboard(context, unitKey)
                    }
                    PillButton(Icons.Rounded.Share, "Поделиться", Modifier.weight(1f)) {
                        shareKey(context, unitKey, unitName)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "На другом телефоне: «Вход по ключу» → «Сканировать QR» и наведите камеру на этот код.",
                    color = TacticalTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Join another unit
            SectionCard {
                Text("Подключиться к другому подразделению", color = TacticalTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Отсканируйте QR-код или введите ключ вида kapt_…",
                    color = TacticalTextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = manualKeyInput,
                    onValueChange = { manualKeyInput = it.trim() },
                    placeholder = { Text("kapt_…", color = TacticalTextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 15.sp,
                        color = TacticalTextPrimary,
                        fontFamily = FontFamily.Monospace
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SageGreenBright,
                        unfocusedBorderColor = TacticalBorderSubtle
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { scanLauncher() },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = SageGreenBright, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сканировать", color = SageGreenBright, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = {
                            val invite = UnitQr.parse(manualKeyInput)
                            if (invite == null) {
                                Toast.makeText(context, "Проверьте ключ: только латиница, цифры, _ и -", Toast.LENGTH_LONG).show()
                            } else {
                                onUpdateUnitKey(invite.unitKey)
                                onDismiss()
                            }
                        },
                        enabled = manualKeyInput.length >= 4,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SageGreenPrimary, contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Подключить", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Если на этом телефоне уже есть записи, смена подразделения будет заблокирована — чтобы ничего не потерять.",
                    color = TacticalTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sync tools
            SectionCard {
                Button(
                    onClick = { onForceSync(); onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3D2B), contentColor = SageGreenBright)
                ) {
                    Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Синхронизировать сейчас", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Если на телефонах разные данные", color = TacticalTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FixRow(Icons.Rounded.CloudUpload, "Этот телефон — эталон", "отправить его данные всем", SageGreenBright) {
                    confirmAction = "reference"
                }
                Spacer(modifier = Modifier.height(6.dp))
                FixRow(Icons.Rounded.CloudDownload, "Загрузить всё из облака", "заменить данные этого телефона", TacticalGold) {
                    confirmAction = "load"
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Local mode and cloud data removal
            SectionCard {
                Text("Данные и конфиденциальность", color = TacticalTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                if (unitKey.isBlank()) {
                    Text(
                        "Общий учёт не подключён: данные хранятся только на этом телефоне и никуда не отправляются.",
                        color = TacticalTextMuted, fontSize = 12.sp, lineHeight = 16.sp
                    )
                } else if (isSyncPaused) {
                    Text(
                        "Синхронизация приостановлена: данные хранятся только на этом телефоне. Ключ сохранён.",
                        color = TacticalTextMuted, fontSize = 12.sp, lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FixRow(Icons.Rounded.Sync, "Включить синхронизацию", "продолжить обмен с облаком по этому ключу", SageGreenBright) {
                        onResumeSync()
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    FixRow(Icons.Rounded.DeleteForever, "Удалить мои данные из облака", "ключ и данные на телефоне останутся", TacticalRed) {
                        confirmAction = "delete_cloud"
                    }
                } else {
                    Text(
                        "Пока синхронизация включена, данные подразделения хранятся в облаке (Google Firebase).",
                        color = TacticalTextMuted, fontSize = 12.sp, lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FixRow(Icons.Rounded.CloudOff, "Приостановить синхронизацию", "данные и ключ остаются, включить можно в любой момент", TacticalGold) {
                        confirmAction = "pause"
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    FixRow(Icons.Rounded.DeleteForever, "Удалить мои данные из облака", "и приостановить синхронизацию", TacticalRed) {
                        confirmAction = "delete_cloud"
                    }
                }
            }

            TextButton(
                onClick = { confirmAction = "regenerate" },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Создать новый ключ подразделения", color = TacticalTextMuted, fontSize = 12.sp)
            }
        }
    }
}

/** Opens the camera scanner; calls [onInvite] only for a valid unit key. */
@Composable
fun rememberQrScanLauncher(onInvite: (UnitQr.Invite) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result?.contents ?: return@rememberLauncherForActivityResult
        val invite = UnitQr.parse(contents)
        if (invite == null) {
            Toast.makeText(context, "Это не QR-код подразделения «Каптёрка ПРО»", Toast.LENGTH_LONG).show()
        } else {
            onInvite(invite)
        }
    }
    return {
        runCatching {
            launcher.launch(
                ScanOptions()
                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    .setPrompt("Наведите камеру на QR-код подразделения")
                    .setBeepEnabled(false)
                    .setOrientationLocked(false)
            )
        }.onFailure {
            Toast.makeText(context, "Не удалось открыть камеру", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun UnitQrCard(unitKey: String, unitName: String) {
    val bitmap = remember(unitKey, unitName) {
        runCatching { UnitQr.bitmap(UnitQr.payload(unitKey, unitName)).asImageBitmap() }.getOrNull()
    }
    val appear = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1F7A57), Color(0xFF0F766E))))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer { scaleX = appear.value; scaleY = appear.value }
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = "QR-код подразделения", modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("КЛЮЧ ПОДРАЗДЕЛЕНИЯ", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(
            unitKey,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TacticalSurfaceLight)
            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) { content() }
}

@Composable
private fun PillButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TacticalSurfaceLight)
            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = SageGreenBright, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = TacticalTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FixRow(icon: ImageVector, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TacticalSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TacticalTextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SyncConfirmDialog(
    action: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, text, button) = when (action) {
        "reference" -> Triple(
            "Сделать этот телефон эталоном?",
            "Облако станет точной копией ЭТОГО телефона. Точки, остатки и операции, которых нет на этом телефоне, " +
                "будут удалены из облака и со всех телефонов подразделения версии 3.6. Делайте это на телефоне с правильными данными.",
            "Да, отправить"
        )
        "load" -> Triple(
            "Загрузить всё из облака?",
            "ЭТОТ телефон станет точной копией облака. Точки, остатки и операции, которых нет в облаке, будут удалены с этого телефона.",
            "Да, загрузить"
        )
        "pause" -> Triple(
            "Приостановить синхронизацию?",
            "Обмен с облаком прекратится: данные будут храниться только на этом телефоне и никуда не отправляться. " +
                "Ключ подразделения и все данные останутся. Данные, которые уже лежат в облаке, не удаляются. " +
                "Включить синхронизацию обратно можно в любой момент этим же окном.",
            "Приостановить"
        )
        "delete_cloud" -> Triple(
            "Удалить данные из облака?",
            "Все данные подразделения (склады, остатки, операции, заявки, список устройств и облачная копия) будут удалены с сервера, " +
                "синхронизация приостановится. Ключ и данные на этом телефоне останутся. " +
                "Если у других телефонов подразделения синхронизация включена, они могут загрузить свои данные в облако заново. " +
                "Отменить удаление нельзя.",
            "Удалить"
        )
        else -> Triple(
            "Создать новый ключ?",
            "Старый ключ перестанет подходить для новых подключений. Другим телефонам нужно будет подключиться заново по новому ключу или QR-коду.",
            "Создать"
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(button) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("UnitCode", text))
    Toast.makeText(context, "Ключ скопирован", Toast.LENGTH_SHORT).show()
}

private fun shareKey(context: Context, unitKey: String, unitName: String) {
    val text = "Подключение к подразделению «$unitName» в «Каптёрка ПРО»:\n" +
        "ключ $unitKey\n\nУстановить приложение: https://kapterka-pro.ru/"
    runCatching {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
                "Отправить ключ"
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary

/** Official links of the app. */
object AppLinks {
    const val SITE = "https://kapterka-pro.ru/"
    const val CHANNEL = "https://t.me/kapterka_pro"
    const val SUPPORT_BOT = "https://t.me/kapterka_help_bot"
    const val EMPTY_TEMPLATE = "https://kapterka-pro.ru/uchet-instrumenta-android.html?utm_source=referral&utm_medium=share&utm_campaign=workshop_30d&utm_content=blank_template#workshop-kit"
    const val RECOMMEND_APP = "https://kapterka-pro.ru/?utm_source=referral&utm_medium=share&utm_campaign=workshop_30d&utm_content=app"
}

private data class CommunityLink(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val url: String,
    val colors: List<Color>
)

private data class PublicShareLink(val title: String, val text: String, val url: String)

/** Official links and voluntary sharing of public materials only. */
@Composable
fun CommunityLinksCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val links = listOf(
        CommunityLink(Icons.Rounded.Language, "Сайт", "kapterka-pro.ru", AppLinks.SITE, listOf(Color(0xFF1F7A57), Color(0xFF0F766E))),
        CommunityLink(Icons.AutoMirrored.Rounded.Send, "Канал", "новости и обновления", AppLinks.CHANNEL, listOf(Color(0xFF2AABEE), Color(0xFF1E88C7))),
        CommunityLink(Icons.Rounded.SupportAgent, "Поддержка", "бот в Telegram", AppLinks.SUPPORT_BOT, listOf(Color(0xFF7C5CFF), Color(0xFF5B3FD6)))
    )
    val publicShares = listOf(
        PublicShareLink("Поделиться пустым шаблоном", "Бесплатный пустой шаблон учёта для небольшой мастерской: Excel и PDF.", AppLinks.EMPTY_TEMPLATE),
        PublicShareLink("Посоветовать приложение", "Каптёрка ПРО — учёт имущества и остатков на Android.", AppLinks.RECOMMEND_APP)
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TacticalSurface)
            .border(1.dp, TacticalBorderSubtle, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Text("Мы на связи", color = TacticalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            "Новости, советы по учёту и помощь — в нашем канале и на сайте",
            color = TacticalTextMuted,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            links.forEach { link ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(link.colors))
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(link.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }.onFailure {
                                Toast.makeText(context, link.url, Toast.LENGTH_LONG).show()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(link.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(link.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(1.dp))
                    Text(
                        link.subtitle,
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Можно отправить знакомым публичную ссылку. Данные вашего учёта не добавляются.",
            color = TacticalTextMuted,
            fontSize = 11.sp
        )
        publicShares.forEach { link ->
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TITLE, link.title)
                        putExtra(Intent.EXTRA_TEXT, "${link.text}\n${link.url}")
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(send, link.title))
                    }.onFailure {
                        Toast.makeText(context, "Не удалось открыть меню отправки", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalTextPrimary),
                border = BorderStroke(1.dp, TacticalBorderSubtle)
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(link.title, fontSize = 13.sp)
            }
        }
    }
}

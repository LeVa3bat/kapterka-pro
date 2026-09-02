package com.example.ui.components

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserProfile
import com.example.ui.theme.SageGreenBright
import com.example.ui.theme.SageGreenContainer
import com.example.ui.theme.SageGreenDark
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.TacticalBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalBorderSubtle
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalGoldDark
import com.example.ui.theme.TacticalGoldText
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceLight
import com.example.ui.theme.TacticalTextDim
import com.example.ui.theme.TacticalTextMuted
import com.example.ui.theme.TacticalTextPrimary
import com.example.ui.theme.TacticalTextSecondary

@Composable
fun PaymentProDialog(
    profile: UserProfile?,
    onActivatePro: () -> Unit,
    onDismiss: () -> Unit
) {
    var promoCode by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("sbp") } // sbp or card

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(TacticalGoldDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MilitaryTech,
                                contentDescription = "PRO",
                                tint = TacticalGoldText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ТАРИФ «КАПТЁРКА ПРО»",
                            color = TacticalGoldText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
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

                Spacer(modifier = Modifier.height(12.dp))

                // Price Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF262013))
                        .border(1.dp, TacticalGoldDark, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ПОЛНЫЙ ДОСТУП ДЛЯ ВСЕЙ РОТЫ",
                            color = TacticalGoldText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Неограниченное число телефонов по единому ключу",
                            color = TacticalTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = "500 ₽/мес",
                        color = TacticalGoldText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Feature checklist
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProFeatureRow("Безлимитный учет имущества и складов")
                    ProFeatureRow("Синхронизация неограниченного числа телефонов")
                    ProFeatureRow("Экспорт Формы № 8 и Формы № 18 в Excel")
                    ProFeatureRow("Полная автономная работа в поле без интернета")
                    ProFeatureRow("Приоритетная круглосуточная поддержка")
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Options Selector
                Text(
                    text = "СПОСОБ ОПЛАТЫ / АКТИВАЦИИ",
                    color = TacticalTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethodOption(
                        label = "СБП (0% комиссия)",
                        selected = paymentMethod == "sbp",
                        onClick = { paymentMethod = "sbp" },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentMethodOption(
                        label = "Банковская карта",
                        selected = paymentMethod == "card",
                        onClick = { paymentMethod = "card" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Activation Code Field
                OutlinedTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it },
                    label = { Text("Код активации / Промокод роты", color = TacticalTextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("например: PRO-WAR-2026", color = TacticalTextDim, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Ключ",
                            tint = SageGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TacticalSurfaceLight,
                        unfocusedContainerColor = TacticalSurfaceLight,
                        focusedBorderColor = SageGreenPrimary,
                        unfocusedBorderColor = TacticalBorder,
                        focusedTextColor = TacticalTextPrimary,
                        unfocusedTextColor = TacticalTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pay / Activate Button
                Button(
                    onClick = {
                        onActivatePro()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("activate_pro_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TacticalGold,
                        contentColor = Color(0xFF231B05)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (promoCode.isNotBlank()) "Активировать по коду" else "Оплатить 500 ₽ и активировать PRO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProFeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = SageGreenBright,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = TacticalTextPrimary, fontSize = 12.sp)
    }
}

@Composable
private fun PaymentMethodOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) SageGreenContainer else TacticalSurfaceLight)
            .border(
                1.dp,
                if (selected) SageGreenPrimary else TacticalBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 9.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) SageGreenBright else TacticalTextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

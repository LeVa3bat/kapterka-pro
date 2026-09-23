package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppDestination

private data class UniversalNavItem(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector
)

@Composable
fun UniversalBottomNavigationBar(
    currentDestination: AppDestination,
    pendingRequestsCount: Int,
    onNavigate: (AppDestination) -> Unit
) {
    val items = listOf(
        UniversalNavItem(AppDestination.HOME, "Склад", Icons.Default.Home),
        UniversalNavItem(AppDestination.CATALOG, "Каталог", Icons.Default.Inventory2),
        UniversalNavItem(AppDestination.HISTORY, "Операции", Icons.Default.ReceiptLong),
        UniversalNavItem(AppDestination.REQUESTS, "Заявки", Icons.Default.Assignment),
        UniversalNavItem(AppDestination.MORE, "Ещё", Icons.Default.Person)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F7FB))
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentDestination == item.destination
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(item.destination) }
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) Color(0xFFEEEEFF) else Color.Transparent,
                                    RoundedCornerShape(17.dp)
                                )
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.layout.Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (item.destination == AppDestination.REQUESTS && pendingRequestsCount > 0) {
                                            Badge(
                                                containerColor = Color(0xFFD94C4C),
                                                contentColor = Color.White
                                            ) {
                                                Text(
                                                    text = pendingRequestsCount.coerceAtMost(99).toString(),
                                                    fontSize = 8.sp
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = if (selected) Color(0xFF5B5CE2) else Color(0xFF9AA1AE),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Text(
                                    text = item.label,
                                    color = if (selected) Color(0xFF5B5CE2) else Color(0xFF8B93A1),
                                    fontSize = 8.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

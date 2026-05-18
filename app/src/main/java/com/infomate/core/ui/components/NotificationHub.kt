package com.infomate.core.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.core.domain.model.InfomateNotification
import com.infomate.core.domain.model.NotificationType
import com.infomate.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationHub(notifications: List<InfomateNotification>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(modifier = Modifier.size(12.dp, 2.dp).background(CyberCyan))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "SYSTEM BROADCASTS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CyberCyan,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "${notifications.size} ACTIVE",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SilverText.copy(alpha = 0.4f),
                    fontSize = 9.sp
                )
            )
        }

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(DeepSpace.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No active transmissions.",
                    style = MaterialTheme.typography.bodySmall.copy(color = GhostGray)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationItem(notification)
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: InfomateNotification) {
    val color = when(notification.type) {
        NotificationType.COMPANION -> CompanionGold
        NotificationType.AWAKENED -> ElectricViolet
        NotificationType.DELEGATION -> NeonBlue
        NotificationType.SECURITY -> MatrixGreen
        else -> CyberCyan
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DeepSpace.copy(alpha = 0.5f),
        border = BorderStroke(0.5.dp, Brush.linearGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent)))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Indicator
            Box(
                modifier = Modifier
                    .size(4.dp, 24.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = color,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(notification.timestamp)),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GhostGray,
                            fontSize = 8.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SilverText.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}

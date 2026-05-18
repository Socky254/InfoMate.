package com.infomate.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.core.ui.theme.MatrixGreen
import com.infomate.core.ui.theme.SilverText
import com.infomate.core.ui.theme.Obsidian

@Composable
fun MemoryStreamPanel(logs: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Obsidian.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(modifier = Modifier.size(4.dp).background(MatrixGreen))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SYSTEM TELEMETRY",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MatrixGreen.copy(alpha = 0.5f),
                    letterSpacing = 2.sp,
                    fontSize = 10.sp
                )
            )
        }
        LazyColumn(
            modifier = Modifier.height(120.dp),
            reverseLayout = true
        ) {
            items(logs) { log ->
                Text(
                    text = "[LOG] $log",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MatrixGreen.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        lineHeight = 14.sp
                    ),
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

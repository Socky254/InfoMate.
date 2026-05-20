package com.infomate.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.app.ui.theme.Obsidian
import com.infomate.app.ui.theme.MatrixGreen
import com.infomate.app.ui.theme.SilverText
import com.infomate.app.ui.theme.ErrorRed
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SystemTerminalView(state: UIState, onDismiss: () -> Unit) {
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    LaunchedEffect(state.terminalLogs.size) {
        if (state.terminalLogs.isNotEmpty()) {
            listState.animateScrollToItem(state.terminalLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Futuristic Terminal Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Terminal, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "OMEGA_TERMINAL",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = SilverText.copy(alpha = 0.5f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Terminal Output Surface
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MatrixGreen.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            color = Color.Black.copy(alpha = 0.4f),
            shape = RoundedCornerShape(16.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(state.terminalLogs) { log ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            "${timeFormat.format(Date(log.timestamp))} > ",
                            color = MatrixGreen.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            log.message,
                            color = when (log.level) {
                                "ERROR" -> ErrorRed
                                "WARN" -> Color.Yellow
                                "SUCCESS" -> MatrixGreen
                                else -> SilverText.copy(alpha = 0.8f)
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

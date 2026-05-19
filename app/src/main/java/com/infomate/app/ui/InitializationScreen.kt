package com.infomate.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.core.ui.theme.*

@Composable
fun InitializationScreen(vm: AgentViewModel) {
    val state by vm.state.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Central Pulsing Logo / Indicator
            LoadingNeuralCore()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "INFOMATE_STABILIZING",
                color = CyberCyan,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "ESTABLISHING NEURAL LINK AND ARCHIVE SYNC",
                color = SilverText.copy(alpha = 0.5f),
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // System Status Grid
            SystemStatusRow(
                label = "INTERNET_SYNC",
                isActive = state.isInternetAvailable,
                icon = if (state.isInternetAvailable) Icons.Default.Wifi else Icons.Default.WifiOff
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SystemStatusRow(
                label = "NEURAL_CORE_BRIDGE",
                isActive = state.isConnected,
                icon = if (state.isConnected) Icons.Default.Power else Icons.Default.CloudOff
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SystemStatusRow(
                label = "SUBSTRATE_AWARENESS",
                isActive = state.isSubstrateAwake,
                icon = Icons.Default.AutoAwesome
            )
            
            if (state.pendingUpdate != null) {
                Spacer(modifier = Modifier.height(32.dp))
                UpdateAlertCard(
                    version = state.pendingUpdate?.version_name ?: "",
                    onUpdate = { vm.startUpdate() }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom Action - Allow bypass or AI interaction
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "You can communicate with the local substrate while sync completes.",
                    color = SilverText.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { vm.selectTab(DashboardTab.CHAT) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ENTER MAIN TERMINAL", color = CyberCyan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LoadingNeuralCore() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer Rings
        Box(
            modifier = Modifier
                .size(140.dp)
                .border(1.dp, CyberCyan.copy(alpha = 0.1f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .border(2.dp, CyberCyan.copy(alpha = 0.2f), CircleShape)
        )
        
        // Inner Pulsing Core
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyberCyan.copy(alpha = alpha), Color.Transparent)
                    )
                )
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(CyberCyan)
            )
        }
    }
}

@Composable
fun SystemStatusRow(label: String, isActive: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) MatrixGreen else ErrorRed,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            label,
            color = SilverText.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            if (isActive) "ONLINE" else "STANDBY",
            color = if (isActive) MatrixGreen else SilverText.copy(alpha = 0.3f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun UpdateAlertCard(version: String, onUpdate: () -> Unit) {
    Surface(
        color = ErrorRed.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CRITICAL UPGRADE AVAILABLE", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "System v$version is ready for synchronization. Upgrading now improves neural stability.",
                color = SilverText.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onUpdate,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("INITIALIZE UPGRADE", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
    }
}

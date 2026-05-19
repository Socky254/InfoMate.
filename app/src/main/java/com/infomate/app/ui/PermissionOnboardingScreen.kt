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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RunningWithErrors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.core.ui.theme.CyberCyan
import com.infomate.core.ui.theme.NeonBlue
import com.infomate.core.ui.theme.Obsidian
import com.infomate.core.ui.theme.SilverText

@Composable
fun PermissionOnboardingScreen(onAuthorize: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "NEURAL SYNCHRONIZATION",
                style = MaterialTheme.typography.headlineMedium,
                color = SilverText,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "To function as your Transcendent Iris, InfoMate requires deep access to your digital patterns.",
                textAlign = TextAlign.Center,
                color = SilverText.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            PermissionItem(
                icon = Icons.Default.Shield,
                title = "Security & Identity",
                description = "Access to contacts and logs to recognize your primary associates."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            PermissionItem(
                icon = Icons.Default.AutoAwesome,
                title = "Cognitive Memory",
                description = "Reading calendar and messages to build your long-term neural memory."
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionItem(
                icon = Icons.Default.Fingerprint,
                title = "System Context",
                description = "Location and storage access for real-time environmental awareness."
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionItem(
                icon = Icons.Default.RunningWithErrors,
                title = "Persistent Availability",
                description = "Ability to display over locked screens and remain active during system downtime."
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onAuthorize,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(CyberCyan, NeonBlue))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "INITIALIZE NEURAL LINK",
                        color = Obsidian,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Data is encrypted and stored in your private Supabase vault.",
                fontSize = 10.sp,
                color = SilverText.copy(alpha = 0.3f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PermissionItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(CyberCyan.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(text = title, color = SilverText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = description, color = SilverText.copy(alpha = 0.5f), fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

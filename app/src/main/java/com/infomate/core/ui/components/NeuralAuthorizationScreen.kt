package com.infomate.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.core.ui.theme.*

@Composable
fun NeuralAuthorizationScreen(onAuthorize: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .background(DeepSpace.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "NEURAL LINK",
                style = MaterialTheme.typography.headlineLarge,
                color = CyberCyan,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp
            )
            Text(
                "ENVIRONMENTAL SYNCHRONIZATION",
                style = MaterialTheme.typography.labelSmall,
                color = SilverText.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                "To optimize cognitive resonance, Infomate requires access to ambient acoustic frequencies. This enables the 'Sage' protocol to monitor environmental stillness and synchronize with your focus.",
                color = SilverText,
                lineHeight = 24.sp,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onAuthorize,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("INITIALIZE LINK", color = Obsidian, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
    }
}

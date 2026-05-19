package com.infomate.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomate.core.brain.ThoughtStep
import com.infomate.core.ui.theme.CyberCyan
import com.infomate.core.ui.theme.SilverText

@Composable
fun LiveThinkingView(steps: List<ThoughtStep>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "COGNITIVE THREADS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CyberCyan.copy(alpha = 0.6f),
                    letterSpacing = 2.sp
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(CyberCyan, CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        steps.forEachIndexed { index, step ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically()
            ) {
                Row(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        text = "0${index + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = step.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SilverText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SilverText.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

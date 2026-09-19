package com.palash.voicebridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.palash.voicebridge.ui.theme.*

/**
 * Bottom status bar showing offline status, language pair, and script.
 */
@Composable
fun OfflineIndicator(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Offline badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OfflineGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "● OFFLINE",
                        style = MaterialTheme.typography.labelMedium,
                        color = OfflineGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Language pair
            Text(
                text = "Hindi → Santali",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Script
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PrimaryBlue.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "Ol Chiki",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Badge showing whether a translation is verified or AI-generated.
 */
@Composable
fun TranslationSourceBadge(
    isVerified: Boolean,
    engineUsed: String,
    modifier: Modifier = Modifier
) {
    val (text, color) = if (isVerified) {
        "✓ Verified" to PrimaryBlue
    } else {
        "AI: $engineUsed" to SecondaryOrange
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Bilingual text card showing Hindi and Santali side by side.
 */
@Composable
fun BilingualCard(
    hindi: String,
    santali: String,
    modifier: Modifier = Modifier,
    isVerified: Boolean? = null,
    engineUsed: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hindi
            Column {
                Text(
                    text = "हिंदी",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = hindi,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Divider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // Santali
            Column {
                Text(
                    text = "संताली (Ol Chiki)",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryBlue
                )
                Text(
                    text = santali,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Source badge
            if (isVerified != null && engineUsed != null) {
                TranslationSourceBadge(
                    isVerified = isVerified,
                    engineUsed = engineUsed
                )
            }
        }
    }
}

/**
 * Audio play button for Santali playback.
 */
@Composable
fun AudioPlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPlaying) StatusGreen else MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    ) {
        Text(
            text = if (isPlaying) "⏸ रुकें" else "🔊 सुनें",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

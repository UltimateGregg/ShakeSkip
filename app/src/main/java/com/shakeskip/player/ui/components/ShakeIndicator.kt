package com.shakeskip.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Lightweight indicator shown briefly when a shake is detected.
 */
@Composable
fun ShakeIndicator(
    isEnabled: Boolean,
    isShaking: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isEnabled || !isShaking) {
        return
    }

    val transition = rememberInfiniteTransition(label = "shake_indicator_motion")
    val offset by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake_indicator_offset"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Vibration,
            contentDescription = "Shake detected",
            modifier = Modifier
                .size(24.dp)
                .offset(x = offset.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Simulating CD skip…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Hint prompting the user about the shake gesture. Animates only while shaking.
 */
@Composable
fun ShakeGestureHint(
    isDetectionEnabled: Boolean,
    isShaking: Boolean,
    modifier: Modifier = Modifier
) {
    val offset: Float = if (isShaking) {
        val transition = rememberInfiniteTransition(label = "shake_hint_motion")
        val movement by transition.animateFloat(
            initialValue = -6f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 160, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shake_hint_offset"
        )
        movement
    } else {
        0f
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Vibration,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .offset(x = offset.dp),
            tint = if (isDetectionEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isDetectionEnabled) {
                "Shake to create a skip"
            } else {
                "Enable shake skip in settings"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

package com.shakeskip.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Visual status indicator for the shake-to-skip feature.
 */
@Composable
fun ShakeIndicator(
    isEnabled: Boolean,
    isDetectionRunning: Boolean,
    isShaking: Boolean,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
            isShaking -> MaterialTheme.colorScheme.primary
            isDetectionRunning -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.secondary
        },
        label = "shake_indicator_color"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isShaking) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "shake_indicator_scale"
    )

    val iconTilt by animateFloatAsState(
        targetValue = if (isShaking) 14f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "shake_indicator_tilt"
    )

    val headline = when {
        !isEnabled -> "Shake detection off"
        isShaking -> "Shake detected"
        isDetectionRunning -> "Listening for movement"
        else -> "Shake detection ready"
    }

    val supportingText: String? = when {
        !isEnabled -> "Turn on shakes in settings to unlock the effect."
        isShaking -> "Simulating that classic CD skip for a moment."
        isDetectionRunning -> "Keep your device steady to avoid skips."
        else -> null
    }

    val textColor: Color = if (isEnabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Vibration,
            contentDescription = "Shake detection",
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    rotationZ = iconTilt
                },
            tint = statusColor.copy(alpha = if (isEnabled) 1f else 0.6f)
        )

        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = headline,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = textColor
            )

            supportingText?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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

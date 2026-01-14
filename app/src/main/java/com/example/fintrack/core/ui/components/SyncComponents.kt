package com.example.fintrack.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fintrack.presentation.home.SyncStatus

/**
 * Modern sync status overlay that slides in from the top.
 * Features glassmorphism, gradient backgrounds, and smooth animations.
 */
@Composable
fun SyncStatusOverlay(
    status: SyncStatus,
    modifier: Modifier = Modifier
) {
    // Determine visibility
    val isVisible = status !is SyncStatus.Idle

    // Keep track of the last non-idle status to show during exit animation
    var activeStatus by remember { mutableStateOf<SyncStatus?>(null) }
    if (status !is SyncStatus.Idle) {
        activeStatus = status
    }

    // Use activeStatus for rendering content. If null (initial state), we don't render.
    val currentStatus = activeStatus ?: return

    // Animated visibility with slide from top
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow // Slower entry
            )
        ) + fadeIn(animationSpec = tween(600)),
        exit = slideOutVertically(
            targetOffsetY = { -it }, // Slide UP when exiting (negative value)
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow // Slower exit
            )
        ) + fadeOut(animationSpec = tween(700)),
        modifier = modifier
    ) {
        // Determine colors and content based on status
        val (gradientColors, iconColor, icon, text, showProgress) = when (currentStatus) {
            is SyncStatus.Syncing -> SyncUIConfig(
                gradientColors = listOf(
                    Color(0xFF1E88E5).copy(alpha = 0.95f),
                    Color(0xFF1565C0).copy(alpha = 0.95f)
                ),
                iconColor = Color.White,
                icon = Icons.Default.CloudSync,
                text = "Syncing...",
                showProgress = true
            )
            is SyncStatus.Success -> SyncUIConfig(
                gradientColors = listOf(
                    Color(0xFF43A047).copy(alpha = 0.95f),
                    Color(0xFF2E7D32).copy(alpha = 0.95f)
                ),
                iconColor = Color.White,
                icon = Icons.Default.CheckCircle,
                text = "Sync complete!",
                showProgress = false
            )
            is SyncStatus.Error -> SyncUIConfig(
                gradientColors = listOf(
                    Color(0xFFE53935).copy(alpha = 0.95f),
                    Color(0xFFC62828).copy(alpha = 0.95f)
                ),
                iconColor = Color.White,
                icon = Icons.Default.ErrorOutline,
                text = currentStatus.message,
                showProgress = false
            )
            else -> return@AnimatedVisibility
        }

        // Animated scale for entrance
        val scale by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .scale(scale)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.horizontalGradient(gradientColors)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated Icon
                AnimatedSyncIcon(
                    icon = icon,
                    iconColor = iconColor,
                    isAnimating = showProgress
                )

                // Text content
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                // Progress indicator for syncing state
                if (showProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

/**
 * Animated icon that rotates when syncing
 */
@Composable
private fun AnimatedSyncIcon(
    icon: ImageVector,
    iconColor: Color,
    isAnimating: Boolean
) {
    // Infinite rotation animation for sync icon
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAnimating) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Scale animation for success/error
    val scale by animateFloatAsState(
        targetValue = if (!isAnimating) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconScale"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
                .let { mod ->
                    if (isAnimating) mod.graphicsLayer { rotationZ = rotation }
                    else mod
                }
        )
    }
}

/**
 * Data class to hold sync UI configuration
 */
private data class SyncUIConfig(
    val gradientColors: List<Color>,
    val iconColor: Color,
    val icon: ImageVector,
    val text: String,
    val showProgress: Boolean
)
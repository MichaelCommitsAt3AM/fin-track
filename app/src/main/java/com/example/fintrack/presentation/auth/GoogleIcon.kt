package com.example.fintrack.presentation.auth

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Google Icon Composable
 * Draws the official Google "G" logo using Canvas paths
 */
@Composable
fun GoogleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val scale = size.width / 48f

        // Blue (Right segment)
        drawPath(
            path = Path().apply {
                moveTo(43.611f * scale, 20.083f * scale)
                lineTo(42f * scale, 20f * scale)
                lineTo(24f * scale, 20f * scale)
                lineTo(24f * scale, 28f * scale)
                lineTo(35.303f * scale, 28f * scale)
                cubicTo(34.511f * scale, 30.237f * scale, 33.072f * scale, 32.166f * scale, 31.216f * scale, 33.571f * scale)
                lineTo(37.406f * scale, 38.809f * scale)
                cubicTo(40.971f * scale, 35.205f * scale, 44f * scale, 30f * scale, 44f * scale, 24f * scale)
                cubicTo(44f * scale, 22.659f * scale, 43.862f * scale, 21.35f * scale, 43.611f * scale, 20.083f * scale)
                close()
            },
            color = Color(0xFF4285F4)
        )

        // Green (Bottom segment)
        drawPath(
            path = Path().apply {
                moveTo(24f * scale, 44f * scale)
                cubicTo(29.166f * scale, 44f * scale, 33.86f * scale, 42.023f * scale, 37.409f * scale, 38.808f * scale)
                lineTo(31.219f * scale, 33.57f * scale)
                cubicTo(29.211f * scale, 35.091f * scale, 26.715f * scale, 36f * scale, 24f * scale, 36f * scale)
                cubicTo(18.779f * scale, 36f * scale, 14.348f * scale, 32.657f * scale, 12.697f * scale, 28f * scale)
                lineTo(6.126f * scale, 32.827f * scale)
                cubicTo(9.655f * scale, 39.664f * scale, 16.318f * scale, 44f * scale, 24f * scale, 44f * scale)
                close()
            },
            color = Color(0xFF34A853)
        )

        // Red (Top segment)
        drawPath(
            path = Path().apply {
                moveTo(6.306f * scale, 14.691f * scale)
                lineTo(12.877f * scale, 19.51f * scale)
                cubicTo(14.655f * scale, 15.108f * scale, 18.961f * scale, 12f * scale, 24f * scale, 12f * scale)
                cubicTo(27.059f * scale, 12f * scale, 29.842f * scale, 13.154f * scale, 31.961f * scale, 15.039f * scale)
                lineTo(37.618f * scale, 9.382f * scale)
                cubicTo(34.046f * scale, 6.053f * scale, 29.268f * scale, 4f * scale, 24f * scale, 4f * scale)
                cubicTo(16.318f * scale, 4f * scale, 9.656f * scale, 8.337f * scale, 6.306f * scale, 14.691f * scale)
                close()
            },
            color = Color(0xFFEA4335)
        )

        // Yellow (Left segment)
        drawPath(
            path = Path().apply {
                moveTo(12.877f * scale, 19.51f * scale)
                lineTo(6.306f * scale, 14.691f * scale)
                cubicTo(4.97f * scale, 17.39f * scale, 4.182f * scale, 20.455f * scale, 4.182f * scale, 23.727f * scale)
                cubicTo(4.182f * scale, 27.318f * scale, 5.091f * scale, 30.682f * scale, 6.626f * scale, 33.627f * scale)
                lineTo(12.697f * scale, 28f * scale)
                cubicTo(12.239f * scale, 26.764f * scale, 12f * scale, 25.418f * scale, 12f * scale, 24f * scale)
                cubicTo(12f * scale, 22.41f * scale, 12.321f * scale, 20.897f * scale, 12.877f * scale, 19.51f * scale)
                close()
            },
            color = Color(0xFFFBBC05)
        )
    }
}
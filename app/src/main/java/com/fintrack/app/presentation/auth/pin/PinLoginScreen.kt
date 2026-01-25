package com.fintrack.app.presentation.auth.pin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun PinLoginScreen(
    onPinVerified: () -> Unit,
    onUseBiometrics: () -> Unit,
    onForgotPin: () -> Unit,
    isBiometricAvailable: Boolean = true,
    viewModel: PinLoginViewModel = hiltViewModel()
) {
    // 1. Observe UI State
    val uiState by viewModel.uiState.collectAsState()

    val haptic = LocalHapticFeedback.current

    // Animation State for Error Shake
    val offsetX = remember { Animatable(0f) }

    // 2. Handle One-time Events (Success/Error Side Effects)
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is PinLoginEvent.Success -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPinVerified()
                }
                is PinLoginEvent.Error -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    // Trigger Shake Animation
                    launch {
                        repeat(3) {
                            offsetX.animateTo(10f, animationSpec = tween(50))
                            offsetX.animateTo(-10f, animationSpec = tween(50))
                        }
                        offsetX.animateTo(0f)
                    }
                }
            }
        }
    }

    // Wrapper to handle local UI feedback (Haptics) + ViewModel Logic
    fun onDigitClick(digit: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        viewModel.onDigitEntered(digit)
    }

    fun onDeleteClick() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        viewModel.onBackspace()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .offset(x = offsetX.value.dp), // Apply shake offset here
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // --- Header Section ---
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            // Dynamic Text based on ViewModel state
            Text(
                text = if (uiState.isError) "Incorrect PIN. Try again." else "Enter your PIN to access your wallet",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (uiState.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // --- PIN Dots ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fixed length of 4 based on your ViewModel logic
                repeat(4) { index ->
                    PinDot(
                        isActive = index < uiState.enteredPin.length,
                        isError = uiState.isError
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- Keypad ---
            Column(
                modifier = Modifier.widthIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    KeypadButton("1") { onDigitClick("1") }
                    KeypadButton("2") { onDigitClick("2") }
                    KeypadButton("3") { onDigitClick("3") }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    KeypadButton("4") { onDigitClick("4") }
                    KeypadButton("5") { onDigitClick("5") }
                    KeypadButton("6") { onDigitClick("6") }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    KeypadButton("7") { onDigitClick("7") }
                    KeypadButton("8") { onDigitClick("8") }
                    KeypadButton("9") { onDigitClick("9") }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (isBiometricAvailable) {
                        ActionButton(Icons.Default.Fingerprint, onUseBiometrics)
                    } else {
                        Spacer(modifier = Modifier.size(80.dp))
                    }
                    KeypadButton("0") { onDigitClick("0") }
                    ActionButton(Icons.AutoMirrored.Filled.Backspace, { onDeleteClick() }, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Footer ---
            TextButton(onClick = onForgotPin) {
                Text(
                    text = "Forgot PIN?",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- Components (Kept exactly as provided) ---

@Composable
fun PinDot(isActive: Boolean, isError: Boolean) {
    val color = when {
        isError -> MaterialTheme.colorScheme.error
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(if (isActive || isError) color else Color.Transparent)
            .border(
                width = if (isActive || isError) 0.dp else 1.5.dp,
                color = color,
                shape = CircleShape
            )
    )
}

@Composable
fun KeypadButton(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.onSurface),
                onClick = onClick
            )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.onSurface),
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(32.dp)
        )
    }
}
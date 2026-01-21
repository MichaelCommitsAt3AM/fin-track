package com.example.fintrack.presentation.settings.mpesa

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fintrack.presentation.settings.SettingsItem
import com.example.fintrack.presentation.settings.SettingsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpesaSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMerchantMapping: () -> Unit,
    viewModel: MpesaSettingsViewModel = hiltViewModel()
) {
    val isRealTimeEnabled by viewModel.isRealTimeEnabled.collectAsState()
    val lookbackPeriod by viewModel.lookbackPeriodMonths.collectAsState()

    var showLookbackDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    if (showLookbackDialog) {
        LookbackPeriodDialog(
            currentMonths = lookbackPeriod,
            onDismiss = { showLookbackDialog = false },
            onSelect = { months ->
                viewModel.setLookbackPeriod(months)
                showLookbackDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete All M-Pesa Data?") },
            text = { Text("This will permanently delete all parsed M-Pesa transactions from the device. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllData()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Onboarding?") },
            text = { Text("This will force the app to show the M-Pesa onboarding flow again on next launch. Data will NOT be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetOnboarding()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("M-Pesa Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Configuration
            SettingsSection(title = "Configuration") {
                SettingsItem(
                    icon = Icons.Default.Sync,
                    title = "Real-time Scanning",
                    trailingText = if (isRealTimeEnabled) "On" else "Off",
                    hasToggle = true,
                    isToggleChecked = isRealTimeEnabled,
                    onToggleChange = { viewModel.setRealTimeEnabled(it) }
                )
                
                // Divider (manual or from wrapper if consistent, using Spacer for now if needed, but SettingsSection handles container)
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SettingsItem(
                    icon = Icons.Default.History,
                    title = "Lookback Period",
                    trailingText = "$lookbackPeriod Months",
                    onClick = { showLookbackDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Smart Features
            SettingsSection(title = "Smart Features") {
                SettingsItem(
                    icon = Icons.Default.Category,
                    title = "Merchant Categories",
                    trailingText = "Map merchants",
                    onClick = onNavigateToMerchantMapping
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Maintenance
            SettingsSection(title = "Maintenance") {
                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = "Re-scan Inbox",
                    trailingText = "Safe",
                    onClick = { viewModel.triggerRescan() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Danger Zone
            Text(
                text = "DANGER ZONE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp)
            ) {
                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Reset Onboarding")
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Delete All M-Pesa Data")
                }
            }
        }
    }
}

@Composable
fun LookbackPeriodDialog(
    currentMonths: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val options = listOf(1, 3, 6, 12)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Lookback Period") },
        text = {
            Column {
                options.forEach { months ->
                    TextButton(
                        onClick = { onSelect(months) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "$months Months",
                            fontWeight = if (currentMonths == months) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

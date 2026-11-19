package com.fintrack.app.presentation.settings.mpesa

import com.fintrack.app.core.domain.model.LookbackPeriod
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.work.WorkInfo
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.ui.Alignment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.app.presentation.settings.SettingsItem
import com.fintrack.app.presentation.settings.SettingsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpesaSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMerchantMapping: () -> Unit,
    onNavigateToBackup: () -> Unit = {},
    viewModel: MpesaSettingsViewModel = hiltViewModel()
) {
    val backupStatus by viewModel.backupStatus.collectAsState()
    val isRealTimeEnabled by viewModel.isRealTimeEnabled.collectAsState()
    val lookbackPeriod by viewModel.lookbackPeriod.collectAsState()

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showLookbackSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    
    // Rescan Confirmation
    var pendingLookback by remember { mutableStateOf<LookbackPeriod?>(null) }

    if (showLookbackSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showLookbackSheet = false
                pendingLookback = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                if (pendingLookback == null) {
                    // Selection view
                    Text(
                        text = "Lookback Period",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                    )

                    LookbackPeriod.entries.forEach { option ->
                        val label = if (option == LookbackPeriod.DEFAULT) "${option.label} (recommended)" else option.label
                        val isSelected = lookbackPeriod == option

                        Surface(
                            onClick = {
                                if (lookbackPeriod != option) {
                                    pendingLookback = option
                                } else {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            showLookbackSheet = false
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    // Confirmation view
                    Text(
                        text = "Change Lookback Period?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Rescan Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = buildString {
                                    append("Changing the lookback period requires rescanning your SMS inbox to find transactions.")
                                    if ((pendingLookback?.months ?: 0) > LookbackPeriod.ONE_YEAR.months || pendingLookback == LookbackPeriod.ALL_TIME) {
                                        append(" Scanning a longer history can take a few minutes; it runs in the background.")
                                    } else {
                                        append(" This may take a moment.")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                pendingLookback = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                pendingLookback?.let { period ->
                                    viewModel.changeLookbackPeriodAndRescan(period)
                                    pendingLookback = null
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        showLookbackSheet = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Change & Rescan")
                        }
                    }
                }
            }
        }
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

    // --- Sync Progress ---
    val syncWorkInfo by viewModel.syncWorkInfo.collectAsState()
    var showSyncSheet by remember { mutableStateOf(false) }

    // Show sheet when sync starts
    LaunchedEffect(syncWorkInfo) {
        syncWorkInfo?.let { info ->
            if (info.state != WorkInfo.State.SUCCEEDED && 
                info.state != WorkInfo.State.FAILED && 
                info.state != WorkInfo.State.CANCELLED) {
                showSyncSheet = true
            }
        }
    }

    if (showSyncSheet && syncWorkInfo != null) {
        SyncProgressSheet(
            workInfo = syncWorkInfo!!,
            onDismiss = {
                showSyncSheet = false
                viewModel.clearSyncState()
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
                    trailingText = lookbackPeriod.label,
                    onClick = { showLookbackSheet = true }
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

            // Cloud backup (opt-in, end-to-end encrypted)
            SettingsSection(title = "Backup") {
                SettingsItem(
                    icon = Icons.Default.Sync,
                    title = "Cloud backup",
                    trailingText = when {
                        backupStatus.availability == com.fintrack.app.core.backup.BackupAvailability.NEEDS_ACCOUNT -> "Requires an account"
                        backupStatus.enabled -> "On"
                        else -> "Off"
                    },
                    onClick = onNavigateToBackup
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncProgressSheet(
    workInfo: WorkInfo,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { 
            // Only allow dismiss if finished
            if (workInfo.state.isFinished) {
                onDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            if (workInfo.state.isFinished) BottomSheetDefaults.DragHandle() else Spacer(Modifier.height(24.dp))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                // Success
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50), // Success green color
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sync Complete",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your transactions have been updated.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            } else if (workInfo.state == WorkInfo.State.FAILED) {
                 // Failure
                Icon(
                    imageVector = Icons.Default.CleaningServices, // Placeholder for error icon if cleaner not appropriate
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sync Failed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                 Text(
                    text = "Something went wrong. Please try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            } else {
                // Running / Enqueued
                Text(
                    text = "Syncing Transactions...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(24.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                     androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scanning messages & handling unmapped merchants...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}



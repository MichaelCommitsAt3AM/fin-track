package com.fintrack.app.presentation.settings.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    // Flavor-specific extra sections (the personal edition adds its M-Pesa backup section)
    extraSections: @Composable () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Last updated: January 2026",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            PrivacySection(
                title = "1. Introduction",
                content = "Welcome to FinTrack. We value your privacy above all else. We want to be absolutely clear: we do not use, sell, or analyze your personal data. You can use FinTrack without an account, in which case your data stays on your phone. If you create an account and keep Cloud sync on, your data is uploaded only to give you a backup and to let you switch between devices without losing your information."
            )

            PrivacySection(
                title = "2. Data We Collect",
                content = "The information you enter into the app (such as transactions, budgets, and goals) is stored on your phone. If you have an account and Cloud sync is on, it is also stored in the cloud together with your authentication details. Without an account, or with Cloud sync off, nothing is uploaded."
            )

            PrivacySection(
                title = "3. How We Use Your Data",
                content = "Your data is used strictly for the following purposes:\n\n• **Synchronization:** To ensure your data is available on all your logged-in devices.\n• **Backup:** To keep your data safe in case you lose your device or reinstall the app.\n\nWe do not use your data for marketing, analytics, or any other purpose. It remains yours and is only stored to serve you."
            )

            PrivacySection(
                title = "4. Data Security",
                content = "We employ industry-standard security measures to protect your data during transmission and storage. Your information is strictly accessible only to you through your secure login credentials."
            )

            PrivacySection(
                title = "5. Your Legal Rights",
                content = "You retain full ownership of your data. When you turn Cloud sync off you can choose to delete your cloud copy; your data on the phone is not affected. You can also request the deletion of your account and all associated data at any time."
            )
            
            extraSections()

            PrivacySection(
                title = "6. Contact Us",
                content = "If you have any questions about this privacy policy or our privacy practices, please contact us."
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
internal fun PrivacySection(
    title: String,
    content: String
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
        )
    }
}

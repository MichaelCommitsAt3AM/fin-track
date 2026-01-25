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
                content = "Welcome to FinTrack. We value your privacy above all else. We want to be absolutely clear: we do not use, sell, or analyze your personal data. The only reason your data is uploaded to our servers is to provide you with a backup and to allow you to seamlessly switch between devices without losing your information."
            )

            PrivacySection(
                title = "2. Data We Collect",
                content = "We collect the information you enter into the app (such as transactions, budgets, and goals) and your authentication details. This data is essential for the functionality of the app."
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
                content = "You retain full ownership of your data. You have the right to request the deletion of your account and all associated data at any time."
            )
            
            PrivacySection(
                title = "6. Contact Us",
                content = "If you have any questions about this privacy policy or our privacy practices, please contact us."
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PrivacySection(
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

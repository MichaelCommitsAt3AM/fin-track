package com.example.fintrack.presentation.settings.support

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
fun TermsOfServiceScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Terms of Service",
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

            TermsSection(
                title = "1. Acceptance of Terms",
                content = "By accessing and using FinTrack, you agree to comply with and be bound by these Terms of Service. If you do not agree to these terms, please do not use our application."
            )

            TermsSection(
                title = "2. Use of Service",
                content = "FinTrack is a personal finance management tool designed to help you track your income, expenses, and savings. You agree to use the service only for lawful purposes and in accordance with these Terms."
            )

            TermsSection(
                title = "3. User Accounts & Security",
                content = "You are responsible for maintaining the confidentiality of your account credentials and for all activities that occur under your account. You agree to notify us immediately of any unauthorized use of your account."
            )

            TermsSection(
                title = "4. Your Data & Ownership",
                content = "You retain full ownership of all data you enter into FinTrack. We claim no intellectual property rights over the material you provide. Your data is yours, and you are free to export or delete it at any time."
            )

            TermsSection(
                title = "5. Service Availability",
                content = "While we strive to provide reliable synchronization and backup services, we cannot guarantee that the service will be available 100% of the time without interruption. We are not liable for any loss of data caused by service interruptions or failures."
            )

            TermsSection(
                title = "6. Limitation of Liability",
                content = "To the fullest extent permitted by law, FinTrack shall not be liable for any indirect, incidental, special, consequential, or punitive damages, or any loss of profits or revenues, whether incurred directly or indirectly."
            )
            
            TermsSection(
                title = "7. Termination",
                content = "We reserve the right to terminate or suspend your account immediately, without prior notice or liability, for any reason whatsoever, including without limitation if you breach the Terms."
            )

            TermsSection(
                title = "8. Changes to Terms",
                content = "We reserve the right, at our sole discretion, to modify or replace these Terms at any time. What constitutes a material change will be determined at our sole discretion."
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TermsSection(
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

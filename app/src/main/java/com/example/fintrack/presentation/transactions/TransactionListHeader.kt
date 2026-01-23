package com.example.fintrack.presentation.transactions

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TransactionListHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    isSourceFilterExpanded: Boolean,
    toggleSourceFilterExpanded: () -> Unit,
    selectedSourceFilter: String,
    onSourceFilterSelected: (String) -> Unit
) {
    Column {
        // --- Search & Filter Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search transactions", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(50), // Fully rounded
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Filter Tabs (All / Income / Expense) ---
        TransactionListSegmentedControl(
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterSelected
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- View More Button ---
        ViewMoreButton(
            isExpanded = isSourceFilterExpanded,
            onClick = toggleSourceFilterExpanded
        )

        // --- Secondary Source Filter (Manual / M-Pesa) ---
        AnimatedVisibility(
            visible = isSourceFilterExpanded,
            enter = expandVertically(
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                )
            ) + fadeIn(
                animationSpec = tween(300)
            ),
            exit = shrinkVertically(
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                )
            ) + fadeOut(
                animationSpec = tween(200)
            )
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                TransactionSourceSegmentedControl(
                    selectedSource = selectedSourceFilter,
                    onSourceSelected = onSourceFilterSelected
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

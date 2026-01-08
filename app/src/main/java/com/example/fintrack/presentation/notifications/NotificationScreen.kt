package com.example.fintrack.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// -----------------------------------------------------------------------------
// 1. Static Data & Colors Hoisted Out of Recomposition
// -----------------------------------------------------------------------------

private val NotificationPrimary = Color(0xFF2ECC71)
private val SpendingBills = Color(0xFFE74C3C)
private val SpendingTransport = Color(0xFF3498DB)
private val SpendingFood = Color(0xFFF39C12)
private val SpendingShopping = Color(0xFF9B59B6)

private val FILTERS = listOf("All", "Alerts", "Bills", "Suggestions", "Activity")

// Immutable Data Model
@Immutable // Hints Compose that this class is thread-safe and stable
data class NotificationItemUiModel(
    val id: String,
    val title: String,
    val message: AnnotatedString,
    val timeAgo: String,
    val icon: ImageVector,
    val iconColor: Color,
    val isRead: Boolean
)

// Static Mock Data
private val NEW_NOTIFICATIONS = listOf(
    NotificationItemUiModel(
        id = "1",
        title = "Budget Exceeded",
        message = buildAnnotatedString {
            append("You've exceeded your ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Shopping") }
            append(" budget by $45.20.")
        },
        timeAgo = "2m ago",
        icon = Icons.Default.Warning,
        iconColor = SpendingBills,
        isRead = false
    ),
    NotificationItemUiModel(
        id = "2",
        title = "Upcoming Bill",
        message = buildAnnotatedString {
            append("Your internet bill of $89.00 is due tomorrow.")
        },
        timeAgo = "1h ago",
        icon = Icons.Default.ReceiptLong,
        iconColor = SpendingTransport,
        isRead = false
    )
)

private val EARLIER_NOTIFICATIONS = listOf(
    NotificationItemUiModel(
        id = "3",
        title = "Smart Savings",
        message = buildAnnotatedString {
            append("You spent 15% less on groceries this week compared to last week. Great job!")
        },
        timeAgo = "5h ago",
        icon = Icons.Default.Lightbulb,
        iconColor = SpendingFood,
        isRead = true
    ),
    NotificationItemUiModel(
        id = "4",
        title = "Funds Added",
        message = buildAnnotatedString {
            append("Top-up of $500.00 via Bank Transfer was successful.")
        },
        timeAgo = "1d ago",
        icon = Icons.Default.AccountBalanceWallet,
        iconColor = NotificationPrimary,
        isRead = true
    ),
    NotificationItemUiModel(
        id = "5",
        title = "New Device Login",
        message = buildAnnotatedString {
            append("Login detected from iPhone 14 Pro in New York, USA.")
        },
        timeAgo = "2d ago",
        icon = Icons.Default.Security,
        iconColor = Color.Gray,
        isRead = true
    )
)

// -----------------------------------------------------------------------------
// 2. Main Screen Composable
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }

    // Remember scroll state for the horizontal filter row
    val filterScrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleMedium,
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
                actions = {
                    TextButton(onClick = { /* Clear All Logic */ }) {
                        Text(
                            text = "Clear all",
                            color = NotificationPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            // Consolidated spacing: Removes need for Spacer() inside items
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // 3. Optimized Filter Row: Standard Row + horizontalScroll
            item(contentType = "Filters") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .horizontalScroll(filterScrollState)
                ) {
                    FILTERS.forEach { filter ->
                        FilterChip(
                            selected = filter == selectedFilter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NotificationPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filter == selectedFilter,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            shape = CircleShape
                        )
                    }
                }
            }

            // Section: New
            item(contentType = "Header") {
                SectionHeader(title = "NEW", actionText = "Mark all as read")
            }

            // 4. Stable Keys for efficient diffing
            items(
                items = NEW_NOTIFICATIONS,
                key = { it.id },
                contentType = { "Notification" }
            ) { notification ->
                NotificationItem(notification)
            }

            // Section: Earlier
            item(contentType = "Header") {
                SectionHeader(title = "EARLIER", modifier = Modifier.padding(top = 8.dp))
            }

            items(
                items = EARLIER_NOTIFICATIONS,
                key = { it.id },
                contentType = { "Notification" }
            ) { notification ->
                NotificationItem(notification)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 3. Extracted Components (Skippable & Reusable)
// -----------------------------------------------------------------------------

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 1.sp
        )
        if (actionText != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = NotificationPrimary,
                modifier = Modifier.clickable { /* Action Logic */ }
            )
        }
    }
}

@Composable
fun NotificationItem(notification: NotificationItemUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { /* Open Detail */ }
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(notification.iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = notification.icon,
                contentDescription = null,
                tint = notification.iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.timeAgo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!notification.isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SpendingBills)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}
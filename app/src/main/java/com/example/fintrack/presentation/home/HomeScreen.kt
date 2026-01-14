package com.example.fintrack.presentation.home


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fintrack.presentation.ui.theme.FinTrackGreen
import com.example.fintrack.presentation.navigation.AppRoutes
import com.example.fintrack.core.ui.components.SyncStatusOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    paddingValues: PaddingValues, // These usually come from Scaffold
    onOpenDrawer: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val spendingCategories by viewModel.spendingCategories.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currency by viewModel.currencyPreference.collectAsState()
    val weeklySpending by viewModel.weeklySpending.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    // Root Box is essential for Overlay behavior
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // LAYER 1: Main Content
        PullToRefreshBox(
            isRefreshing = syncStatus is SyncStatus.Syncing,
            onRefresh = { viewModel.triggerManualSync() },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Apply Scaffold padding here
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // REMOVED: SyncStatusBanner from here (item block deleted)

                item {
                    HomeHeader(
                        user = currentUser,
                        onProfileClick = onOpenDrawer,
                        onNotificationClick = { navController.navigate(AppRoutes.Notifications.route) }
                    )
                }

                item {
                    WeeklySpendingCard(
                        amountSpent = weeklySpending.first,
                        lastWeekSpent = weeklySpending.second,
                        currencySymbol = currency.symbol
                    )
                }

                item {
                    SpendingSection(
                        categories = spendingCategories,
                        isEmpty = spendingCategories.isEmpty()
                    )
                }

                item {
                    TransactionsSection(
                        transactions = recentTransactions,
                        currencySymbol = currency.symbol,
                        onViewAllClick = {
                            navController.navigate(AppRoutes.TransactionList.route)
                        }
                    )
                }
            }
        }

        // LAYER 2: Overlay Banner
        // We place it at the top. It is NOT inside the LazyColumn.
        // It ignores paddingValues.top so it can slide in from the very edge of the screen.
        SyncStatusOverlay(
            status = syncStatus,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}



@Composable
fun HomeHeader(
    user: UserUiModel?,
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Clickable avatar
            Image(
                painter = painterResource(
                    id = com.example.fintrack.presentation.utils.getAvatarResource(user?.avatarId ?: 1)
                ),
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() }
            )

            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = user?.fullName?.substringBefore(" ") ?: "Loading...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        IconButton(
            onClick = onNotificationClick,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WeeklySpendingCard(
    amountSpent: Double = 433.62,
    lastWeekSpent: Double = 377.06,
    currencySymbol: String = "Ksh"
) {
    val difference = amountSpent - lastWeekSpent
    val percentageChange = if (lastWeekSpent > 0) {
        ((difference / lastWeekSpent) * 100).toInt()
    } else 0
    val isUp = difference > 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = linearGradient(
                            colors = listOf(
                                Color(0xFF5B7FFF),
                                Color(0xFF9B59B6)
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .offset(x = 250.dp, y = (-48).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                )

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .offset(x = (-32).dp, y = 100.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Weekly Spending",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD8B4FE)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = currencySymbol,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "%,.2f".format(amountSpent),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 48.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "vs. last week ($currencySymbol %,.2f)".format(lastWeekSpent),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD8B4FE)
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isUp) Color(0xFFFCA5A5) else Color(0xFFA7F3D0),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${kotlin.math.abs(percentageChange)}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isUp) Color(0xFFFCA5A5) else Color(0xFFA7F3D0)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingSection(
    categories: List<SpendingCategoryUiModel>,
    isEmpty: Boolean
) {
    Column(modifier = Modifier.padding(top = 28.dp)) {
        SectionHeader(title = "Spending", onViewAllClick = null)

        if (isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No spending data this month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(216.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp)
            ) {
                items(categories) { category ->
                    SpendingItem(category)
                }
            }
        }
    }
}

@Composable
fun SpendingItem(category: SpendingCategoryUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(category.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.name,
                    tint = category.color
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = category.amount,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun TransactionsSection(
    transactions: List<TransactionUiModel>,
    currencySymbol: String,
    onViewAllClick: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 28.dp)) {
        SectionHeader(
            title = "Recent Transactions",
            onViewAllClick = onViewAllClick
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            transactions.forEach { transaction ->
                TransactionItem(transaction, currencySymbol)
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionUiModel, currencySymbol: String) {
    val amountColor = if (transaction.amount > 0) FinTrackGreen else MaterialTheme.colorScheme.error
    val amountString = if (transaction.amount > 0) {
        "+$currencySymbol ${"%.2f".format(transaction.amount)}"
    } else {
        "-$currencySymbol ${"%.2f".format(kotlin.math.abs(transaction.amount))}"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(amountColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = transaction.icon,
                    contentDescription = transaction.name,
                    tint = amountColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = transaction.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = amountString,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, onViewAllClick: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (onViewAllClick != null) {
            ClickableText(
                text = AnnotatedString("View All"),
                onClick = { onViewAllClick() },
                style = TextStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
fun ProfileDrawerContent(
    user: UserUiModel?,
    onCloseDrawer: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxHeight()
        ) {
            // Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar with border
                Box {
                    Image(
                        painter = painterResource(
                            id = com.example.fintrack.presentation.utils.getAvatarResource(user?.avatarId ?: 1)
                        ),
                        contentDescription = "User Avatar Large",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(4.dp)
                    )
                    // Online indicator
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = user?.fullName ?: "User Name",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = user?.email ?: "user@example.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            HorizontalDivider()
            
            // Menu Items
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                DrawerMenuItem(
                    icon = Icons.Default.Person,
                    text = "My Profile",
                    onClick = onNavigateToProfile
                )
                
                DrawerMenuItem(
                    icon = Icons.Default.Settings,
                    text = "Settings",
                    onClick = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Helper data class for tuple
private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

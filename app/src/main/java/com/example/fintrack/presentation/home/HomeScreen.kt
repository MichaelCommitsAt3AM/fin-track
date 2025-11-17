package com.example.fintrack.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fintrack.presentation.ui.theme.FinTrackGreen
import com.example.fintrack.presentation.ui.theme.SpendingBills
import com.example.fintrack.presentation.ui.theme.SpendingFood
import com.example.fintrack.presentation.ui.theme.SpendingShopping
import com.example.fintrack.presentation.ui.theme.SpendingTransport
import com.example.fintrack.presentation.navigation.AppRoutes

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {

    val recentTransactions by viewModel.recentTransactions.collectAsState()

    // Dummy data
    val spendingCategories = listOf(
        SpendingCategory("Food", "Ksh 345.12", Icons.Default.Restaurant, SpendingFood),
        SpendingCategory("Transport", "Ksh 88.50", Icons.Default.DirectionsBus, SpendingTransport),
        SpendingCategory("Shopping", "Ksh 1,204.99", Icons.Default.ShoppingBag, SpendingShopping),
        SpendingCategory("Bills", "Ksh 450.00", Icons.Default.ReceiptLong, SpendingBills)
    )
//    val transactions = listOf(
//        Transaction("Amazon Purchase", "Today", -78.99, Icons.Default.ShoppingCart),
//        Transaction("Salary", "Yesterday", 2500.00, Icons.Default.Paid),
//        Transaction("Starbucks", "2 days ago", -6.50, Icons.Default.Coffee)
//    )

    // FIX: Removed Scaffold entirely.
    // The padding for the Bottom Bar is handled by MainActivity -> NavGraph.
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp), // Apply horizontal padding
        // Add a little bottom padding so the last item isn't flush with the bottom bar
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item { HomeHeader() }
        item { BalanceCard() }
        item { SpendingSection(spendingCategories) }
        item { TransactionsSection(
            transactions = recentTransactions,
            onViewAllClick = {
                navController.navigate(AppRoutes.TransactionList.route)
            }
        ) }
    }
}

@Composable
fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://placehold.co/100x100/2ECC71/FFFFFF?text=JD") // Placeholder
                    .crossfade(true)
                    .build(),
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "John Doe", // Hardcoded
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        IconButton(
            onClick = { /* TODO */ },
            modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerLow)
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
fun BalanceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Text(
                text = "Ksh 12,345.67", // Updated to Ksh
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BalanceAction(text = "Add Money", icon = Icons.Default.Add, modifier = Modifier.weight(1f))
                BalanceAction(text = "Send", icon = Icons.Default.ArrowUpward, modifier = Modifier.weight(1f))
                BalanceAction(text = "Transfer", icon = Icons.Default.SyncAlt, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun BalanceAction(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Button(
        onClick = { /* TODO */ },
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.2f),
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SpendingSection(categories: List<SpendingCategory>) {
    Column(modifier = Modifier.padding(top = 28.dp)) {
        SectionHeader(title = "Spending", onViewAllClick = { /* TODO */ })
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(216.dp), // Fixed height for 2 rows
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

@Composable
fun SpendingItem(category: SpendingCategory) {
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
                Icon(imageVector = category.icon, contentDescription = category.name, tint = category.color)
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
    onViewAllClick: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 28.dp)) {
        SectionHeader(
            title = "Recent Transactions",
            onViewAllClick =  onViewAllClick )
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            transactions.forEach { transaction ->
                TransactionItem(transaction)
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionUiModel) {
    val amountColor = if (transaction.amount > 0) FinTrackGreen else MaterialTheme.colorScheme.error
    val amountString = if (transaction.amount > 0) "+Ksh ${"%.2f".format(transaction.amount)}" else "-Ksh ${"%.2f".format(kotlin.math.abs(transaction.amount))}"

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
                Icon(imageVector = transaction.icon, contentDescription = transaction.name, tint = amountColor)
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
fun SectionHeader(title: String, onViewAllClick: () -> Unit) {
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
        ClickableText(
            text = AnnotatedString("View All"),
            onClick = { offset -> onViewAllClick() },
            style = TextStyle(
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

// Data classes for dummy data
data class SpendingCategory(val name: String, val amount: String, val icon: ImageVector, val color: Color)
data class Transaction(val name: String, val date: String, val amount: Double, val icon: ImageVector)
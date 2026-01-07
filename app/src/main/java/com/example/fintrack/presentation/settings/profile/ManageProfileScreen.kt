package com.example.fintrack.presentation.settings.profile

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
// IMPORTANT: Ensure this matches your package structure so R.drawable is found
import com.example.fintrack.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAvatarPicker by remember { mutableStateOf(false) }

    // Handle One-time Events (Success/Error toasts)
    LaunchedEffect(key1 = true) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShowSuccess -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is ProfileEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Manage Profile",
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
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                ),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = { viewModel.onEvent(ProfileUiEvent.OnSaveProfile) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Save Changes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // --- Profile Picture Section ---
            Box(
                modifier = Modifier.size(112.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                // Display current avatar using the SAFE helper function
                Image(
                    painter = painterResource(id = getAvatarResource(state.avatarId)),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )

                // Edit Button
                IconButton(
                    onClick = { showAvatarPicker = true },
                    modifier = Modifier
                        .size(36.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.background,
                            shape = CircleShape
                        )
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile Picture",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            // -------------------------------

            Spacer(modifier = Modifier.height(32.dp))

            // --- Form Fields ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ProfileTextField(
                    label = "Full Name",
                    value = state.fullName,
                    onValueChange = { viewModel.onEvent(ProfileUiEvent.OnFullNameChange(it)) },
                    placeholder = "Enter your full name"
                )

                ProfileTextField(
                    label = "Email Address",
                    value = state.email,
                    onValueChange = { /* Read-only */ },
                    placeholder = "email@example.com",
                    enabled = false
                )

                ProfileTextField(
                    label = "Phone Number",
                    value = state.phoneNumber,
                    onValueChange = { viewModel.onEvent(ProfileUiEvent.OnPhoneNumberChange(it)) },
                    placeholder = "Enter your phone number (optional)"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- Avatar Picker Dialog ---
    if (showAvatarPicker) {
        AvatarPickerDialog(
            availableAvatars = state.availableAvatars,
            selectedAvatarId = state.avatarId,
            onAvatarSelected = { avatarId ->
                viewModel.onEvent(ProfileUiEvent.OnAvatarChange(avatarId))
                showAvatarPicker = false
            },
            onDismiss = { showAvatarPicker = false }
        )
    }
}

@Composable
fun AvatarPickerDialog(
    availableAvatars: List<Int>,
    selectedAvatarId: Int,
    onAvatarSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Avatar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            // Container to limit height and ensure grid renders correctly inside Dialog
            Box(modifier = Modifier.heightIn(max = 350.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                ) {
                    items(availableAvatars) { avatarId ->
                        AvatarItem(
                            avatarId = avatarId,
                            isSelected = avatarId == selectedAvatarId,
                            onClick = { onAvatarSelected(avatarId) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
fun AvatarItem(
    avatarId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Dynamic styling based on selection
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Box(
        modifier = Modifier
            .aspectRatio(1f) // Ensures items are always square
            .clip(CircleShape)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = getAvatarResource(avatarId)),
            contentDescription = "Avatar $avatarId",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Checkmark overlay for selected avatar
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Maps the avatar ID (saved in Database) to the actual Drawable Resource ID.
 * This prevents crashes caused by looking up resources by string name.
 */
fun getAvatarResource(avatarId: Int): Int {
    return when (avatarId) {
        1 -> R.drawable.avatar_default
        2 -> R.drawable.avatar_female
        3 -> R.drawable.avatar_male
        4 -> R.drawable.avatar_grandpa
        5 -> R.drawable.avatar_girl
        else -> R.drawable.avatar_default // Safety fallback
    }
}
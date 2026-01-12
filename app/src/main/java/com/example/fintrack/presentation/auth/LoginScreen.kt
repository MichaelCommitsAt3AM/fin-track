package com.example.fintrack.presentation.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import com.example.fintrack.R
import com.example.fintrack.presentation.ui.theme.FinTrackGreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun LoginScreen(
    onNavigateToSetup: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToEmailVerification: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }

    // 1. Setup Google Sign-In Launcher (Same as RegistrationScreen)
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("LoginScreen", "Google Sign-In activity result received. ResultCode: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("LoginScreen", "Result OK, processing sign-in data")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("LoginScreen", "Account retrieved: ${account.email}, ID Token: ${account.idToken?.take(20)}...")
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                Log.d("LoginScreen", "Credential created, sending to ViewModel")
                viewModel.onEvent(AuthUiEvent.SignInWithGoogle(credential))
            } catch (e: ApiException) {
                Log.e("LoginScreen", "Google Sign-In ApiException: ${e.statusCode} - ${e.message}", e)
                Toast.makeText(
                    context,
                    "Google Sign-In failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            Log.w("LoginScreen", "Google Sign-In was canceled by user")
        } else {
            Log.e("LoginScreen", "Google Sign-In failed with result code: ${result.resultCode}")
        }
    }

    if (state.showEmailVerificationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = { Text("Verify your Email") },
            text = { Text("We have sent a verification link to ${state.email}. Please check your inbox and click the link before logging in.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(AuthUiEvent.ResendVerificationEmail) }) {
                    Text("Resend Email")
                }
            }
        )
    }

    // Listen for one-time events
    LaunchedEffect(key1 = true) {
        viewModel.authEvent.collect { event ->
            when (event) {
                is AuthEvent.NavigateToSetup -> onNavigateToSetup()
                is AuthEvent.NavigateToEmailVerification -> onNavigateToEmailVerification()
                AuthEvent.NavigateToLogin -> onNavigateToLogin()
                else -> {}
            }
        }
    }

    // Show error toast
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Logo
            Text(
                text = "FinTrack",
                style = MaterialTheme.typography.headlineMedium.copy(color = FinTrackGreen, fontSize = 36.sp),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Headline
            Text(
                text = "Welcome Back!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Log in to manage your finances.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email Field
            Text(
                text = "Email Address",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.onEvent(AuthUiEvent.EmailChanged(it)) },
                placeholder = { Text("Enter your email", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FinTrackGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            Text(
                text = "Password",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(AuthUiEvent.PasswordChanged(it)) },
                placeholder = { Text("Enter your password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = icon, contentDescription = "Toggle password visibility")
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FinTrackGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Forgot Password
            ClickableText(
                text = AnnotatedString("Forgot Password?"),
                onClick = { onNavigateToForgotPassword() },
                style = TextStyle(
                    color = FinTrackGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.onEvent(AuthUiEvent.SignIn) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinTrackGreen)
                ) {
                    Text("Log In", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Google Sign-In Button
                OutlinedButton(
                    onClick = {
                        Log.d("LoginScreen", "Google Sign-In button clicked")
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(context.getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        Log.d("LoginScreen", "Launching Google Sign-In intent")
                        googleLauncher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    // Google Logo (Drawn from SVG paths to avoid needing asset import)
                    GoogleIcon(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Login with Google",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Create Account Link
            Row {
                Text(
                    "New to FinTrack? ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ClickableText(
                    text = AnnotatedString("Create an account"),
                    onClick = { onNavigateToRegister() },
                    style = TextStyle(
                        color = FinTrackGreen,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                        fontSize = 16.sp
                    ),
                )
            }
        }
    }
}
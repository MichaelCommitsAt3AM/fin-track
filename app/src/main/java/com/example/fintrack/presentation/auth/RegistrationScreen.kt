package com.example.fintrack.presentation.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fintrack.core.util.AppLogger
import com.example.fintrack.BuildConfig
import com.example.fintrack.R
import com.example.fintrack.presentation.ui.theme.FinTrackGreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    onNavigateToHome: () -> Unit,
    onNavigateBackToLogin: () -> Unit,
    onNavigateToEmailVerification: () -> Unit,
    onNavigateToSetup: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val step by viewModel.registrationStep.collectAsState()
    val context = LocalContext.current

    // Listen for one-time events (navigation, toasts)
    LaunchedEffect(key1 = true) {
        viewModel.authEvent.collect { event ->
            when (event) {
                is AuthEvent.NavigateToHome -> onNavigateToHome()
                is AuthEvent.NavigateToEmailVerification -> onNavigateToEmailVerification()
                is AuthEvent.NavigateToSetup -> onNavigateToSetup()
                AuthEvent.NavigateToLogin -> onNavigateBackToLogin()
                else -> {}
            }
        }
    }

    // Show error toast if needed
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Crossfade(
                targetState = step,
                label = "RegistrationStep"
            ) { currentStep ->
                when (currentStep) {
                    RegistrationStep.Email -> RegisterEmailStep(
                        email = state.email,
                        isLoading = state.isLoading,
                        onEmailChange = { viewModel.onEvent(AuthUiEvent.EmailChanged(it)) },
                        onNext = { viewModel.onEvent(AuthUiEvent.CheckEmail) },
                        viewModel = viewModel
                    )
                    RegistrationStep.Password -> RegisterPasswordStep(
                        email = state.email,
                        password = state.password,
                        confirmPassword = state.confirmPassword,
                        isLoading = state.isLoading,
                        onPasswordChange = { viewModel.onEvent(AuthUiEvent.PasswordChanged(it)) },
                        onConfirmPasswordChange = { viewModel.onEvent(AuthUiEvent.ConfirmPasswordChanged(it)) },
                        onCreateAccount = { viewModel.onEvent(AuthUiEvent.SignUp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Back to login Link
            Row {
                Text(
                    "Already have an account? ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ClickableText(
                    text = AnnotatedString("Log In"),
                    onClick = { onNavigateBackToLogin() },
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

@Composable
fun RegisterEmailStep(
    email: String,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onNext: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Google Sign-In launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        AppLogger.d("RegistrationScreen", "Google Sign-In activity result received. ResultCode: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            AppLogger.d("RegistrationScreen", "Result OK, processing sign-in data")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                AppLogger.d("RegistrationScreen", "Account retrieved, ID Token: ${AppLogger.sanitizeToken(account.idToken)}")
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                AppLogger.d("RegistrationScreen", "Credential created, sending to ViewModel")
                viewModel.onEvent(AuthUiEvent.SignInWithGoogle(credential))
            } catch (e: ApiException) {
                AppLogger.e("RegistrationScreen", "Google Sign-In ApiException: ${e.statusCode}", e)
                Toast.makeText(
                    context,
                    "Google Sign-In failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            AppLogger.w("RegistrationScreen", "Google Sign-In cancelled with result code: ${result.resultCode}")
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Headline Text
        Text(
            text = "Create an Account",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Enter your email to get started.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email Field
        Text(
            text = "Email Address",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = {
                Text(
                    "Enter your email",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
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

        Spacer(modifier = Modifier.height(24.dp))

        // Next Button
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinTrackGreen)
            ) {
                Text("Next", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider(modifier = Modifier.fillMaxWidth(0.8f))

        Spacer(modifier = Modifier.height(24.dp))

        // Google Sign In Button with Logo
        OutlinedButton(
            onClick = {
                AppLogger.d("RegistrationScreen", "Google Sign-In button clicked")
                // Trigger Google Sign-In
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                AppLogger.d("RegistrationScreen", "Launching Google Sign-In intent")
                launcher.launch(googleSignInClient.signInIntent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            // Google Logo
            GoogleIcon(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Sign up with Google",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RegisterPasswordStep(
    email: String,
    password: String,
    confirmPassword: String,
    isLoading: Boolean,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onCreateAccount: () -> Unit,
    minPasswordLength: Int = 6
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val isPasswordValid = password.length >= minPasswordLength
    val isConfirmPasswordValid = confirmPassword.length >= minPasswordLength
    val doPasswordsMatch = password == confirmPassword

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Headline Text
        Text(
            text = "Create Password",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Creating account for: $email",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Password Field
        Text(
            text = "Password",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = {
                Text(
                    "Enter your password",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
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
            singleLine = true,
            isError = password.isNotEmpty() && !isPasswordValid
        )
        if (!isPasswordValid && password.isNotEmpty()) {
            Text(
                text = "Password must be at least $minPasswordLength characters",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Password Field
        Text(
            text = "Confirm Password",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = {
                Text(
                    "Confirm your password",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                val icon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(imageVector = icon, contentDescription = "Toggle password visibility")
                }
            },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FinTrackGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = confirmPassword.isNotEmpty() && (!isConfirmPasswordValid || !doPasswordsMatch)
        )

        // Length error message
        if (!isConfirmPasswordValid && confirmPassword.isNotEmpty()) {
            Text(
                text = "Confirm password must be at least $minPasswordLength characters",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp)
            )
        }
        // Mismatch error message
        else if (confirmPassword.isNotEmpty() && !doPasswordsMatch) {
            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Create Account Button
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = onCreateAccount,
                enabled = isPasswordValid && isConfirmPasswordValid && doPasswordsMatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinTrackGreen)
            ) {
                Text("Create Account", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
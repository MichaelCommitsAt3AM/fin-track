package com.fintrack.app.presentation.auth.pin

import com.fintrack.app.core.ui.components.SheetTone
import com.fintrack.app.core.ui.components.SheetActionKind
import com.fintrack.app.core.ui.components.SheetAction
import com.fintrack.app.core.ui.components.AppActionSheet
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.Icons
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.core.domain.repository.TransactionRepository
import com.fintrack.app.core.profile.LocalDataEraser
import com.fintrack.app.core.utils.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LocalForgotPinViewModel @Inject constructor(
    private val eraser: LocalDataEraser,
    private val transactionRepository: TransactionRepository,
    private val csvExporter: CsvExporter
) : ViewModel() {

    private val _exportEvent = MutableSharedFlow<Uri?>()
    val exportEvent: SharedFlow<Uri?> = _exportEvent.asSharedFlow()

    fun export() {
        viewModelScope.launch {
            val transactions = transactionRepository.getAllTransactionsPaged(Int.MAX_VALUE).first()
            _exportEvent.emit(withContext(Dispatchers.IO) { csvExporter.generateTransactionCsv(transactions) })
        }
    }

    fun eraseAndStartOver(onDone: () -> Unit) {
        viewModelScope.launch {
            eraser.eraseAll()
            onDone()
        }
    }
}

/**
 * "Forgot PIN?" for a user with no account to re-authenticate with. There is deliberately no way
 * to unlock the app from here: the options are to export the data (after the phone's own screen
 * lock or biometrics confirm it is the owner) or to erase everything and start over.
 */
@Composable
fun LocalForgotPinDialog(
    onDismiss: () -> Unit,
    onErased: () -> Unit,
    viewModel: LocalForgotPinViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var confirmErase by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.exportEvent.collect { uri ->
            if (uri != null) {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "FinTrack Data Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(share, "Export Data"))
            } else {
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun authenticateThenExport() {
        val activity = context as? FragmentActivity ?: return
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(activity).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(
                context,
                "Set a screen lock on this phone to export, or erase the app's data instead.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.export()
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm it's you")
                .setSubtitle("Use your phone's screen lock to export your data")
                .setAllowedAuthenticators(authenticators)
                .build()
        )
    }

    if (confirmErase) {
        AppActionSheet(
            title = "Erase all data?",
            icon = Icons.Default.DeleteForever,
            heading = "This can't be undone",
            message = "This deletes every transaction, budget and setting stored on this phone. " +
                "There is no cloud copy to restore from.",
            tone = SheetTone.DANGER,
            onDismiss = { confirmErase = false },
            actions = listOf(
                SheetAction("Cancel", SheetActionKind.SECONDARY) { confirmErase = false },
                SheetAction("Erase everything", SheetActionKind.DANGER) {
                    confirmErase = false
                    viewModel.eraseAndStartOver(onErased)
                }
            )
        )
        return
    }

    AppActionSheet(
        title = "Forgot your PIN?",
        icon = Icons.Default.LockReset,
        heading = "No account to unlock with",
        message = "Your data is only on this phone and you have no account to sign in with, so " +
            "FinTrack can't unlock it for you. You can export your transactions first, then " +
            "erase the app's data and start over.",
        onDismiss = onDismiss,
        actions = listOf(
            SheetAction("Export my data first") { authenticateThenExport() },
            SheetAction("Erase this app's data and start over", SheetActionKind.DANGER) { confirmErase = true },
            SheetAction("Back", SheetActionKind.TEXT, onClick = onDismiss)
        )
    )
}

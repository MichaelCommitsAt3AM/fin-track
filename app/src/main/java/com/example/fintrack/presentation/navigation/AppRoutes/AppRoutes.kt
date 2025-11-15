package com.example.fintrack.presentation.navigation

/**
 * A sealed class that defines all navigation routes in the app.
 * This provides a type-safe way to navigate.
 */
sealed class AppRoutes(val route: String) {
    // --- Auth Flow ---
    object Login : AppRoutes("login_screen")
    object Register : AppRoutes("registration_screen")
    object ForgotPassword : AppRoutes("forgot_password_screen")
    object VerifyEmail : AppRoutes("verify_email_screen")

    // --- Main App (Bottom Bar) Flow ---
    object Home : AppRoutes("home_screen")
    object Reports : AppRoutes("reports_screen")
    object Budgets : AppRoutes("budgets_screen")
    object Settings : AppRoutes("settings_screen")

    // --- Add Transaction (FAB) ---
    // We can add this route here for when we build it
    // object AddTransaction : AppRoutes("add_transaction_screen")
}
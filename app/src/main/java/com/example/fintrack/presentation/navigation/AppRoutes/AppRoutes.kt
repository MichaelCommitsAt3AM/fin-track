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

    // Settings feature
    object SettingsGraph : AppRoutes("settings_graph")
    object Settings : AppRoutes("settings_screen")
    object ManageCategories : AppRoutes("manage_categories_screen")
    object AddCategory : AppRoutes("add_category_screen")
    object EditCategory : AppRoutes("edit_category_screen/{categoryName}"){
        fun createRoute(name: String) = "edit_category_screen/$name"
    }


    // --- Add Transaction (FAB) ---
    object AddTransaction : AppRoutes("add_transaction_screen")

}
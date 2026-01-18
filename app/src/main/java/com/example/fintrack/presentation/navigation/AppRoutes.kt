package com.example.fintrack.presentation.navigation

/**
 * A sealed class that defines all navigation routes in the app. This provides a type-safe way to
 * navigate.
 */
sealed class AppRoutes(val route: String) {
    // --- Auth Flow ---
    object Login : AppRoutes("login_screen")
    object Register : AppRoutes("registration_screen")
    object ForgotPassword : AppRoutes("forgot_password_screen")
    object VerifyEmail : AppRoutes("verify_email_screen")
    object ProfileSetup : AppRoutes("profile_setup_screen")
    object Setup : AppRoutes("setup_screen")

    // --- Biometric & Security Flow ---
    object BiometricLock : AppRoutes("biometric_lock_screen")
    object BiometricSetup : AppRoutes("biometric_setup_screen") // For creating PIN/Enabling
    object FingerprintSetup : AppRoutes("fingerprint_setup_screen") // <--- NEW
    object PinLogin : AppRoutes("pin_login_screen")

    object ManageSignInMethods : AppRoutes("manage_sign_in_methods_screen")
    object SetPassword : AppRoutes("set_password_screen")

    // --- Main App (Bottom Bar) Flow ---
    object Home : AppRoutes("home_screen")
    object Reports : AppRoutes("reports_screen")
    object Goals : AppRoutes("goals_screen")
    object Budgets : AppRoutes("budgets_screen")
    object AddBudget : AppRoutes("add_budget_screen?categoryName={categoryName}&month={month}&year={year}") {
        fun createRoute(categoryName: String? = null, month: Int? = null, year: Int? = null): String {
            return if (categoryName != null && month != null && year != null) {
                "add_budget_screen?categoryName=$categoryName&month=$month&year=$year"
            } else {
                "add_budget_screen"
            }
        }
    }

    // Notifications in home screen
    object Notifications : AppRoutes("notifications_screen")

    // --- Goals Flow ---
    object AddDebt : AppRoutes("add_debt_screen")
    object AddSaving : AppRoutes("add_saving_screen")
    object ManageSaving : AppRoutes("manage_saving_screen/{savingId}") {
        fun createRoute(savingId: String) = "manage_saving_screen/$savingId"
    }
    object ManageDebt : AppRoutes("manage_debt_screen/{debtId}") {
        fun createRoute(debtId: String) = "manage_debt_screen/$debtId"
    }

    object ManageBudget : AppRoutes("manage_budget_screen/{categoryName}/{month}/{year}") {
        fun createRoute(categoryName: String, month: Int, year: Int) =
            "manage_budget_screen/$categoryName/$month/$year"
    }


    // Settings feature
    object SettingsGraph : AppRoutes("settings_graph")
    object Settings : AppRoutes("settings_screen")
    object ManageProfile : AppRoutes("manage_profile_screen")
    object NotificationSettings : AppRoutes("notification_settings_screen")
    object PaymentMethods : AppRoutes("payment_methods_screen")
    object PrivacyPolicy : AppRoutes("privacy_policy_screen")
    object TermsOfService : AppRoutes("terms_of_service_screen")
    object HelpSupport : AppRoutes("help_support_screen")

    object ManageCategories : AppRoutes("manage_categories_screen")
    object AddCategory : AppRoutes("add_category_screen")
    object EditCategory : AppRoutes("edit_category_screen/{categoryName}") {
        fun createRoute(name: String) = "edit_category_screen/$name"
    }

    // --- Add Transaction (FAB) ---
    object AddTransaction : AppRoutes("add_transaction_screen?transactionId={transactionId}") {
        fun createRoute(transactionId: String? = null) = if (transactionId != null) {
            "add_transaction_screen?transactionId=$transactionId"
        } else {
            "add_transaction_screen"
        }
    }
    object TransactionList : AppRoutes("transaction_list_screen")
    object ManageTransaction : AppRoutes("manage_transaction_screen/{transactionId}") {
        fun createRoute(transactionId: String) = "manage_transaction_screen/$transactionId"
    }
    object RecurringTransactions : AppRoutes("recurring_transactions_screen")

    object EditRecurringTransaction : AppRoutes("edit_recurring_transaction/{transactionId}") {
        fun createRoute(transactionId: String) = "edit_recurring_transaction/$transactionId"
    }
}
# FinTrack - Personal Finance Tracker 💰

FinTrack is a modern, feature-rich Android application built to help users manage their personal finances effectively. It tracks income, expenses, budgets, savings goals, and debts, providing valuable insights through interactive charts and real-time notifications.

## 📱 Features

### 🔐 Security & Authentication
- **Secure Login/Signup**: Email/Password authentication via Firebase Auth and Google Sign-In.
- **Biometric Lock**: Fingerprint transaction/app locking support for enhanced privacy.
- **PIN Protection**: 4-digit PIN fallback for quick access.

### 💸 Financial Tracking
- **Transaction Logging**: Add income and expense transactions with categories, dates, and notes.
- **Dynamic Dashboard**: Overview of current balance, recent transactions, and spending summaries.
- **Visual Analytics**: Interactive charts (Bar/Pie) powered by Vico to visualize spending patterns.

### 📊 Budget Management
- **Monthly Budgets**: Set spending limits for specific categories (e.g., Food, Transport).
- **Real-time Tracking**: Visual progress bars showing remaining budget.
- **Smart Alerts**: Notifications when you reach 70%, 80%, or 90% of your budget, or when exceeded.

### 🎯 Goal Setting
- **Savings Goals**: Create goals (e.g., "New Laptop", "Vacation") with target amounts and dates.
- **Progress Tracking**: Track contributions and visualize progress towards completion.
- **Milestone Alerts**: Get notified when you're 7 days away from a deadline or reach 100%.

### 💳 Debt Management
- **Debt Tracking**: Manage money you owe or are owed.
- **Payment Reminders**: Receive notifications 7, 3, and 1 day before a payment is due.
- **History**: Track payments and interest rates.

### 🔔 Smart Notifications
- **Background Workers**: Automated daily checks for budgets, upcoming bills, and goal deadlines using WorkManager.
- **Channels**: Categorized notifications for Budgets, Goals, and Debts.

## 🛠️ Tech Stack

- **Language**: Kotlin 100%
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Dagger Hilt
- **Local Database**: Room (SQLite)
- **Cloud Backend**: Firebase (Auth, Firestore)
- **Asynchronous**: Coroutines & Flow
- **Background Tasks**: WorkManager
- **Navigation**: Jetpack Navigation Compose
- **Charting**: Vico
- **Image Loading**: Coil

## 📂 Project Structure

The project follows a modular Clean Architecture approach:

```
com.example.fintrack
├── core                # Core utilities, data/domain layers, common components
│   ├── common          # Helper classes (NotificationHelper, Extensions)
│   ├── data            # Repositories, DAOs, Entities, Mappers
│   ├── di              # Hilt Modules (Network, Database, Repository)
│   ├── domain          # UseCases, Repository Interfaces, Models
│   ├── worker          # Background Workers (BudgetCheck, GoalCheck, DebtCheck)
│   └── utils           # Utility functions
├── presentation        # UI Layer (Screens, ViewModels, Components)
│   ├── auth            # Login, Registration, Biometrics
│   ├── dashboard       # Main Dashboard, Transaction lists
│   ├── goals           # Savings, Debts, Budgets screens
│   ├── settings        # App settings, Profile
│   ├── theme           # Compose Theme and Color definitions
│   └── navigation      # Navigation Graph and Routes
└── FinTrackApplication.kt # App Entry Point
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer.
- JDK 17.
- Firebase Account.

### Setup
1.  **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/fintrack.git
    ```
2.  **Firebase Configuration**:
    - Create a project in the [Firebase Console](https://console.firebase.google.com/).
    - Add an Android app with package `com.example.fintrack`.
    - Download `google-services.json` and place it in the `app/` directory.
    - Enable **Authentication** (Email/Password, Google).
    - Enable **Firestore Database**.
3.  **Build and Run**:
    - Open the project in Android Studio.
    - Sync Gradle.
    - Run on an Emulator or Physical Device (Android 8.0+ recommended).

## 🧪 Testing

The project includes:
- **Local Unit Tests**: For ViewModels and Utility logic.
- **Manual Verification**: See `walkthrough.md` for detailed verification steps for features like Notifications and Biometrics.

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

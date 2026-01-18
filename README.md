# FinTrack - Personal Finance Tracker 💰

FinTrack is a modern, feature-rich Android application built to help users manage their personal finances effectively. It tracks income, expenses, budgets, savings goals, and debts, providing valuable insights through interactive charts and real-time notifications.

## � Download

You can download the latest version of the app from the [GitHub Releases](https://github.com/MichaelCommitsAt3AM/fin-track/releases/)page.

## �📱 Features

### 🔐 Security & Authentication
- **Secure Login/Signup**: Email/Password authentication via Firebase Auth and Google Sign-In.
- **Smart Lock**: Biometric (Fingerprint) lock with automatic PIN fallback if biometrics are disabled or skipped.
- **PIN Protection**: Secure, hashed 4-digit PIN for quick and reliable access.

### 📡 Offline-First Access
- **Full Offline Support**: Continue adding transactions, goals, and debts even without internet.
- **Background Sync**: Data automatically syncs with the cloud (Firestore) as soon as you're back online using WorkManager.
- **Local Persistence**: Powered by Room Database for instant load times.

### 💸 Financial Tracking
- **Transaction Logging**: Add income and expense transactions with categories, dates, and notes.
- **Custom Payment Methods**: Manage your own payment types (e.g., Cash, M-Pesa, Card) with default selection.
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
- **Background Tasks**: WorkManager (Sync & Notifications)
- **Offline Sync**: Custom Offline-First Repository Pattern
- **Navigation**: Jetpack Navigation Compose
- **Charting**: Vico
- **Image Loading**: Coil



[//]: # (## 🚀 Getting Started)

[//]: # ()
[//]: # (### Prerequisites)

[//]: # (- Android Studio Ladybug or newer.)

[//]: # (- JDK 17.)

[//]: # (- Firebase Account.)

[//]: # ()
[//]: # (### Setup)

[//]: # (1.  **Clone the repository**:)

[//]: # (    ```bash)

[//]: # (    git clone https://github.com/MichaelCommitsAt3AM/fin-track.git)

[//]: # (    ```)

[//]: # (2.  **Firebase Configuration**:)

[//]: # (    - Create a project in the [Firebase Console]&#40;https://console.firebase.google.com/&#41;.)

[//]: # (    - Add an Android app with package `com.example.fintrack`.)

[//]: # (    - Download `google-services.json` and place it in the `app/` directory.)

[//]: # (    - Enable **Authentication** &#40;Email/Password, Google&#41;.)

[//]: # (    - Enable **Firestore Database**.)

[//]: # (3.  **Build and Run**:)

[//]: # (    - Open the project in Android Studio.)

[//]: # (    - Sync Gradle.)

[//]: # (    - Run on an Emulator or Physical Device &#40;Android 8.0+ recommended&#41;.)

[//]: # ()
[//]: # (## 🧪 Testing)

[//]: # ()
[//]: # (The project includes:)

[//]: # (- **Local Unit Tests**: For ViewModels and Utility logic.)

[//]: # (- **Manual Verification**: See `walkthrough.md` for detailed verification steps for features like Notifications and Biometrics.)

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

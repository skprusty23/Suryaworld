# Personal Tracker — Enterprise-Grade Secure Android Application

A modern, highly secure, production-ready Android application for managing personal life data, expenses, investments, credentials, and financial tracking in one place.

---

## Features

| Module | Description |
|---|---|
| 📄 Personal Documents Vault | Aadhaar, PAN, Passport, DL, Insurance, Vehicle, Birth Certificates |
| 🔐 Credentials Manager | Encrypted password vault with category filtering |
| 💰 Expense Tracker | Daily/Monthly expense tracking with charts & reports |
| 📈 Investment Tracker | LIC, Mutual Funds, SIP, FD, RD, Stocks, PPF |
| 📅 EMI Tracker | Loan EMIs with payment history & reminders |
| 🥇 Gold Investment | Track physical/digital gold purchases |
| 🎒 School Expenses | Child-wise academic year expense tracking |
| ✈️ Travel Tracker | Trip-based travel expense management |
| 👥 Group Expense Splitter | Vacation/group expense splitting |
| 📊 Dashboard | Smart analytics with charts and summaries |
| ☁️ OneDrive Backup | Encrypted cloud backup & restore |

---

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt (Dagger)
- **Database**: Room + SQLCipher (AES-256 encrypted)
- **Security**: Android Keystore + AES-GCM + Biometric Auth
- **Async**: Coroutines + StateFlow
- **Background Work**: WorkManager (Hilt-integrated)
- **Navigation**: Jetpack Navigation Compose
- **Charts**: Vico Charts
- **Cloud Backup**: Microsoft OneDrive via MSAL
- **Min SDK**: 26 (Android 8.0) | Target SDK: 35 (Android 15)

---

## Security Architecture

```
App Access
    ├── Biometric Authentication (fingerprint/face)
    └── 6-digit PIN (SHA-256 + salt, Keystore-encrypted)

Module-Level Security
    ├── Credentials Vault → separate PIN
    └── Investment Tracker → separate PIN

Database Security
    └── SQLCipher AES-256 encrypted Room database
        └── Key derived from Android Keystore (per-device, non-exportable)

Data Encryption
    └── All sensitive fields → AES/GCM/NoPadding via Android Keystore
        ├── Passwords in Credentials Vault
        └── Backup files before cloud upload

Device Security
    ├── No Android auto-backup (disabled in manifest)
    ├── Screenshot prevention on sensitive screens
    └── Backup encrypted with user key (device-specific derivation)
```

---

## Prerequisites

- Android Studio Ladybug (2024.2.1) or later
- JDK 17
- Android SDK 35
- Gradle 8.9

---

## Setup & Build

### 1. Clone / Open Project
```
Open E:\poc\persosnaltracker in Android Studio
```

### 2. OneDrive Setup (Optional)
1. Register an app in [Azure Portal](https://portal.azure.com) → Azure Active Directory → App Registrations
2. Add redirect URI: `msauth://com.personaltracker/callback`
3. Copy your Client ID
4. Replace `YOUR_AZURE_CLIENT_ID` in `app/src/main/res/raw/msal_auth_config.json`

### 3. Build Debug APK
```bash
# In Android Studio terminal or PowerShell:
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### 4. Build Release APK
```bash
# Create keystore first:
keytool -genkey -v -keystore personaltracker.jks -keyalg RSA -keysize 2048 -validity 10000 -alias personaltracker

# Configure signing in app/build.gradle.kts, then:
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### 5. Build Release AAB (for Play Store)
```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

---

## Project Structure

```
app/src/main/java/com/personaltracker/
├── PersonalTrackerApp.kt          # Hilt Application + Notification Channels
├── MainActivity.kt                # Single Activity host
│
├── security/
│   ├── SecurityManager.kt         # Android Keystore + AES-GCM encrypt/decrypt
│   ├── BiometricHelper.kt         # Biometric authentication
│   └── PinManager.kt              # PIN hashing + per-module PINs
│
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt         # Room DB (SQLCipher encrypted)
│   │   ├── entity/                # 13 Room entities
│   │   ├── dao/                   # 9 DAOs with Flow-based queries
│   │   └── converter/             # LocalDate/LocalDateTime converters
│   └── repository/                # Repository implementations
│
├── domain/
│   └── repository/                # Repository interfaces
│
├── di/
│   ├── AppModule.kt               # DataStore
│   ├── DatabaseModule.kt          # SQLCipher Room DB
│   ├── RepositoryModule.kt        # Repository bindings
│   └── BackupModule.kt            # Backup services
│
├── backup/
│   ├── BackupManager.kt           # Local encrypted backup
│   └── OneDriveManager.kt         # MSAL + Graph API integration
│
├── workers/
│   ├── ExpenseReminderWorker.kt   # Daily 10PM expense reminder
│   └── EmiReminderWorker.kt       # EMI due-date reminders
│
├── ui/
│   ├── theme/                     # Material 3 theme, colors, typography
│   ├── navigation/                # NavGraph with 25+ routes
│   ├── components/                # Reusable composables
│   └── screens/
│       ├── splash/                # Animated splash screen
│       ├── auth/                  # PIN auth + biometric + setup
│       ├── dashboard/             # Smart analytics dashboard
│       ├── documents/             # Documents vault
│       ├── credentials/           # Password manager
│       ├── expenses/              # Expense tracker + reports
│       ├── investments/           # Investment portfolio
│       ├── emi/                   # EMI tracker
│       ├── gold/                  # Gold investments
│       ├── school/                # School expenses
│       ├── travel/                # Travel tracker
│       ├── groups/                # Group expense splitter
│       ├── settings/              # App settings
│       ├── backup/                # Backup management
│       └── security/              # Security settings
│
└── util/
    ├── DateUtils.kt               # Date formatting utilities
    ├── ExportUtils.kt             # CSV/PDF export
    └── NotificationUtils.kt       # Notification helpers
```

---

## First Run

1. Install APK on device
2. App opens → Splash screen → PIN Setup screen
3. Enter 6-digit PIN → Confirm PIN → Dashboard
4. Optionally enable biometric in Settings → Security

---

## Notifications

| Notification | Schedule |
|---|---|
| Expense Reminder | Daily at 10:00 PM |
| EMI Due Reminder | Daily (shows if EMI due in ≤3 days) |
| Document Expiry | When document expires within 30 days |
| Investment Maturity | When investment matures within 30 days |

---

## Backup & Restore

### Local Backup
Encrypted `.ptbak` files stored in app-private storage.

### OneDrive Backup
1. Go to Settings → Backup
2. Sign in with Microsoft account
3. Tap "Backup Now" or enable Auto-backup
4. Backup is AES-GCM encrypted before upload

### Restore on New Device
- Backup file is device-key encrypted
- Restoring requires the original master password
- Prevents unauthorized data migration

---

## Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

---

## Troubleshooting

**Build fails with SQLCipher error**
Ensure NDK is installed. In Android Studio: SDK Manager → SDK Tools → NDK (Side by side) → Install.

**Biometric not available**
The app gracefully falls back to PIN authentication if biometric hardware is unavailable.

**Room schema migration**
In development, `fallbackToDestructiveMigration()` is used. Before production release, add proper migration scripts.

---

## License
Private / Proprietary. All rights reserved.

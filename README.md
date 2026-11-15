# Secure Vault — Android Password Manager

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-24%2B%20(Nougat%20to%20Android%2015)-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Security](https://img.shields.io/badge/Cryptography-AES--256%20GCM-orange.svg)](https://csrc.nist.gov/publications/detail/sp/800-38d/final)

A modern, offline-first Android password manager built with security, privacy, and simplicity in mind. Protects personal credentials using military-grade authenticated encryption (**AES-256 GCM**), biometric authentication, screen protection, sensitive clipboard auto-clearing, and built-in security auditing.

---

## 🌟 Key Features

### 🔐 Multi-Layered Security Architecture
* **Authenticated AES-256 GCM Encryption:** All stored passwords are encrypted using Galois/Counter Mode (`AES/GCM/NoPadding`) with a unique 12-byte initialization vector (IV) per record, ensuring confidentiality and cryptographic integrity.
* **Biometric & Master Key Authentication:** Unlock your vault using device biometrics (Fingerprint / Face Unlock / Device Credentials) or your master passphrase.
* **Security Questions Recovery:** Built-in account recovery with salted SHA-256 security questions in case the master passphrase is forgotten.
* **Anti-Snooping (`FLAG_SECURE`):** Prevents unauthorized screenshots, screen recording, and hides vault contents from the Android Recent Apps / Task Switcher preview.
* **Sensitive Clipboard with Auto-Clear:** Credentials copied to the system clipboard are tagged with `EXTRA_IS_SENSITIVE` (preventing keyboard/history logging on Android 13+) and automatically purged from memory after 45 seconds.

### ⚡ Vault Management & Experience
* **Real-Time Instant Search:** Filter credentials instantly across website/app names, usernames, and notes as you type.
* **Category Filtering:** Organize credentials into categories: *General*, *Work*, *Social*, *Finance*, *Entertainment*, and *Other* with one-tap filter chips.
* **Custom App / Service Logos:** Attach custom logos or service icons to entries directly from the device gallery.
* **One-Tap Copy & Reveal:** Copy usernames and passwords with dedicated buttons; toggle password visibility on demand.

### 🛠️ Built-in Security Tools
* **Interactive Password Generator:** Generate cryptographically secure passwords (`SecureRandom`) with customizable lengths (6–32 characters) and granular character set toggles (uppercase, lowercase, digits, symbols).
* **Live Password Strength Meter:** Visual real-time indicator evaluating password entropy, length, and complexity (Weak, Fair, Good, Strong) during entry creation or editing.
* **Vault Security Health Audit:** Scan and analyze your vault in real-time for security health:
  * Total stored credentials
  * Strong vs. weak passwords
  * Reused password detection across multiple accounts
  * Tailored security action items

### 💾 Backup & Migration
* **RFC 4180 Compliant CSV Export & Import:** Safely export your decrypted vault to standard CSV for offline backups, or import credentials from external managers (Chrome, Bitwarden, etc.) with automatic encryption upon import.

---

## 🏗️ Architecture & Project Structure

The project follows clean Android architecture patterns with separation of concerns:

```
app/src/main/
├── AndroidManifest.xml                  # App manifest with permissions & activity definitions
├── java/com/example/mypasswordmanager/
│   ├── AuthActivity.java               # Master login, setup, recovery, and biometric unlock
│   ├── MainActivity.java               # Main vault dashboard, search, filtering, and dialogs
│   ├── DatabaseHelper.java             # SQLiteOpenHelper managing encrypted credential storage
│   ├── EncryptionUtil.java             # Authenticated AES-GCM and SHA-256 cryptographic utility
│   ├── PasswordAdapter.java            # RecyclerView adapter for vault items
│   ├── PasswordEntry.java              # Data model representing an entry
│   ├── PasswordGenerator.java          # Cryptographic random password generator
│   └── PasswordStrengthUtil.java       # Real-time password entropy & strength evaluator
└── res/
    ├── drawable/                       # Vector assets, rounded buttons, category badges
    ├── layout/                         # Modern Material layouts for activities and dialogs
    │   ├── activity_auth.xml           # Authentication & recovery interface
    │   ├── activity_main.xml           # Vault dashboard with search and categories
    │   ├── dialog_entry.xml            # Universal add/edit credential dialog
    │   ├── dialog_generator.xml        # Interactive password generator dialog
    │   ├── dialog_security_audit.xml   # Security health analysis dialog
    │   └── item_password.xml           # Vault credential item card
    └── values/                         # Colors, themes, and string resources
```

---

## 💻 Tech Stack

| Component | Technology |
|---|---|
| **Language** | Java (Java 11 compatibility) |
| **Minimum SDK** | Android 7.0 (API Level 24) |
| **Target SDK** | Android 15 (API Level 36) |
| **Cryptography** | AES/GCM/NoPadding (256-bit), SHA-256, SecureRandom |
| **Database** | SQLite with structured migration support |
| **UI Components** | AndroidX, Google Material Components 3, CardView, RecyclerView |
| **Biometrics** | AndroidX Biometric 1.1.0 (`BiometricPrompt`) |
| **Build System** | Gradle 9.0.1 with Android Gradle Plugin 9.0.1 |

---

## 🚀 Setup & Installation Instructions

### Prerequisites
* **Android Studio** (Ladybug / Jellyfish or later recommended)
* **Java Development Kit (JDK):** Version 11 or higher
* **Android SDK:** Platform 36, Build-Tools 36.0.0

### Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/jr0226/My-password-manager.git
   cd My-password-manager
   ```

2. **Configure SDK Location:**
   Create a `local.properties` file in the root directory pointing to your Android SDK:
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   # Windows example:
   # sdk.dir=C\:\\Users\\<Username>\\AppData\\Local\\Android\\Sdk
   ```

3. **Build Debug APK via Command Line:**
   * **Windows (PowerShell / Command Prompt):**
     ```powershell
     .\gradlew.bat assembleDebug
     ```
   * **macOS / Linux:**
     ```bash
     chmod +x gradlew
     ./gradlew assembleDebug
     ```

4. **Run Unit Tests:**
   ```bash
   .\gradlew.bat test
   ```

5. **Deploy to Device / Emulator:**
   Connect your Android device with USB debugging enabled, then run:
   ```bash
   .\gradlew.bat installDebug
   ```
   Or click **Run 'app'** inside Android Studio.

---

## 🔒 Security & Threat Model

* **Zero Plaintext Persistence:** Passwords are never written to SQLite or shared preferences in cleartext. All entries are encrypted before reaching disk.
* **No Unsolicited Network Access:** The application operates entirely offline. Credential data never leaves your device unless explicitly exported via user-initiated CSV backup.
* **Memory Safety:** Sensitive clipboard items are cleared proactively to prevent third-party background scrapers from harvesting credentials.
* **Backward Compatibility:** Modern `v2:` authenticated AES-GCM ciphertexts coexist with automatic legacy decrypt fallback to ensure zero data loss across upgrades.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

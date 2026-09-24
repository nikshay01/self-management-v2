# Samsung Health Data Retrieval Bridge

A lightweight, dedicated Android/Kotlin companion bridge for ingesting health data from Samsung Health into the web-based Self Management App.

```text
Samsung Health
      ↓
Samsung Health Data SDK
      ↓
Android/Kotlin Bridge (this app)
      ↓
Normalized Canonical JSON
      ↓
Self Management Backend API (POST /api/integrations/samsung/health-data)
```

> **Design Principle**: The main Self Management App is web-based. This Android bridge has only ONE responsibility: reliably reading Samsung Health data, converting it to canonical JSON, and securely uploading it to the backend ingestion endpoint.

---

## Table of Contents

1. [Architectural Overview](#architectural-overview)
2. [Prerequisites & System Requirements](#1-prerequisites--system-requirements)
3. [JDK 17 Installation](#2-jdk-installation)
4. [Android SDK Command-Line Tools Setup](#3-android-sdk-command-line-tools-setup)
5. [Environment Variables Configuration](#4-environment-variables-configuration)
6. [Samsung Health App Setup & Prerequisites](#5-samsung-health-app-setup)
7. [Enabling Samsung Health Developer Mode](#6-samsung-health-developer-mode)
8. [Samsung Health Data SDK Installation (.aar)](#7-samsung-health-data-sdk-installation)
9. [Building from VS Code & Command Line](#8-building-from-command-line--vs-code)
10. [Connecting a Physical Samsung/Android Device](#9-connecting-a-physical-device)
11. [Installing & Running the Application](#10-installing--running-the-application)
12. [Permission Flow & Verification](#11-granting-permissions)
13. [Testing Each Data Type](#12-testing-each-data-type)
14. [Backend API Contract & Configuration](#13-backend-api-contract--configuration)
15. [Synchronization Behavior & Deduplication](#14-sync-behavior--deduplication)
16. [Troubleshooting & FAQs](#15-troubleshooting)

---

## Architectural Overview

```text
┌────────────────────────────────────────────────────────┐
│                   Samsung Health App                   │
└───────────────────────────┬────────────────────────────┘
                            │ Samsung Health Data SDK (.aar)
┌───────────────────────────▼────────────────────────────┐
│                  Samsung-Specific Layer                │
│  - SamsungHealthManager   - SamsungPermissionManager   │
│  - StepReader             - HeartRateReader            │
│  - SleepReader            - ExerciseReader             │
│  - SamsungDataMapper (converts SDK → Canonical Models) │
└───────────────────────────┬────────────────────────────┘
                            │ Clean Kotlin Data Classes
┌───────────────────────────▼────────────────────────────┐
│                  Canonical Health Models               │
│  - StepsRecord            - HeartRateRecord            │
│  - SleepRecord            - ExerciseRecord             │
│  - HealthRecord           - HealthSyncPayload          │
└───────────────────────────┬────────────────────────────┘
                            │ Encrypted State & Storage
┌───────────────────────────▼────────────────────────────┐
│                    Sync Layer                          │
│  - SyncManager            - SyncStateStore             │
│  - SyncResult             - Deduplication Keys         │
└───────────────────────────┬────────────────────────────┘
                            │ OkHttp + Bearer Auth
┌───────────────────────────▼────────────────────────────┐
│                    Network Layer                       │
│  - ApiClient              - HealthSyncApi              │
└───────────────────────────┬────────────────────────────┘
                            │ HTTPS POST (JSON)
┌───────────────────────────▼────────────────────────────┐
│             Self Management Backend API                │
│       POST /api/integrations/samsung/health-data       │
└────────────────────────────────────────────────────────┘
```

The Samsung SDK is completely isolated behind the `samsung/` package. Neither the UI, the sync manager, nor the API client ever touches Samsung SDK classes. This ensures we can easily swap or augment Samsung Health with Google Health Connect, Apple Health, Garmin, or Oura in the future without backend modifications.

---

## 1. Prerequisites & System Requirements

* **Operating System**: Windows 10/11, macOS, or Linux
* **IDE**: VS Code (No Android Studio required)
* **Java Development Kit**: JDK 17 (or compatible LTS)
* **Android Target**: Android 10 (API level 29) minimum; Target SDK 34 (Android 14)
* **Device**: Physical Android smartphone running Samsung Health v6.30.2 or later (Emulators cannot access Samsung Health data)
* **Command-line tools**: Git, PowerShell (Windows) or Bash (macOS/Linux), ADB

---

## 2. JDK Installation

JDK 17 or higher is required.

### Windows (winget or manual)
```powershell
# Using winget (Eclipse Temurin JDK 17)
winget install EclipseAdoptium.Temurin.17.JDK

# Or verify existing Java
java -version
```

### macOS (Homebrew)
```bash
brew install openjdk@17
```

### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

---

## 3. Android SDK Command-Line Tools Setup

You do NOT need the full Android Studio bundle. You only need the Android Command-Line Tools (`cmdline-tools`).

1. Download the Command-Line Tools from the [Android Developer Portal](https://developer.android.com/studio#command-line-tools-only).
2. Extract to a directory of your choice, for example:
   * **Windows**: `C:\Android\Sdk`
   * **macOS/Linux**: `~/Android/Sdk`
3. Structure the folders so `cmdline-tools/latest/bin` exists:
   ```text
   C:\Android\Sdk\
   └── cmdline-tools\
       └── latest\
           ├── bin\
           │   ├── sdkmanager.bat
           │   └── avdmanager.bat
           └── ...
   ```
4. Install required SDK platform and platform-tools using `sdkmanager`:
   ```powershell
   # Accept licenses and install platform-tools (includes adb) and API 34
   cmdline-tools\latest\bin\sdkmanager.bat --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   cmdline-tools\latest\bin\sdkmanager.bat --licenses
   ```

---

## 4. Environment Variables Configuration

### Windows (PowerShell)
Set environment variables permanently or in your session:
```powershell
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.0.x", "User")
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Android\Sdk", "User")

# Add ADB and tools to Path:
$oldPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
$newPath = "$oldPath;C:\Android\Sdk\platform-tools;C:\Android\Sdk\cmdline-tools\latest\bin"
[System.Environment]::SetEnvironmentVariable("Path", $newPath, "User")
```

### macOS / Linux (`~/.bashrc` or `~/.zshrc`)
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin
```

### Project `local.properties`
Create `local.properties` in the `samsung-health-bridge/` root folder (copied from `local.properties.template`):
```properties
# sdk.dir path (use forward slashes or escaped backslashes on Windows)
sdk.dir=C:/Android/Sdk

# Backend API URLs
API_BASE_URL_DEV=http://10.0.2.2:3000
API_BASE_URL_STAGING=https://staging-api.selfmanagement.app
API_BASE_URL=https://api.selfmanagement.app

# Optional dev token for testing backend POST
# DEV_AUTH_TOKEN=your-jwt-or-bearer-token-here
```

---

## 5. Samsung Health App Setup

1. Install the latest **Samsung Health** app from the Samsung Galaxy Store or Google Play Store on your physical device (version 6.30.2 or higher required).
2. Open Samsung Health, sign in with your Samsung Account, and complete initial onboarding.
3. Ensure at least a few steps, heart rate readings, or exercise activities are recorded so there is data available to retrieve.

---

## 6. Samsung Health Developer Mode

To allow third-party companion apps to read health data without waiting for Samsung Partner Program approval, **Developer Mode** must be enabled in Samsung Health:

1. Open the **Samsung Health** app on your physical phone.
2. Tap the **More options (three dots)** menu in the top right -> **Settings**.
3. Scroll to the bottom and tap **About Samsung Health**.
4. Rapidly tap the **Version number** (e.g. `6.30.x.xxx`) **10 times in a row**.
5. A toast message or prompt will appear: **"Developer Mode is turned on"** (or will offer Developer options).
6. Enable **Developer Mode**.
7. Restart the Samsung Health app.

> **Note**: Developer Mode allows local test builds matching your development package ID (`com.selfmanagement.samsungbridge`) to request permissions directly.

---

## 7. Samsung Health Data SDK Installation

Samsung distributes the SDK as an `.aar` library.

1. Visit the [Samsung Developer Health Portal](https://developer.samsung.com/health).
2. Sign in and navigate to **Samsung Health Data SDK** -> **Downloads**.
3. Download the SDK archive and extract the file:
   `samsung-health-data-api-*.aar`
4. Copy the `.aar` file into:
   ```text
   samsung-health-bridge/app/libs/
   ```
   Example:
   `samsung-health-bridge/app/libs/samsung-health-data-api-1.5.0.aar`

Gradle is pre-configured in `app/build.gradle.kts` to automatically link all `.aar` files in `app/libs/`:
```kotlin
dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
}
```

---

## 8. Building from Command Line & VS Code

No Android Studio required! Use the Gradle wrapper included in the project:

### Build Debug APK
```powershell
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

The compiled APK will be generated at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### Build Environment Flavors
The project supports three environment flavors: `development`, `staging`, and `production`:
```powershell
# Development build
.\gradlew.bat assembleDevelopmentDebug

# Staging build
.\gradlew.bat assembleStagingDebug

# Production release
.\gradlew.bat assembleProductionRelease
```

### Run Unit Tests
```powershell
.\gradlew.bat test
```

---

## 9. Connecting a Physical Device

1. Enable **Developer Options** on your Android/Samsung phone:
   * Go to **Settings** -> **About Phone** -> **Software Information**.
   * Tap **Build number** 7 times until it says "You are now a developer!".
2. Enable **USB Debugging**:
   * Go to **Settings** -> **Developer Options**.
   * Toggle **USB Debugging** to ON.
3. Connect your phone to your PC via USB cable.
4. When prompted on your phone screen, check "Always allow from this computer" and tap **Allow**.
5. Verify ADB recognizes the device:
   ```powershell
   adb devices
   ```
   Output:
   ```text
   List of devices attached
   RFCNA0XXXXX    device
   ```

---

## 10. Installing & Running the Application

### Install the APK via ADB
```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Launch the App via ADB
```powershell
adb shell am start -n com.selfmanagement.samsungbridge/.MainActivity
```

### View Live Structured Logs via Logcat
```powershell
adb logcat -s SamsungHealthMgr SamsungPermMgr SamsungHealthReader SyncMgr ApiClient
```

---

## 11. Granting Permissions

When you open the app on your physical phone:

1. The UI checks if Samsung Health is installed and connects to the `HealthDataStore`.
2. The **Status** displays: `Samsung Health: Connected`.
3. Tap **[ Request Permissions ]**.
4. Samsung Health opens its official permission consent dialog.
5. Turn on permissions for:
   * **Daily Steps** (Read)
   * **Heart Rate** (Read)
   * **Sleep** (Read)
   * **Exercise** (Read)
6. Tap **Done**.
7. The bridge UI will instantly reflect:
   ```text
   Steps: ✓
   Heart Rate: ✓
   Sleep: ✓
   Exercise: ✓
   ```

---

## 12. Testing Each Data Type

### Milestone 1: Daily Steps
Tap **[ Sync Now ]**.
The app queries the Samsung Health Data Store for today's steps:
* Summarizes total steps in the UI:
  ```text
  Steps: 8,421
    Records: 24
  ```
* Logs normalized records in Logcat:
  ```text
  [SamsungHealth] Retrieved 24 step records
  [Mapper] Normalized 24 records
  ```

### Milestone 2: Heart Rate, Sleep & Exercise
The app queries all 4 data readers in parallel:
* **Heart Rate**: Extracts BPM samples, calculates average, min, max.
* **Sleep**: Extracts start time, end time, total sleep duration, and stage breakdown (deep, light, REM, awake).
* **Exercise**: Extracts workout type (e.g., Walking, Running, Cycling), duration, distance (m), active calories (kcal), and heart rate metrics.

Data summary displayed in UI:
```text
Steps: 8,421
  Records: 24

Heart Rate:
  Samples: 320
  Average: 71 bpm
  Minimum: 58 bpm
  Maximum: 124 bpm

Sleep:
  Start: 2026-09-23T23:30:00+05:30
  End: 2026-09-24T06:45:00+05:30
  Duration: 7h 15m
  Stages: 4
    Light: 210m
    Deep: 65m
    REM: 110m
    Awake: 50m

Exercise:
  Walking
  Start: 2026-09-24T07:15:00+05:30
  Duration: 32m
  Distance: 2,450.0m
  Calories: 142.5 kcal
  Avg HR: 104 bpm
  Max HR: 128 bpm
```

---

## 13. Backend API Contract & Configuration

### Endpoint
```http
POST /api/integrations/samsung/health-data
Content-Type: application/json
Authorization: Bearer <token>
```

### Request Payload Format
```json
{
  "source": "samsung_health",
  "device": "Galaxy S24 / Watch6",
  "sync_started_at": "2026-09-24T08:00:00Z",
  "sync_range": {
    "start": "2026-09-23T00:00:00Z",
    "end": "2026-09-24T08:00:00Z"
  },
  "records": [
    {
      "source": "samsung_health",
      "source_record_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "source_device": "Galaxy Watch6 (BT)",
      "type": "steps",
      "timestamp_start": "2026-09-24T07:00:00+05:30",
      "timestamp_end": "2026-09-24T07:10:00+05:30",
      "value": 450.0,
      "unit": "count",
      "metadata": {
        "calories_kcal": 18.2,
        "distance_m": 320.5,
        "speed_ms": 1.2
      }
    },
    {
      "source": "samsung_health",
      "source_record_id": "hr-1727143200000-72.0",
      "source_device": "Galaxy Watch6 (BT)",
      "type": "heart_rate",
      "timestamp_start": "2026-09-24T07:30:00+05:30",
      "timestamp_end": null,
      "value": 72.0,
      "unit": "bpm",
      "metadata": {
        "heart_rate_min": 70.0,
        "heart_rate_max": 74.0
      }
    },
    {
      "source": "samsung_health",
      "source_record_id": "sleep-1727114400000-1727140500000",
      "source_device": "Galaxy Watch6 (BT)",
      "type": "sleep",
      "timestamp_start": "2026-09-23T23:30:00+05:30",
      "timestamp_end": "2026-09-24T06:45:00+05:30",
      "value": 435.0,
      "unit": "minutes",
      "metadata": {
        "duration_ms": 26100000,
        "stages": [
          { "stage": 40001, "stage_name": "Awake", "duration_ms": 3000000 },
          { "stage": 40002, "stage_name": "Light", "duration_ms": 12600000 },
          { "stage": 40003, "stage_name": "Deep", "duration_ms": 3900000 },
          { "stage": 40004, "stage_name": "REM", "duration_ms": 6600000 }
        ]
      }
    },
    {
      "source": "samsung_health",
      "source_record_id": "exercise-1001-1727142900000",
      "source_device": "Galaxy Watch6 (BT)",
      "type": "exercise",
      "timestamp_start": "2026-09-24T07:15:00+05:30",
      "timestamp_end": "2026-09-24T07:47:00+05:30",
      "value": 32.0,
      "unit": "minutes",
      "metadata": {
        "exercise_type": 1001,
        "exercise_name": "Walking",
        "duration_ms": 1920000,
        "distance_m": 2450.0,
        "calories_kcal": 142.5,
        "mean_heart_rate": 104.0,
        "max_heart_rate": 128.0
      }
    }
  ]
}
```

### Backend Response Format
```json
{
  "success": true,
  "records_received": 28,
  "records_inserted": 28,
  "records_skipped": 0,
  "records_failed": 0,
  "message": "Health data ingested successfully"
}
```

### Status Code Handling
* **200 OK / 201 Created**: Successful ingestion.
* **400 Bad Request**: Malformed payload.
* **401 Unauthorized**: Missing or expired auth token.
* **409 Conflict**: Duplicate records detected. Treated gracefully by the sync manager as skipped duplicate records.
* **500 Internal Server Error**: Backend error; logged and stored in `SyncStateStore`.

---

## 14. Sync Behavior & Deduplication

### Deterministic Deduplication IDs
The backend must enforce `UNIQUE(user_id, source, source_record_id)`.
Every record produced by this bridge has a stable, deterministic ID:
1. **Samsung UUID**: If Samsung Health provides a `datauuid`, it is preserved directly as `source_record_id`.
2. **Deterministic Fallback**: If `datauuid` is null or unavailable, a deterministic key is derived from immutable attributes:
   * **Steps**: `steps-{startTimeMillis}-{endTimeMillis}`
   * **Heart Rate**: `hr-{startTimeMillis}-{bpm}`
   * **Sleep**: `sleep-{startTimeMillis}-{endTimeMillis}`
   * **Exercise**: `exercise-{exerciseType}-{startTimeMillis}`

Random UUIDs are NEVER generated for synced records.

### Sync State Persistence
The `SyncStateStore` uses Android's `EncryptedSharedPreferences` with MasterKey AES-256 encryption to persist:
* `last_successful_sync`: Timestamp of last 200 OK upload
* `last_attempted_sync`: Timestamp of last sync attempt
* `sync_status`: `NEVER_SYNCED`, `IN_PROGRESS`, `SUCCESS`, `FAILED`
* `last_error`: Message describing the most recent failure

### Incremental Synchronization
Call `syncManager.syncIncremental { ... }`.
The sync manager reads `last_successful_sync`, and queries only data from that timestamp up to `System.currentTimeMillis()`, avoiding redundant re-fetching of historical data.

---

## 15. Troubleshooting

### 1. "Samsung Health: Not Installed"
* Verify that the official **Samsung Health** app is installed from Google Play or Galaxy Store.
* Verify package visibility in `AndroidManifest.xml`: `<queries><package android:name="com.sec.android.app.shealth" /></queries>` is present.

### 2. "Connection failed (USER_AGREEMENT_NEEDED)"
* Open Samsung Health on the phone manually, accept the Terms & Conditions and Privacy Policy, and initialize your profile.

### 3. "Permission dialog does not show requested types"
* Ensure Samsung Health **Developer Mode** is enabled (see Section 6). In production, non-developer builds require Samsung Partner approval.
* Re-launch the app after toggling Developer Mode.

### 4. "No .aar found in app/libs"
* Download `samsung-health-data-api-*.aar` from the Samsung Developer Health Portal and place it into `app/libs/`.

### 5. "adb: command not found"
* Add `$ANDROID_HOME/platform-tools` to your system `PATH`.
* Ensure USB Debugging is enabled on the device.

### 6. "HTTP 401 Unauthorized"
* Provide a valid authentication token via `apiClient.setAuthToken("your_token")` or set `DEV_AUTH_TOKEN` in `local.properties`.

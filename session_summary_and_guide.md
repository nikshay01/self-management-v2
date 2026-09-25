# Samsung Health Bridge — Build & Usage Guide

## 1. Executive Summary & What Was Done

During this session, we resolved a critical SDK mismatch issue that was preventing the Samsung Health Bridge Android app from compiling, and successfully produced a verified debug APK.

### The Problem
The project codebase had been written targeting the **legacy Samsung Health SDK** (`com.samsung.android.sdk.healthdata.*`, which used `HealthDataStore(context, listener)`, `HealthDataResolver`, `HealthConstants`, and untyped cursors). However, the `.aar` library placed in `app/libs/` was the **modern Samsung Health Data SDK v1.1.0** (`com.samsung.android.sdk.health.data.*`). This caused hundreds of Kotlin compilation errors (`Unresolved reference: healthdata`, `HealthConstants`, `HealthDataResolver`, etc.).

### What Was Changed
We completely refactored the Samsung integration layer (`samsung/`) to adopt the **Samsung Health Data SDK v1.1.0 API**:

1. **[SamsungHealthManager.kt](file:///d:/nikshay/coding/big%20project/self-managerment-v2/bridge_proto/samsung-health-bridge/app/src/main/java/com/selfmanagement/samsungbridge/samsung/SamsungHealthManager.kt)**:
   - Initialized `HealthDataStore` via `HealthDataService.getStore(context)`.
   - Updated connection flow and unified dispatching.

2. **[SamsungPermissionManager.kt](file:///d:/nikshay/coding/big%20project/self-managerment-v2/bridge_proto/samsung-health-bridge/app/src/main/java/com/selfmanagement/samsungbridge/samsung/SamsungPermissionManager.kt)**:
   - Migrated permission requests to `Permission.of(DataTypes.*, AccessType.READ)`.
   - Adapted permission queries and request flows to be asynchronous coroutines.

3. **[StepReader.kt](file:///d:/nikshay/coding/big%20project/self-managerment-v2/bridge_proto/samsung-health-bridge/app/src/main/java/com/selfmanagement/samsungbridge/samsung/StepReader.kt)**:
   - Migrated to `DataType.StepsType.TOTAL.requestBuilder` and `healthDataStore.aggregateData()` with `LocalTimeFilter`.

4. **[HeartRateReader.kt](file:///d:/nikshay/coding/big%20project/self-managerment-v2/bridge_proto/samsung-health-bridge/app/src/main/java/com/selfmanagement/samsungbridge/samsung/HeartRateReader.kt)**:
   - Replaced old queries with `DataTypes.HEART_RATE.readDataRequestBuilder` using `InstantTimeFilter`.
   - Extracted both single-point readings and series data samples (`SERIES_DATA`).

5. **[SleepReader.kt](file:///d:/nikshay/coding/big%20project/self-managerment-v2/bridge_proto/samsung-health-bridge/app/src/main/java/com/selfmanagement/samsungbridge/samsung/SleepReader.kt)**:
   - Refactored to read `DataTypes.SLEEP` sessions and extract structured sleep stages (`AWAKE`, `LIGHT`, `DEEP`, `REM`).

6. **[ExerciseReader.kt](file:///d:/nikshay/coding/big%20project/self-managerment-v2/bridge_proto/samsung-health-bridge/app/src/main/java/com/selfmanagement/samsungbridge/samsung/ExerciseReader.kt)**:
   - Migrated to `DataTypes.EXERCISE.readDataRequestBuilder` reading duration, calories, distance, and exercise types.

7. **[SamsungDataMapper.kt](file:///d:/nikshay/coding/big%20project/self-managerment-v2/bridge_proto/samsung-health-bridge/app/src/main/java/com/selfmanagement/samsungbridge/samsung/SamsungDataMapper.kt)**:
   - Replaced custom date formatting with modern `java.time.Instant` and `OffsetDateTime` ISO-8601 formatting.
   - Added robust fallback deterministic ID generation for deduplication.

8. **[SamsungHealthReader.kt](file:///d:/nikshay/coding/big%20project/self-managerment-v2/bridge_proto/samsung-health-bridge/app/src/main/java/com/selfmanagement/samsungbridge/samsung/SamsungHealthReader.kt) & [MainActivity.kt](file:///d:/nikshay/coding/big%20project/self-managerment-v2/bridge_proto/samsung-health-bridge/app/src/main/java/com/selfmanagement/samsungbridge/MainActivity.kt)**:
   - Fixed imports and updated UI permission checks to support async responses.

### Build Verification
Ran `.\gradlew.bat assembleDevelopmentDebug`:
- **Result**: `BUILD SUCCESSFUL`
- **Generated APK**: `bridge_proto\samsung-health-bridge\app\build\outputs\apk\development\debug\app-development-debug.apk`

---

## 2. Complete Guide: How to Run and Use the Application

### Prerequisites
- **Physical Samsung/Android phone** running Samsung Health v6.30.2 or later.
  *(Note: Emulators cannot run or read Samsung Health data).*
- **USB Cable** with USB Debugging enabled on your phone.
- **ADB** (Android Debug Bridge) installed on your computer.

---

### Step 1: Enable Developer Mode in Samsung Health on Your Phone
Before third-party or local debug apps can access Samsung Health data without Samsung Partner Program approval, you **must enable Developer Mode**:

1. Open **Samsung Health** on your phone.
2. Tap the **Three dots menu (⋮)** in the top right -> **Settings**.
3. Scroll to the bottom and tap **About Samsung Health**.
4. Quickly tap the **Version number** (e.g. `6.30.x.xxx`) **10 times in a row**.
5. A toast/dialog will confirm: **"Developer Mode is turned on"** (or developer options will appear).
6. Ensure Developer Mode is switched **ON**.
7. Force stop and restart the Samsung Health app.

---

### Step 2: Connect Phone via ADB
1. On your phone: Go to **Settings** -> **Developer options** -> enable **USB debugging**.
2. Connect your phone to your PC via USB.
3. Accept the prompt on your phone screen: **"Allow USB debugging?"**.
4. Verify ADB detects the device by opening PowerShell/terminal and running:
   ```powershell
   adb devices
   ```
   You should see your device listed (e.g., `RF8M...    device`).

---

### Step 3: Install the APK on Your Phone
From the project directory, run:
```powershell
adb install -r "d:\nikshay\coding\big project\self-managerment-v2\bridge_proto\samsung-health-bridge\app\build\outputs\apk\development\debug\app-development-debug.apk"
```

Or use Gradle to build and install in one step:
```powershell
cd "d:\nikshay\coding\big project\self-managerment-v2\bridge_proto\samsung-health-bridge"
.\gradlew.bat installDevelopmentDebug
```

---

### Step 4: Configure Backend Ingestion (Optional for UI Verification)
If you want to sync data to the Self Management backend:

1. Open `local.properties` in `bridge_proto/samsung-health-bridge/`:
   ```properties
   # Point to your local or deployed backend
   API_BASE_URL_DEV=http://<YOUR_COMPUTER_LOCAL_IP>:3000
   API_BASE_URL=https://api.selfmanagement.app
   ```
   *(Note: Use your PC's LAN IP, like `http://192.168.1.50:3000`, when testing on a physical phone, not `localhost` or `10.0.2.2`).*

2. The bridge sends a canonical payload via `POST /api/integrations/samsung/health-data`.

---

### Step 5: Using the Testing UI
1. Launch the app **"Samsung Health Bridge"** on your phone.
2. **Connection Status**:
   - The app will automatically connect to Samsung Health upon launch.
   - Status will show **"Samsung Health: Connected"** in green.
3. **Request Permissions**:
   - Tap **"Request Permissions"**.
   - A Samsung Health permissions dialog will appear requesting access to Steps, Heart Rate, Sleep, and Exercise.
   - Tap **"All permissions"** -> **"Done"**.
   - The app UI will update displaying checkmarks (`✓`) next to each data type.
4. **Sync / Read Data**:
   - Tap **"Sync Now"**.
   - The app reads all recorded samples for today (Steps, Heart Rate, Sleep, and Exercise).
   - The data summary box will display:
     - Total step count and records.
     - Heart rate sample count, average, min, and max BPM.
     - Sleep session duration and stages (awake, light, deep, REM).
     - Exercise activity logs, duration, calories, and distances.
   - If an auth token is set, it will upload the data payload to your backend.

---

## 3. Useful Commands Quick Reference

| Action | Command |
| :--- | :--- |
| **Recompile APK** | `.\gradlew.bat assembleDevelopmentDebug` |
| **Install to Device** | `adb install -r app/build/outputs/apk/development/debug/app-development-debug.apk` |
| **View Live App Logs** | `adb logcat -s "SamsungHealthMgr" "MainActivity" "StepReader" "HeartRateReader" "SleepReader" "ExerciseReader" "SyncManager"` |
| **Uninstall App** | `adb uninstall com.selfmanagement.samsungbridge.dev` |

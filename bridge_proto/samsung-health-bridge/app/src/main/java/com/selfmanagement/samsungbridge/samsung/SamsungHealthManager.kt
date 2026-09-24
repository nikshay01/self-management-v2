package com.selfmanagement.samsungbridge.samsung

import android.app.Activity
import android.content.Context
import android.util.Log
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult
import com.samsung.android.sdk.healthdata.HealthDataStore
import com.selfmanagement.samsungbridge.models.*

/**
 * High-level Samsung Health service.
 *
 * This is the single entry point for the rest of the application to interact
 * with Samsung Health. It manages:
 * - SDK initialization and connection lifecycle
 * - Permission management
 * - Data reading (delegates to [SamsungHealthReader])
 *
 * The rest of the application should NOT need to know how Samsung's SDK
 * works internally. Only canonical model types leave this class.
 *
 * ## Usage
 * ```kotlin
 * val manager = SamsungHealthManager(context)
 * manager.connect { connected ->
 *     if (connected) {
 *         manager.requestPermissions(activity) { state ->
 *             if (state.allGranted) {
 *                 manager.readSteps(start, end) { records -> ... }
 *             }
 *         }
 *     }
 * }
 * ```
 */
class SamsungHealthManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "SamsungHealthMgr"
    }

    /** Connection state */
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    /** Current connection state */
    var connectionState: ConnectionState = ConnectionState.DISCONNECTED
        private set

    /** Last connection error, if any */
    var lastError: String? = null
        private set

    private var healthDataStore: HealthDataStore? = null
    private var permissionManager: SamsungPermissionManager? = null
    private var healthReader: SamsungHealthReader? = null

    /**
     * Check if Samsung Health is available on this device.
     *
     * Samsung Health must be installed and the device must support it.
     */
    fun isAvailable(): Boolean {
        return try {
            // Check if Samsung Health app is installed
            val pm = context.packageManager
            pm.getPackageInfo("com.sec.android.app.shealth", 0)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Samsung Health not installed or not available", e)
            false
        }
    }

    /**
     * Connect to Samsung Health Data Store.
     *
     * Must be called before any data operations. The connection is
     * asynchronous — [callback] is invoked with `true` on success
     * or `false` on failure.
     *
     * @param callback Called with the connection result
     */
    fun connect(callback: (Boolean) -> Unit) {
        if (connectionState == ConnectionState.CONNECTED && healthDataStore != null) {
            Log.d(TAG, "Already connected")
            callback(true)
            return
        }

        connectionState = ConnectionState.CONNECTING
        lastError = null

        Log.d(TAG, "[SamsungHealth] Initializing")

        try {
            val store = HealthDataStore(context, object : HealthDataStore.ConnectionListener {
                override fun onConnected() {
                    Log.d(TAG, "[SamsungHealth] Connected")
                    connectionState = ConnectionState.CONNECTED
                    healthDataStore = store
                    permissionManager = SamsungPermissionManager(store)
                    healthReader = SamsungHealthReader(store)
                    callback(true)
                }

                override fun onConnectionFailed(error: HealthConnectionErrorResult) {
                    val errorMsg = mapConnectionError(error)
                    Log.e(TAG, "[SamsungHealth] Connection failed: $errorMsg")
                    connectionState = ConnectionState.ERROR
                    lastError = errorMsg
                    callback(false)
                }

                override fun onDisconnected() {
                    Log.d(TAG, "[SamsungHealth] Disconnected")
                    connectionState = ConnectionState.DISCONNECTED
                    healthDataStore = null
                    permissionManager = null
                    healthReader = null
                }
            })

            healthDataStore = store
            store.connectService()

        } catch (e: Exception) {
            Log.e(TAG, "[SamsungHealth] SDK initialization failure", e)
            connectionState = ConnectionState.ERROR
            lastError = "SDK initialization failed: ${e.message}"
            callback(false)
        }
    }

    /**
     * Disconnect from Samsung Health Data Store.
     */
    fun disconnect() {
        try {
            healthDataStore?.disconnectService()
        } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting", e)
        }
        connectionState = ConnectionState.DISCONNECTED
        healthDataStore = null
        permissionManager = null
        healthReader = null
    }

    /**
     * Check current permission state for all data types.
     *
     * @return Current permission state, or null if not connected
     */
    fun getPermissions(): SamsungPermissionManager.PermissionState? {
        val pm = permissionManager ?: run {
            Log.w(TAG, "[SamsungHealth] Permissions checked — not connected")
            return null
        }
        Log.d(TAG, "[SamsungHealth] Permissions checked")
        return pm.checkPermissions()
    }

    /**
     * Request all required permissions.
     *
     * @param activity The activity to use for the permission dialog
     * @param callback Called with the updated permission state
     */
    fun requestPermissions(
        activity: Activity,
        callback: (SamsungPermissionManager.PermissionState) -> Unit
    ) {
        val pm = permissionManager ?: run {
            Log.e(TAG, "Cannot request permissions — not connected")
            callback(SamsungPermissionManager.PermissionState())
            return
        }
        pm.requestPermissions(activity, callback)
    }

    /**
     * Read step records within the specified time range.
     */
    fun readSteps(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<StepsRecord>) -> Unit
    ) {
        val reader = healthReader ?: run {
            Log.e(TAG, "[SamsungHealth] Reading steps — not connected")
            callback(emptyList())
            return
        }
        Log.d(TAG, "[SamsungHealth] Reading steps")
        reader.readSteps(startTimeMillis, endTimeMillis) { records ->
            Log.d(TAG, "[SamsungHealth] Retrieved ${records.size} step records")
            callback(records)
        }
    }

    /**
     * Read total step count (aggregate) for the specified time range.
     */
    fun readTotalSteps(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (Int?) -> Unit
    ) {
        val reader = healthReader ?: run {
            callback(null)
            return
        }
        reader.readTotalSteps(startTimeMillis, endTimeMillis, callback)
    }

    /**
     * Read heart rate samples within the specified time range.
     */
    fun readHeartRate(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<HeartRateRecord>) -> Unit
    ) {
        val reader = healthReader ?: run {
            Log.e(TAG, "[SamsungHealth] Reading heart rate — not connected")
            callback(emptyList())
            return
        }
        Log.d(TAG, "[SamsungHealth] Reading heart rate")
        reader.readHeartRate(startTimeMillis, endTimeMillis) { records ->
            Log.d(TAG, "[SamsungHealth] Retrieved ${records.size} heart rate records")
            callback(records)
        }
    }

    /**
     * Read sleep sessions within the specified time range.
     */
    fun readSleep(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<SleepRecord>) -> Unit
    ) {
        val reader = healthReader ?: run {
            Log.e(TAG, "[SamsungHealth] Reading sleep — not connected")
            callback(emptyList())
            return
        }
        Log.d(TAG, "[SamsungHealth] Reading sleep")
        reader.readSleep(startTimeMillis, endTimeMillis) { records ->
            Log.d(TAG, "[SamsungHealth] Retrieved ${records.size} sleep records")
            callback(records)
        }
    }

    /**
     * Read exercise sessions within the specified time range.
     */
    fun readExercise(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<ExerciseRecord>) -> Unit
    ) {
        val reader = healthReader ?: run {
            Log.e(TAG, "[SamsungHealth] Reading exercise — not connected")
            callback(emptyList())
            return
        }
        Log.d(TAG, "[SamsungHealth] Reading exercise")
        reader.readExercise(startTimeMillis, endTimeMillis) { records ->
            Log.d(TAG, "[SamsungHealth] Retrieved ${records.size} exercise records")
            callback(records)
        }
    }

    /**
     * Read ALL data types and return combined results.
     */
    fun readAll(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (SamsungHealthReader.ReadAllResult) -> Unit
    ) {
        val reader = healthReader ?: run {
            Log.e(TAG, "Cannot read — not connected")
            callback(SamsungHealthReader.ReadAllResult())
            return
        }
        reader.readAll(startTimeMillis, endTimeMillis, callback)
    }

    /**
     * Perform a full sync: read all data types and return normalized records.
     *
     * @param startTimeMillis Start of the sync range (UTC milliseconds)
     * @param endTimeMillis End of the sync range (UTC milliseconds)
     * @param callback Called with the list of canonical [HealthRecord] objects
     */
    fun sync(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<HealthRecord>) -> Unit
    ) {
        readAll(startTimeMillis, endTimeMillis) { result ->
            val normalized = result.toHealthRecords()
            Log.d(TAG, "[Mapper] Normalized ${normalized.size} records")
            callback(normalized)
        }
    }

    /**
     * Map Samsung connection error to human-readable message.
     */
    private fun mapConnectionError(error: HealthConnectionErrorResult): String {
        return when {
            error.hasResolution() -> "Connection error (resolvable): ${error.errorCode}"
            else -> when (error.errorCode) {
                HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED ->
                    "Samsung Health is not installed"
                HealthConnectionErrorResult.OLD_VERSION_PLATFORM ->
                    "Samsung Health needs to be updated"
                HealthConnectionErrorResult.PLATFORM_DISABLED ->
                    "Samsung Health is disabled"
                HealthConnectionErrorResult.USER_AGREEMENT_NEEDED ->
                    "Samsung Health user agreement is needed"
                else -> "Connection failed (error code: ${error.errorCode})"
            }
        }
    }
}

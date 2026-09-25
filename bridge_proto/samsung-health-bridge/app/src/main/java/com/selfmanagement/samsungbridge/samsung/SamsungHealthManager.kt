package com.selfmanagement.samsungbridge.samsung

import android.app.Activity
import android.content.Context
import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.selfmanagement.samsungbridge.models.*

/**
 * High-level Samsung Health service using Samsung Health Data SDK v1.1.0.
 */
class SamsungHealthManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "SamsungHealthMgr"
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    var connectionState: ConnectionState = ConnectionState.DISCONNECTED
        private set

    var lastError: String? = null
        private set

    private var healthDataStore: HealthDataStore? = null
    private var permissionManager: SamsungPermissionManager? = null
    private var healthReader: SamsungHealthReader? = null

    /**
     * Check if Samsung Health is installed on this device.
     */
    fun isAvailable(): Boolean {
        return try {
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
     * In SDK v1.1.0, HealthDataService.getStore(context) initializes the store.
     */
    fun connect(callback: (Boolean) -> Unit) {
        if (connectionState == ConnectionState.CONNECTED && healthDataStore != null) {
            Log.d(TAG, "Already connected")
            callback(true)
            return
        }

        connectionState = ConnectionState.CONNECTING
        lastError = null

        Log.d(TAG, "[SamsungHealth] Initializing HealthDataStore via HealthDataService")

        try {
            val store = HealthDataService.getStore(context)
            healthDataStore = store
            permissionManager = SamsungPermissionManager(store)
            healthReader = SamsungHealthReader(store)
            connectionState = ConnectionState.CONNECTED
            Log.d(TAG, "[SamsungHealth] Connected successfully")
            callback(true)
        } catch (e: Exception) {
            Log.e(TAG, "[SamsungHealth] SDK initialization failure", e)
            connectionState = ConnectionState.ERROR
            lastError = "SDK initialization failed: ${e.message}"
            callback(false)
        }
    }

    /**
     * Disconnect from Samsung Health.
     */
    fun disconnect() {
        connectionState = ConnectionState.DISCONNECTED
        healthDataStore = null
        permissionManager = null
        healthReader = null
    }

    /**
     * Check current permission state for all data types asynchronously.
     */
    fun getPermissions(callback: (SamsungPermissionManager.PermissionState) -> Unit) {
        val pm = permissionManager ?: run {
            Log.w(TAG, "[SamsungHealth] Permissions checked — not connected")
            callback(SamsungPermissionManager.PermissionState())
            return
        }
        pm.checkPermissions(callback)
    }

    /**
     * Request all required permissions.
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

    fun readSteps(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<StepsRecord>) -> Unit
    ) {
        val reader = healthReader ?: run {
            callback(emptyList())
            return
        }
        reader.readSteps(startTimeMillis, endTimeMillis, callback)
    }

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

    fun readHeartRate(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<HeartRateRecord>) -> Unit
    ) {
        val reader = healthReader ?: run {
            callback(emptyList())
            return
        }
        reader.readHeartRate(startTimeMillis, endTimeMillis, callback)
    }

    fun readSleep(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<SleepRecord>) -> Unit
    ) {
        val reader = healthReader ?: run {
            callback(emptyList())
            return
        }
        reader.readSleep(startTimeMillis, endTimeMillis, callback)
    }

    fun readExercise(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (List<ExerciseRecord>) -> Unit
    ) {
        val reader = healthReader ?: run {
            callback(emptyList())
            return
        }
        reader.readExercise(startTimeMillis, endTimeMillis, callback)
    }

    fun readAll(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (SamsungHealthReader.ReadAllResult) -> Unit
    ) {
        val reader = healthReader ?: run {
            callback(SamsungHealthReader.ReadAllResult())
            return
        }
        reader.readAll(startTimeMillis, endTimeMillis, callback)
    }

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
}

package com.selfmanagement.samsungbridge.samsung

import android.app.Activity
import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages Samsung Health Data permissions using the new Samsung Health Data SDK (v1.1.0).
 */
class SamsungPermissionManager(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "SamsungPermMgr"

        val STEP_PERMISSION = Permission.of(DataTypes.STEPS, AccessType.READ)
        val HEART_RATE_PERMISSION = Permission.of(DataTypes.HEART_RATE, AccessType.READ)
        val SLEEP_PERMISSION = Permission.of(DataTypes.SLEEP, AccessType.READ)
        val EXERCISE_PERMISSION = Permission.of(DataTypes.EXERCISE, AccessType.READ)

        val ALL_PERMISSIONS: Set<Permission> = setOf(
            STEP_PERMISSION,
            HEART_RATE_PERMISSION,
            SLEEP_PERMISSION,
            EXERCISE_PERMISSION
        )
    }

    enum class PermissionStatus {
        GRANTED,
        DENIED,
        UNAVAILABLE,
        UNKNOWN
    }

    data class PermissionState(
        val steps: PermissionStatus = PermissionStatus.UNKNOWN,
        val heartRate: PermissionStatus = PermissionStatus.UNKNOWN,
        val sleep: PermissionStatus = PermissionStatus.UNKNOWN,
        val exercise: PermissionStatus = PermissionStatus.UNKNOWN
    ) {
        val allGranted: Boolean
            get() = steps == PermissionStatus.GRANTED &&
                    heartRate == PermissionStatus.GRANTED &&
                    sleep == PermissionStatus.GRANTED &&
                    exercise == PermissionStatus.GRANTED

        val anyDenied: Boolean
            get() = steps == PermissionStatus.DENIED ||
                    heartRate == PermissionStatus.DENIED ||
                    sleep == PermissionStatus.DENIED ||
                    exercise == PermissionStatus.DENIED
    }

    /**
     * Check which permissions are currently granted asynchronously.
     */
    fun checkPermissions(callback: (PermissionState) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val granted = healthDataStore.getGrantedPermissions(ALL_PERMISSIONS)
                val state = PermissionState(
                    steps = if (granted.contains(STEP_PERMISSION)) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                    heartRate = if (granted.contains(HEART_RATE_PERMISSION)) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                    sleep = if (granted.contains(SLEEP_PERMISSION)) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                    exercise = if (granted.contains(EXERCISE_PERMISSION)) PermissionStatus.GRANTED else PermissionStatus.DENIED
                )
                callback(state)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking permissions", e)
                callback(PermissionState())
            }
        }
    }

    /**
     * Request permissions with an Activity.
     */
    fun requestPermissions(
        activity: Activity,
        callback: (PermissionState) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val granted = healthDataStore.requestPermissions(ALL_PERMISSIONS, activity)
                val state = PermissionState(
                    steps = if (granted.contains(STEP_PERMISSION)) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                    heartRate = if (granted.contains(HEART_RATE_PERMISSION)) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                    sleep = if (granted.contains(SLEEP_PERMISSION)) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                    exercise = if (granted.contains(EXERCISE_PERMISSION)) PermissionStatus.GRANTED else PermissionStatus.DENIED
                )
                callback(state)
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting permissions", e)
                callback(PermissionState())
            }
        }
    }
}

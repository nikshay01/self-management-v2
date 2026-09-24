package com.selfmanagement.samsungbridge.samsung

import android.app.Activity
import android.util.Log
import com.samsung.android.sdk.healthdata.HealthConstants
import com.samsung.android.sdk.healthdata.HealthDataStore
import com.samsung.android.sdk.healthdata.HealthPermissionManager
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionType

/**
 * Manages Samsung Health permissions.
 *
 * Determines permission status, requests missing permissions,
 * and clearly reports granted/denied/unavailable states.
 *
 * Only requests READ permissions for the four supported data types:
 * Steps, Heart Rate, Sleep, Exercise.
 */
class SamsungPermissionManager(
    private val healthDataStore: HealthDataStore
) {
    companion object {
        private const val TAG = "SamsungPermMgr"

        /**
         * The set of permission keys we need for this bridge.
         * READ-only permissions for the four supported data types.
         */
        val REQUIRED_PERMISSION_KEYS: Set<PermissionKey> = setOf(
            PermissionKey(HealthConstants.StepCount.HEALTH_DATA_TYPE, PermissionType.READ),
            PermissionKey(HealthConstants.HeartRate.HEALTH_DATA_TYPE, PermissionType.READ),
            PermissionKey(HealthConstants.Sleep.HEALTH_DATA_TYPE, PermissionType.READ),
            PermissionKey(HealthConstants.Exercise.HEALTH_DATA_TYPE, PermissionType.READ)
        )

        /**
         * Additional read permission for sleep stages (sub-data of Sleep).
         */
        val SLEEP_STAGE_PERMISSION = PermissionKey(
            HealthConstants.SleepStage.HEALTH_DATA_TYPE,
            PermissionType.READ
        )

        val ALL_PERMISSION_KEYS: Set<PermissionKey> =
            REQUIRED_PERMISSION_KEYS + SLEEP_STAGE_PERMISSION
    }

    /** Permission status for a single data type */
    enum class PermissionStatus {
        GRANTED,
        DENIED,
        UNAVAILABLE,
        UNKNOWN
    }

    /** Result of checking all permissions */
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
     * Check which permissions are currently granted.
     *
     * @return Current permission state for all data types
     */
    fun checkPermissions(): PermissionState {
        return try {
            val permManager = HealthPermissionManager(healthDataStore)
            val resultMap = permManager.isPermissionAcquired(ALL_PERMISSION_KEYS)

            val stepsGranted = resultMap[PermissionKey(
                HealthConstants.StepCount.HEALTH_DATA_TYPE, PermissionType.READ
            )] ?: false

            val heartRateGranted = resultMap[PermissionKey(
                HealthConstants.HeartRate.HEALTH_DATA_TYPE, PermissionType.READ
            )] ?: false

            val sleepGranted = resultMap[PermissionKey(
                HealthConstants.Sleep.HEALTH_DATA_TYPE, PermissionType.READ
            )] ?: false

            val exerciseGranted = resultMap[PermissionKey(
                HealthConstants.Exercise.HEALTH_DATA_TYPE, PermissionType.READ
            )] ?: false

            Log.d(TAG, "Permissions checked — steps=$stepsGranted, hr=$heartRateGranted, " +
                    "sleep=$sleepGranted, exercise=$exerciseGranted")

            PermissionState(
                steps = if (stepsGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                heartRate = if (heartRateGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                sleep = if (sleepGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED,
                exercise = if (exerciseGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check permissions", e)
            PermissionState(
                steps = PermissionStatus.UNAVAILABLE,
                heartRate = PermissionStatus.UNAVAILABLE,
                sleep = PermissionStatus.UNAVAILABLE,
                exercise = PermissionStatus.UNAVAILABLE
            )
        }
    }

    /**
     * Request all required permissions that are not yet granted.
     *
     * Launches the Samsung Health permission dialog.
     * Only requests permissions that are currently not granted.
     *
     * @param activity The activity to use for the permission dialog
     * @param callback Called with the updated permission state after the dialog
     */
    fun requestPermissions(
        activity: Activity,
        callback: (PermissionState) -> Unit
    ) {
        try {
            val currentState = checkPermissions()

            // Build set of permissions that still need to be requested
            val missingPermissions = mutableSetOf<PermissionKey>()

            if (currentState.steps != PermissionStatus.GRANTED) {
                missingPermissions.add(PermissionKey(
                    HealthConstants.StepCount.HEALTH_DATA_TYPE, PermissionType.READ
                ))
            }
            if (currentState.heartRate != PermissionStatus.GRANTED) {
                missingPermissions.add(PermissionKey(
                    HealthConstants.HeartRate.HEALTH_DATA_TYPE, PermissionType.READ
                ))
            }
            if (currentState.sleep != PermissionStatus.GRANTED) {
                missingPermissions.add(PermissionKey(
                    HealthConstants.Sleep.HEALTH_DATA_TYPE, PermissionType.READ
                ))
                // Also request sleep stage permission
                missingPermissions.add(SLEEP_STAGE_PERMISSION)
            }
            if (currentState.exercise != PermissionStatus.GRANTED) {
                missingPermissions.add(PermissionKey(
                    HealthConstants.Exercise.HEALTH_DATA_TYPE, PermissionType.READ
                ))
            }

            if (missingPermissions.isEmpty()) {
                Log.d(TAG, "All permissions already granted")
                callback(currentState)
                return
            }

            Log.d(TAG, "Requesting ${missingPermissions.size} missing permissions")

            val permManager = HealthPermissionManager(healthDataStore)
            permManager.requestPermissions(missingPermissions, activity)
                .setResultListener { result ->
                    Log.d(TAG, "Permission request result received")
                    // Re-check permissions after the dialog
                    val updatedState = checkPermissions()
                    callback(updatedState)
                }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to request permissions", e)
            callback(PermissionState(
                steps = PermissionStatus.UNAVAILABLE,
                heartRate = PermissionStatus.UNAVAILABLE,
                sleep = PermissionStatus.UNAVAILABLE,
                exercise = PermissionStatus.UNAVAILABLE
            ))
        }
    }

    /**
     * Check if a specific data type has read permission granted.
     */
    fun isPermissionGranted(dataType: String): Boolean {
        return try {
            val permManager = HealthPermissionManager(healthDataStore)
            val key = PermissionKey(dataType, PermissionType.READ)
            val result = permManager.isPermissionAcquired(setOf(key))
            result[key] ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check permission for $dataType", e)
            false
        }
    }
}

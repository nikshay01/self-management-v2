package com.selfmanagement.samsungbridge.samsung

import android.util.Log
import com.samsung.android.sdk.healthdata.HealthConstants
import com.samsung.android.sdk.healthdata.HealthDataResolver
import com.samsung.android.sdk.healthdata.HealthDataStore
import com.selfmanagement.samsungbridge.models.HealthRecord
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for mapping Samsung Health SDK data to our canonical models.
 *
 * This class centralizes ALL data extraction from Samsung's HealthData objects
 * so that no other part of the application interacts with Samsung SDK types directly.
 *
 * All timestamp handling preserves timezone information using TIME_OFFSET
 * from Samsung Health records, and outputs ISO-8601 format.
 */
object SamsungDataMapper {

    private const val TAG = "SamsungDataMapper"

    /**
     * ISO-8601 date-time formatter with timezone.
     */
    private val isoFormatter: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    /**
     * Convert a Samsung Health UTC millisecond timestamp + time offset
     * to an ISO-8601 string with timezone information.
     *
     * Samsung stores timestamps as UTC milliseconds + TIME_OFFSET (ms from UTC).
     * We convert to a proper ISO-8601 timestamp with the offset applied.
     *
     * @param utcMillis UTC milliseconds timestamp from Samsung Health
     * @param timeOffsetMs Time offset in milliseconds (e.g., +19800000 for IST +5:30)
     * @return ISO-8601 formatted timestamp string
     */
    fun toIso8601(utcMillis: Long, timeOffsetMs: Long = 0L): String {
        return try {
            if (timeOffsetMs != 0L) {
                // Create a formatter with the specific offset
                val offsetHours = (timeOffsetMs / 3600000).toInt()
                val offsetMinutes = ((Math.abs(timeOffsetMs) % 3600000) / 60000).toInt()
                val sign = if (timeOffsetMs >= 0) "+" else "-"
                val tzId = String.format("GMT%s%02d:%02d", sign, Math.abs(offsetHours), offsetMinutes)
                val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
                formatter.timeZone = TimeZone.getTimeZone(tzId)
                formatter.format(java.util.Date(utcMillis))
            } else {
                isoFormatter.format(java.util.Date(utcMillis))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to format timestamp: utcMillis=$utcMillis, offset=$timeOffsetMs", e)
            // Fallback: UTC without offset
            isoFormatter.format(java.util.Date(utcMillis))
        }
    }

    /**
     * Safely extract a String value from a Samsung HealthData object.
     */
    fun safeGetString(data: HealthDataResolver.ReadResult.DataIterator.HealthData, key: String): String? {
        return try {
            data.getString(key)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read string '$key'", e)
            null
        }
    }

    /**
     * Safely extract an Int value from a Samsung HealthData object.
     */
    fun safeGetInt(data: HealthDataResolver.ReadResult.DataIterator.HealthData, key: String): Int? {
        return try {
            data.getInt(key)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read int '$key'", e)
            null
        }
    }

    /**
     * Safely extract a Long value from a Samsung HealthData object.
     */
    fun safeGetLong(data: HealthDataResolver.ReadResult.DataIterator.HealthData, key: String): Long? {
        return try {
            data.getLong(key)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read long '$key'", e)
            null
        }
    }

    /**
     * Safely extract a Float value from a Samsung HealthData object.
     */
    fun safeGetFloat(data: HealthDataResolver.ReadResult.DataIterator.HealthData, key: String): Float? {
        return try {
            data.getFloat(key)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read float '$key'", e)
            null
        }
    }

    /**
     * Safely extract a Double value from a Samsung HealthData object.
     */
    fun safeGetDouble(data: HealthDataResolver.ReadResult.DataIterator.HealthData, key: String): Double? {
        return try {
            data.getDouble(key)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read double '$key'", e)
            null
        }
    }

    /**
     * Extract the Samsung record UUID. Used for deduplication.
     * Falls back to a deterministic ID if UUID is unavailable.
     *
     * @param data The Samsung HealthData record
     * @param dataType The health data type (for fallback ID generation)
     * @return UUID string, or deterministic fallback
     */
    fun extractRecordId(
        data: HealthDataResolver.ReadResult.DataIterator.HealthData,
        dataType: String
    ): String {
        val uuid = safeGetString(data, HealthConstants.Common.UUID)
        if (!uuid.isNullOrBlank()) {
            return uuid
        }

        // Fallback: generate a deterministic ID from stable record attributes
        val startTime = safeGetLong(data, HealthConstants.SessionMeasurement.START_TIME) ?: 0L
        val endTime = safeGetLong(data, HealthConstants.SessionMeasurement.END_TIME) ?: 0L
        val packageName = safeGetString(data, HealthConstants.Common.PACKAGE_NAME) ?: "unknown"
        val deviceUuid = safeGetString(data, HealthConstants.Common.DEVICE_UUID) ?: "unknown"

        val deterministicId = "${dataType}_${startTime}_${endTime}_${packageName}_${deviceUuid}"
        Log.d(TAG, "No UUID for record, using deterministic ID: $deterministicId")
        return deterministicId
    }

    /**
     * Extract device identifier from a Samsung record.
     */
    fun extractDevice(data: HealthDataResolver.ReadResult.DataIterator.HealthData): String? {
        return safeGetString(data, HealthConstants.Common.DEVICE_UUID)
    }

    /**
     * Extract start time as ISO-8601 string with timezone.
     */
    fun extractStartTime(data: HealthDataResolver.ReadResult.DataIterator.HealthData): String? {
        val startTime = safeGetLong(data, HealthConstants.SessionMeasurement.START_TIME) ?: return null
        val timeOffset = safeGetLong(data, HealthConstants.SessionMeasurement.TIME_OFFSET) ?: 0L
        return toIso8601(startTime, timeOffset)
    }

    /**
     * Extract end time as ISO-8601 string with timezone.
     */
    fun extractEndTime(data: HealthDataResolver.ReadResult.DataIterator.HealthData): String? {
        val endTime = safeGetLong(data, HealthConstants.SessionMeasurement.END_TIME) ?: return null
        val timeOffset = safeGetLong(data, HealthConstants.SessionMeasurement.TIME_OFFSET) ?: 0L
        return toIso8601(endTime, timeOffset)
    }
}

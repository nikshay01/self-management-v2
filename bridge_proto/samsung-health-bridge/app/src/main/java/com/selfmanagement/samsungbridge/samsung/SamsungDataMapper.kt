package com.selfmanagement.samsungbridge.samsung

import com.samsung.android.sdk.health.data.data.DataSource
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.selfmanagement.samsungbridge.models.HealthRecord
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Utility for mapping Samsung Health Data SDK data to our canonical models.
 *
 * This class centralizes ALL data extraction from Samsung's HealthDataPoint objects
 * so that no other part of the application interacts with Samsung SDK types directly.
 */
object SamsungDataMapper {

    private const val TAG = "SamsungDataMapper"
    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    /**
     * Convert epoch millisecond timestamp to ISO-8601 string in UTC.
     */
    fun toIso8601(utcMillis: Long, timeOffsetMs: Long = 0L): String {
        val instant = Instant.ofEpochMilli(utcMillis)
        val offset = ZoneOffset.ofTotalSeconds((timeOffsetMs / 1000).toInt())
        return OffsetDateTime.ofInstant(instant, offset).format(isoFormatter)
    }

    /**
     * Convert java.time.Instant + ZoneOffset to ISO-8601 string.
     */
    fun formatInstant(instant: Instant, zoneOffset: ZoneOffset? = null): String {
        val offset = zoneOffset ?: ZoneOffset.UTC
        return OffsetDateTime.ofInstant(instant, offset).format(isoFormatter)
    }

    /**
     * Extract record UUID or client ID from HealthDataPoint.
     */
    fun extractRecordId(point: HealthDataPoint, dataType: String): String {
        val uid = point.uid
        if (!uid.isNullOrBlank()) {
            return uid
        }
        val clientDataId = point.clientDataId
        if (!clientDataId.isNullOrBlank()) {
            return clientDataId
        }

        val start = point.startTime.toEpochMilli()
        val end = point.endTime?.toEpochMilli() ?: start
        val deviceId = point.dataSource?.deviceId ?: "unknown"
        val appId = point.dataSource?.appId ?: "unknown"
        return "${dataType}_${start}_${end}_${appId}_${deviceId}"
    }

    /**
     * Extract device identifier from DataSource.
     */
    fun extractDevice(dataSource: DataSource?): String? {
        return dataSource?.deviceId
    }
}

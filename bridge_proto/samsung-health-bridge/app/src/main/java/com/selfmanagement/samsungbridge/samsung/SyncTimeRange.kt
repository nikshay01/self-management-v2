package com.selfmanagement.samsungbridge.samsung

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Reusable local-timezone query window for Samsung Health reads.
 *
 * Individual readers must use this range rather than hard-coding "today".
 * Current-day sync is [todayUntilNow]; historical backfill uses
 * [inclusiveLocalDates] or an explicit start/end.
 */
data class SyncTimeRange(
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val zoneId: ZoneId = ZoneId.systemDefault()
) {
    init {
        require(!endDateTime.isBefore(startDateTime)) {
            "endDateTime ($endDateTime) must not be before startDateTime ($startDateTime)"
        }
    }

    val startInstant: Instant
        get() = startDateTime.atZone(zoneId).toInstant()

    val endInstant: Instant
        get() = endDateTime.atZone(zoneId).toInstant()

    val startTimeMillis: Long
        get() = startInstant.toEpochMilli()

    val endTimeMillis: Long
        get() = endInstant.toEpochMilli()

    override fun toString(): String {
        return "start=$startDateTime end=$endDateTime zone=$zoneId"
    }

    companion object {
        /**
         * Start of the current local calendar day through [now] (inclusive).
         * Preserves the existing "Sync Now" window.
         */
        fun todayUntilNow(
            zoneId: ZoneId = ZoneId.systemDefault(),
            now: Instant = Instant.now()
        ): SyncTimeRange {
            val nowLocal = LocalDateTime.ofInstant(now, zoneId)
            return SyncTimeRange(
                startDateTime = nowLocal.toLocalDate().atStartOfDay(),
                endDateTime = nowLocal,
                zoneId = zoneId
            )
        }

        /**
         * Inclusive local-date window: [startDate] 00:00:00 through [endDate] 23:59:59.
         */
        fun inclusiveLocalDates(
            startDate: LocalDate,
            endDate: LocalDate,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): SyncTimeRange {
            return SyncTimeRange(
                startDateTime = startDate.atStartOfDay(),
                endDateTime = LocalDateTime.of(endDate, LocalTime.of(23, 59, 59)),
                zoneId = zoneId
            )
        }

        /**
         * Last [days] local calendar days through now (inclusive of today).
         * Compatible with future 7/30/90-day backfills; not wired to UI yet.
         */
        fun lastNCalendarDaysUntilNow(
            days: Int,
            zoneId: ZoneId = ZoneId.systemDefault(),
            now: Instant = Instant.now()
        ): SyncTimeRange {
            require(days >= 1) { "days must be >= 1" }
            val nowLocal = LocalDateTime.ofInstant(now, zoneId)
            val startDate = nowLocal.toLocalDate().minusDays((days - 1).toLong())
            return SyncTimeRange(
                startDateTime = startDate.atStartOfDay(),
                endDateTime = nowLocal,
                zoneId = zoneId
            )
        }

        /**
         * Convert epoch-millis bounds using the device local zone.
         * Keeps existing millis-based callers working.
         */
        fun fromEpochMillis(
            startTimeMillis: Long,
            endTimeMillis: Long,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): SyncTimeRange {
            return SyncTimeRange(
                startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimeMillis), zoneId),
                endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimeMillis), zoneId),
                zoneId = zoneId
            )
        }
    }
}

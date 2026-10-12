package com.selfmanagement.samsungbridge.samsung

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.util.Log
import com.selfmanagement.samsungbridge.models.AppUsageDay
import com.selfmanagement.samsungbridge.models.AppUsageRecord
import com.selfmanagement.samsungbridge.models.ScreenTimeDay
import com.selfmanagement.samsungbridge.models.ScreenTimeRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Reads the device interactive-screen total and per-app foreground use through Android Usage Access. */
class ScreenTimeReader(context: Context) {
    companion object {
        private const val TAG = "ScreenTimeReader"
        private const val MAX_EVENT_QUERY_RANGE_MS = 4L * 24 * 60 * 60 * 1000
        private const val EVENT_RECONSTRUCTION_LOOKBACK_MS = 24L * 60 * 60 * 1000
    }

    private val appContext = context.applicationContext
    private val usageStatsManager =
        appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    @Suppress("DEPRECATION")
    fun hasUsageAccess(): Boolean {
        return try {
            val appOpsManager = appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                appContext.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.w(TAG, "Unable to check Usage Access state", e)
            false
        }
    }

    fun read(range: SyncTimeRange, callback: (ScreenTimeRecord) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            if (!hasUsageAccess()) {
                Log.i(TAG, "Usage Access is not granted")
                callback(emptyReport(range, accessGranted = false))
                return@launch
            }

            try {
                val startMillis = range.startTimeMillis
                // Android's usage APIs take an exclusive end; include the requested end instant.
                val endExclusiveMillis = Math.addExact(range.endTimeMillis, 1L)
                val startDate = range.startDateTime.toLocalDate()
                val endDate = range.endDateTime.toLocalDate()
                val packageByDate = linkedMapOf<String, MutableMap<LocalDate, MutableUsage>>()

                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startMillis,
                    endExclusiveMillis
                ).orEmpty()

                stats.forEach { item ->
                    val day = Instant.ofEpochMilli(item.firstTimeStamp).atZone(range.zoneId).toLocalDate()
                    if (day < startDate || day > endDate) return@forEach

                    val daily = packageByDate.getOrPut(item.packageName) { linkedMapOf() }
                        .getOrPut(day) { MutableUsage() }
                    daily.foregroundMs += item.totalTimeInForeground
                    daily.visibleMs += item.totalTimeVisible
                    daily.foregroundServiceMs += item.totalTimeForegroundServiceUsed
                    daily.lastUsedAt = maxTimestamp(daily.lastUsedAt, item.lastTimeUsed)
                    daily.lastVisibleAt = maxTimestamp(daily.lastVisibleAt, item.lastTimeVisible)
                    daily.lastForegroundServiceUsedAt = maxTimestamp(
                        daily.lastForegroundServiceUsedAt,
                        item.lastTimeForegroundServiceUsed
                    )
                }

                // Daily aggregates can omit the still-open current day on some devices. Android
                // retains recent usage events, so reconstruct foreground time for short ranges.
                val recentUsage = if (endExclusiveMillis - startMillis <= MAX_EVENT_QUERY_RANGE_MS) {
                    readRecentEvents(startMillis, endExclusiveMillis, range.zoneId)
                } else {
                    null
                }
                recentUsage?.appsByPackageAndDate?.forEach { (packageName, days) ->
                    days.forEach { (day, eventUsage) ->
                        if (day >= startDate && day <= endDate) {
                        val daily = packageByDate.getOrPut(packageName) { linkedMapOf() }
                            .getOrPut(day) { MutableUsage() }
                        if (daily.foregroundMs == 0L) daily.foregroundMs = eventUsage.foregroundMs
                        if (daily.visibleMs == 0L) daily.visibleMs = eventUsage.visibleMs
                        if (daily.foregroundServiceMs == 0L) {
                                daily.foregroundServiceMs = eventUsage.foregroundServiceMs
                            }
                            eventUsage.lastUsedAt?.let {
                                daily.lastUsedAt = maxTimestamp(daily.lastUsedAt, it)
                            }
                            eventUsage.lastForegroundServiceUsedAt?.let {
                                daily.lastForegroundServiceUsedAt = maxTimestamp(
                                    daily.lastForegroundServiceUsedAt,
                                    it
                                )
                            }
                        }
                    }
                }

                val apps = packageByDate.mapNotNull { (packageName, days) ->
                    val dayRecords = days.entries
                        .sortedBy { it.key }
                        .filter { (_, usage) ->
                            usage.foregroundMs > 0L || usage.visibleMs > 0L || usage.foregroundServiceMs > 0L
                        }
                        .map { (date, usage) ->
                            AppUsageDay(
                                localDate = date.toString(),
                                foregroundMs = usage.foregroundMs,
                                visibleMs = usage.visibleMs,
                                foregroundServiceMs = usage.foregroundServiceMs,
                                lastUsedAt = usage.lastUsedAt?.toIso(range.zoneId),
                                lastVisibleAt = usage.lastVisibleAt?.toIso(range.zoneId),
                                lastForegroundServiceUsedAt = usage.lastForegroundServiceUsedAt?.toIso(range.zoneId)
                            )
                        }
                    val foregroundMs = days.values.sumOf { it.foregroundMs }
                    val visibleMs = days.values.sumOf { it.visibleMs }
                    val foregroundServiceMs = days.values.sumOf { it.foregroundServiceMs }
                    if (foregroundMs == 0L && visibleMs == 0L && foregroundServiceMs == 0L) {
                        return@mapNotNull null
                    }

                    AppUsageRecord(
                        packageName = packageName,
                        appName = resolveAppName(packageName),
                        totalForegroundMs = foregroundMs,
                        totalVisibleMs = visibleMs,
                        totalForegroundServiceMs = foregroundServiceMs,
                        lastUsedAt = days.values.mapNotNull { it.lastUsedAt }.maxOrNull()?.toIso(range.zoneId),
                        lastVisibleAt = days.values.mapNotNull { it.lastVisibleAt }.maxOrNull()?.toIso(range.zoneId),
                        lastForegroundServiceUsedAt = days.values
                            .mapNotNull { it.lastForegroundServiceUsedAt }
                            .maxOrNull()
                            ?.toIso(range.zoneId),
                        dailyUsage = dayRecords
                    )
                }.sortedWith(compareByDescending<AppUsageRecord> { it.totalForegroundMs }.thenBy { it.appName })

                val screenByDate = linkedMapOf<LocalDate, Long>()
                usageStatsManager.queryEventStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startMillis,
                    endExclusiveMillis
                ).orEmpty()
                    .filter { it.eventType == android.app.usage.UsageEvents.Event.SCREEN_INTERACTIVE }
                    .forEach { eventStats ->
                        val day = Instant.ofEpochMilli(eventStats.firstTimeStamp).atZone(range.zoneId).toLocalDate()
                        if (day >= startDate && day <= endDate) {
                            screenByDate[day] = (screenByDate[day] ?: 0L) + eventStats.totalTime
                        }
                    }

                recentUsage?.screenInteractiveMsByDate?.forEach { (day, duration) ->
                    if ((screenByDate[day] ?: 0L) == 0L) screenByDate[day] = duration
                }

                val dailyScreen = screenByDate.entries
                    .sortedBy { it.key }
                    .map { (date, duration) -> ScreenTimeDay(date.toString(), duration) }
                val result = ScreenTimeRecord(
                    accessGranted = true,
                    timestampStart = SamsungDataMapper.formatLocalDateTime(range.startDateTime, range.zoneId),
                    timestampEnd = SamsungDataMapper.formatLocalDateTime(range.endDateTime, range.zoneId),
                    totalScreenInteractiveMs = dailyScreen.sumOf { it.interactiveMs },
                    dailyScreenInteractive = dailyScreen,
                    apps = apps
                )
                Log.i(
                    TAG,
                    "Retrieved screen-time summary: ${apps.size} apps, " +
                        "${result.totalScreenInteractiveMs} ms interactive, ${result.totalAppForegroundMs} ms app foreground"
                )
                if (recentUsage != null) {
                    Log.i(
                        TAG,
                        "Recent-event fallback: ${recentUsage.eventCount} events, " +
                            "${recentUsage.appsByPackageAndDate.size} apps"
                    )
                }
                callback(result)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read screen-time usage for range $range", e)
                callback(emptyReport(range, accessGranted = hasUsageAccess(), error = e.message))
            }
        }
    }

    private fun emptyReport(
        range: SyncTimeRange,
        accessGranted: Boolean,
        error: String? = null
    ): ScreenTimeRecord = ScreenTimeRecord(
        accessGranted = accessGranted,
        timestampStart = SamsungDataMapper.formatLocalDateTime(range.startDateTime, range.zoneId),
        timestampEnd = SamsungDataMapper.formatLocalDateTime(range.endDateTime, range.zoneId),
        error = error
    )

    private fun resolveAppName(packageName: String): String = try {
        val appInfo = appContext.packageManager.getApplicationInfo(packageName, 0)
        appContext.packageManager.getApplicationLabel(appInfo).toString()
    } catch (_: Exception) {
        packageName
    }

    private fun maxTimestamp(previous: Long?, value: Long): Long? =
        if (value <= 0L) previous else maxOf(previous ?: value, value)

    private fun readRecentEvents(
        rangeStartMillis: Long,
        rangeEndExclusiveMillis: Long,
        zoneId: ZoneId
    ): RecentUsage? {
        return try {
            val eventQueryStart = (rangeStartMillis - EVENT_RECONSTRUCTION_LOOKBACK_MS).coerceAtLeast(0L)
            val events = usageStatsManager.queryEvents(eventQueryStart, rangeEndExclusiveMillis)
            val apps = linkedMapOf<String, MutableMap<LocalDate, MutableUsage>>()
            val activeActivities = linkedMapOf<String, MutableSet<String>>()
            val activityStartedAt = linkedMapOf<String, Long>()
            val foregroundIntervals = mutableListOf<AppInterval>()
            val visibleActivities = linkedMapOf<String, MutableSet<String>>()
            val visibleStartedAt = linkedMapOf<String, Long>()
            val visibleIntervals = mutableListOf<AppInterval>()
            val activeServices = linkedMapOf<String, MutableSet<String>>()
            val serviceStartedAt = linkedMapOf<String, Long>()
            val screenInteractiveByDate = linkedMapOf<LocalDate, Long>()
            val screenInteractiveIntervals = mutableListOf<TimeInterval>()
            var screenInteractiveStartedAt: Long? = null
            var eventCount = 0

            fun usageFor(packageName: String, date: LocalDate): MutableUsage =
                apps.getOrPut(packageName) { linkedMapOf() }.getOrPut(date) { MutableUsage() }

            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                if (!events.getNextEvent(event)) break
                eventCount++
                val timestamp = event.timeStamp
                val packageName = event.packageName

                when (event.eventType) {
                    UsageEvents.Event.SCREEN_INTERACTIVE -> {
                        if (screenInteractiveStartedAt == null) screenInteractiveStartedAt = timestamp
                    }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                        val startedAt = screenInteractiveStartedAt
                        if (startedAt != null) {
                            screenInteractiveIntervals += TimeInterval(startedAt, timestamp)
                            addDurationByLocalDay(
                                startedAt,
                                timestamp,
                                rangeStartMillis,
                                rangeEndExclusiveMillis,
                                zoneId
                            ) { day, duration ->
                                screenInteractiveByDate[day] = (screenInteractiveByDate[day] ?: 0L) + duration
                            }
                        }
                        screenInteractiveStartedAt = null
                    }
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        if (!packageName.isNullOrBlank()) {
                            val token = event.className ?: "<activity>"
                            val active = activeActivities.getOrPut(packageName) { linkedSetOf() }
                            if (active.add(token) && active.size == 1) {
                                activityStartedAt[packageName] = timestamp
                            }
                            val visible = visibleActivities.getOrPut(packageName) { linkedSetOf() }
                            if (visible.add(token) && visible.size == 1) {
                                visibleStartedAt[packageName] = timestamp
                            }
                            val day = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
                            val daily = usageFor(packageName, day)
                            daily.lastUsedAt = maxTimestamp(daily.lastUsedAt, timestamp)
                            daily.lastVisibleAt = maxTimestamp(daily.lastVisibleAt, timestamp)
                        }
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        if (!packageName.isNullOrBlank()) {
                            val active = activeActivities[packageName]
                            active?.remove(event.className ?: "<activity>")
                            if (active != null && active.isEmpty()) {
                                activityStartedAt.remove(packageName)?.let { startedAt ->
                                    foregroundIntervals += AppInterval(packageName, startedAt, timestamp)
                                }
                                activeActivities.remove(packageName)
                            }
                        }
                    }
                    UsageEvents.Event.ACTIVITY_STOPPED -> {
                        if (!packageName.isNullOrBlank()) {
                            val visible = visibleActivities[packageName]
                            visible?.remove(event.className ?: "<activity>")
                            if (visible != null && visible.isEmpty()) {
                                visibleStartedAt.remove(packageName)?.let { startedAt ->
                                    visibleIntervals += AppInterval(packageName, startedAt, timestamp)
                                    val day = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
                                    val daily = usageFor(packageName, day)
                                    daily.lastVisibleAt = maxTimestamp(daily.lastVisibleAt, timestamp)
                                }
                                visibleActivities.remove(packageName)
                            }
                        }
                    }
                    UsageEvents.Event.FOREGROUND_SERVICE_START -> {
                        if (!packageName.isNullOrBlank()) {
                            val token = event.className ?: "<service>"
                            val active = activeServices.getOrPut(packageName) { linkedSetOf() }
                            if (active.add(token) && active.size == 1) serviceStartedAt[packageName] = timestamp
                            val day = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
                            val daily = usageFor(packageName, day)
                            daily.lastForegroundServiceUsedAt = maxTimestamp(
                                daily.lastForegroundServiceUsedAt,
                                timestamp
                            )
                        }
                    }
                    UsageEvents.Event.FOREGROUND_SERVICE_STOP -> {
                        if (!packageName.isNullOrBlank()) {
                            val active = activeServices[packageName]
                            active?.remove(event.className ?: "<service>")
                            if (active != null && active.isEmpty()) {
                                serviceStartedAt.remove(packageName)?.let { startedAt ->
                                    addDurationByLocalDay(
                                        startedAt,
                                        timestamp,
                                        rangeStartMillis,
                                        rangeEndExclusiveMillis,
                                        zoneId
                                    ) { day, duration ->
                                        val daily = usageFor(packageName, day)
                                        daily.foregroundServiceMs += duration
                                        daily.lastForegroundServiceUsedAt = maxTimestamp(
                                            daily.lastForegroundServiceUsedAt,
                                            timestamp
                                        )
                                    }
                                }
                                activeServices.remove(packageName)
                            }
                        }
                    }
                }
            }

            activityStartedAt.forEach { (packageName, startedAt) ->
                foregroundIntervals += AppInterval(packageName, startedAt, rangeEndExclusiveMillis)
            }
            visibleStartedAt.forEach { (packageName, startedAt) ->
                visibleIntervals += AppInterval(packageName, startedAt, rangeEndExclusiveMillis)
            }
            serviceStartedAt.forEach { (packageName, startedAt) ->
                addDurationByLocalDay(
                    startedAt,
                    rangeEndExclusiveMillis,
                    rangeStartMillis,
                    rangeEndExclusiveMillis,
                    zoneId
                ) { day, duration -> usageFor(packageName, day).foregroundServiceMs += duration }
            }
            screenInteractiveStartedAt?.let { startedAt ->
                screenInteractiveIntervals += TimeInterval(startedAt, rangeEndExclusiveMillis)
                addDurationByLocalDay(
                    startedAt,
                    rangeEndExclusiveMillis,
                    rangeStartMillis,
                    rangeEndExclusiveMillis,
                    zoneId
                ) { day, duration ->
                    screenInteractiveByDate[day] = (screenInteractiveByDate[day] ?: 0L) + duration
                }
            }

            // A foreground Activity can remain resumed while the display is off. Intersect app
            // intervals with the screen-interactive intervals so app screen time never counts
            // time when the user could not see the app.
            foregroundIntervals.forEach { appInterval ->
                screenInteractiveIntervals.forEach { screenInterval ->
                    val overlapStart = maxOf(appInterval.startedAtMillis, screenInterval.startedAtMillis)
                    val overlapEnd = minOf(appInterval.endedAtMillis, screenInterval.endedAtMillis)
                    if (overlapStart < overlapEnd) {
                        addDurationByLocalDay(
                            overlapStart,
                            overlapEnd,
                            rangeStartMillis,
                            rangeEndExclusiveMillis,
                            zoneId
                        ) { day, duration ->
                            usageFor(appInterval.packageName, day).foregroundMs += duration
                        }
                    }
                }
            }
            visibleIntervals.forEach { appInterval ->
                screenInteractiveIntervals.forEach { screenInterval ->
                    val overlapStart = maxOf(appInterval.startedAtMillis, screenInterval.startedAtMillis)
                    val overlapEnd = minOf(appInterval.endedAtMillis, screenInterval.endedAtMillis)
                    if (overlapStart < overlapEnd) {
                        addDurationByLocalDay(
                            overlapStart,
                            overlapEnd,
                            rangeStartMillis,
                            rangeEndExclusiveMillis,
                            zoneId
                        ) { day, duration ->
                            usageFor(appInterval.packageName, day).visibleMs += duration
                        }
                    }
                }
            }

            RecentUsage(apps, screenInteractiveByDate, eventCount)
        } catch (e: Exception) {
            Log.w(TAG, "Recent UsageEvents unavailable; keeping aggregate usage results", e)
            null
        }
    }

    private fun addDurationByLocalDay(
        startedAtMillis: Long,
        endedAtMillis: Long,
        rangeStartMillis: Long,
        rangeEndExclusiveMillis: Long,
        zoneId: ZoneId,
        addDuration: (LocalDate, Long) -> Unit
    ) {
        var cursor = maxOf(startedAtMillis, rangeStartMillis)
        val end = minOf(endedAtMillis, rangeEndExclusiveMillis)
        while (cursor < end) {
            val date = Instant.ofEpochMilli(cursor).atZone(zoneId).toLocalDate()
            val nextDateMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val segmentEnd = minOf(end, nextDateMillis)
            addDuration(date, segmentEnd - cursor)
            cursor = segmentEnd
        }
    }

    private fun Long.toIso(zoneId: ZoneId): String =
        Instant.ofEpochMilli(this).atZone(zoneId).toOffsetDateTime().toString()

    private data class MutableUsage(
        var foregroundMs: Long = 0L,
        var visibleMs: Long = 0L,
        var foregroundServiceMs: Long = 0L,
        var lastUsedAt: Long? = null,
        var lastVisibleAt: Long? = null,
        var lastForegroundServiceUsedAt: Long? = null
    )

    private data class RecentUsage(
        val appsByPackageAndDate: Map<String, Map<LocalDate, MutableUsage>>,
        val screenInteractiveMsByDate: Map<LocalDate, Long>,
        val eventCount: Int
    )

    private data class AppInterval(
        val packageName: String,
        val startedAtMillis: Long,
        val endedAtMillis: Long
    )

    private data class TimeInterval(
        val startedAtMillis: Long,
        val endedAtMillis: Long
    )
}

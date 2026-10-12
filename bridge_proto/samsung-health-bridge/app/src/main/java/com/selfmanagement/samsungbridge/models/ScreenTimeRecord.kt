package com.selfmanagement.samsungbridge.models

/** Local Android usage report for one requested date range. */
data class ScreenTimeRecord(
    val accessGranted: Boolean = false,
    val timestampStart: String? = null,
    val timestampEnd: String? = null,
    /** Time the device screen was interactive, as reported by Android's daily event statistics. */
    val totalScreenInteractiveMs: Long = 0L,
    val dailyScreenInteractive: List<ScreenTimeDay> = emptyList(),
    /** Per-package app usage with its local-day breakdown. */
    val apps: List<AppUsageRecord> = emptyList(),
    val error: String? = null
) {
    val totalAppForegroundMs: Long
        get() = apps.sumOf { it.totalForegroundMs }

    val totalAppVisibleMs: Long
        get() = apps.sumOf { it.totalVisibleMs }

    val totalAppForegroundServiceMs: Long
        get() = apps.sumOf { it.totalForegroundServiceMs }
}

data class ScreenTimeDay(
    val localDate: String,
    val interactiveMs: Long
)

data class AppUsageRecord(
    val packageName: String,
    val appName: String,
    val totalForegroundMs: Long,
    val totalVisibleMs: Long,
    val totalForegroundServiceMs: Long,
    val lastUsedAt: String?,
    val lastVisibleAt: String?,
    val lastForegroundServiceUsedAt: String?,
    val dailyUsage: List<AppUsageDay>
)

data class AppUsageDay(
    val localDate: String,
    val foregroundMs: Long,
    val visibleMs: Long,
    val foregroundServiceMs: Long,
    val lastUsedAt: String?,
    val lastVisibleAt: String?,
    val lastForegroundServiceUsedAt: String?
)

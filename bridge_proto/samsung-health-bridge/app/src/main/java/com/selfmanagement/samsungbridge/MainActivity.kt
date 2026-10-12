package com.selfmanagement.samsungbridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.selfmanagement.samsungbridge.databinding.ActivityMainBinding
import com.selfmanagement.samsungbridge.databinding.ItemChartCardBinding
import com.selfmanagement.samsungbridge.databinding.ItemMetricBinding
import com.selfmanagement.samsungbridge.models.AppUsageRecord
import com.selfmanagement.samsungbridge.models.ExerciseRecord
import com.selfmanagement.samsungbridge.models.ScreenTimeRecord
import com.selfmanagement.samsungbridge.samsung.SamsungHealthManager
import com.selfmanagement.samsungbridge.samsung.SamsungPermissionManager
import com.selfmanagement.samsungbridge.samsung.ScreenTimeReader
import com.selfmanagement.samsungbridge.samsung.SyncTimeRange
import com.selfmanagement.samsungbridge.sync.SyncManager
import com.selfmanagement.samsungbridge.sync.SyncStateStore
import com.selfmanagement.samsungbridge.ui.DashboardChartView
import com.selfmanagement.samsungbridge.ui.RawJsonActivity
import com.selfmanagement.samsungbridge.ui.RawJsonStore
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    companion object { private const val TAG = "MainActivity" }
    private lateinit var binding: ActivityMainBinding
    private lateinit var healthManager: SamsungHealthManager
    private lateinit var apiClient: com.selfmanagement.samsungbridge.network.ApiClient
    private lateinit var syncManager: SyncManager
    private lateinit var screenTimeReader: ScreenTimeReader
    private var expandedExercises = false
    private var expandedApps = false
    private var permissionDetailsVisible = false
    private var samsungPermissionCount = 0
    private var exerciseRecords: List<ExerciseRecord> = emptyList()
    private var appRecords: List<AppUsageRecord> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        healthManager = SamsungHealthManager(this)
        screenTimeReader = ScreenTimeReader(this)
        apiClient = com.selfmanagement.samsungbridge.network.ApiClient(this)
        val syncApi = com.selfmanagement.samsungbridge.network.HealthSyncApi(apiClient)
        syncManager = SyncManager(this, healthManager, syncApi)

        binding.btnRequestPermissions.setOnClickListener { onRequestPermissions() }
        binding.btnGrantScreenTimeAccess.setOnClickListener { onGrantScreenTimeAccess() }
        binding.btnSyncNow.setOnClickListener { onSyncNow() }
        binding.btnSyncHistorical.setOnClickListener { onSyncHistorical() }
        binding.btnTogglePermissions.setOnClickListener { togglePermissions() }
        binding.btnToggleExercises.setOnClickListener { expandedExercises = !expandedExercises; renderExercises() }
        binding.btnToggleApps.setOnClickListener { expandedApps = !expandedApps; renderApps() }
        binding.btnRawJson.setOnClickListener {
            if (RawJsonStore.isAvailable()) startActivity(Intent(this, RawJsonActivity::class.java))
            else Toast.makeText(this, "Sync data first", Toast.LENGTH_SHORT).show()
        }
        binding.cardStepsChart.tvChartTitle.text = "Steps by day"
        binding.cardHeartChart.tvChartTitle.text = "Heart rate trend"
        binding.cardSleepChart.tvChartTitle.text = "Sleep stages"
        binding.cardScreenChart.tvChartTitle.text = "Top apps"
        setEmptyCharts()
        checkSamsungHealth()
        updateScreenTimeAccessDisplay()
        updateLastSyncDisplay()
    }

    override fun onDestroy() { super.onDestroy(); healthManager.disconnect() }

    override fun onResume() {
        super.onResume()
        if (::screenTimeReader.isInitialized && ::binding.isInitialized) updateScreenTimeAccessDisplay()
    }

    private fun checkSamsungHealth() {
        if (!healthManager.isAvailable()) {
            updateConnectionStatus("Samsung Health: Not installed", false)
            binding.tvPermissionSummary.text = "Install Samsung Health to connect"
            showError("Samsung Health app is not installed on this device")
            return
        }
        updateConnectionStatus("Samsung Health: Connecting…", false)
        healthManager.connect { connected -> runOnUiThread {
            if (connected) {
                updateConnectionStatus("Samsung Health: Connected", true)
                checkPermissions()
            } else {
                val error = healthManager.lastError ?: "Unknown error"
                updateConnectionStatus("Samsung Health: $error", false)
                binding.tvPermissionSummary.text = "Could not read Samsung Health permissions"
                showError(error)
            }
        } }
    }

    private fun checkPermissions() {
        healthManager.getPermissions { state -> runOnUiThread { updatePermissionDisplay(state) } }
    }

    private fun onRequestPermissions() {
        if (healthManager.connectionState != SamsungHealthManager.ConnectionState.CONNECTED) {
            Toast.makeText(this, "Not connected to Samsung Health", Toast.LENGTH_SHORT).show(); return
        }
        binding.btnRequestPermissions.isEnabled = false
        healthManager.requestPermissions(this) { state -> runOnUiThread {
            updatePermissionDisplay(state); binding.btnRequestPermissions.isEnabled = true
            if (state.allGranted) Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
            else if (state.anyDenied) Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_SHORT).show()
        } }
    }

    private fun onGrantScreenTimeAccess() {
        try { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        catch (e: Exception) {
            Log.e(TAG, "Unable to open Usage Access settings", e)
            Toast.makeText(this, "Open Settings and enable Usage Access for Samsung Health Bridge", Toast.LENGTH_LONG).show()
        }
    }

    private fun togglePermissions() {
        permissionDetailsVisible = !permissionDetailsVisible
        binding.llPermissionDetails.visibility = if (permissionDetailsVisible) View.VISIBLE else View.GONE
        binding.btnTogglePermissions.text = if (permissionDetailsVisible) "Hide data access" else "Manage data access"
    }

    private fun updateScreenTimeAccessDisplay() {
        val granted = screenTimeReader.hasUsageAccess()
        binding.tvPermScreenTime.text = "Screen time: ${if (granted) "✓ Access granted" else "✗ Needs Usage Access"}"
        binding.btnGrantScreenTimeAccess.text = if (granted) "Manage screen-time access" else getString(R.string.btn_grant_screen_time_access)
        updatePermissionSummary()
    }

    private fun onSyncNow() = startRead(SyncTimeRange.todayUntilNow(), "Starting sync for today")

    private fun onSyncHistorical() {
        val range = SyncTimeRange.lastNCalendarDaysUntilNow(25)
        startRead(range, "Starting historical sync")
    }

    private fun startRead(range: SyncTimeRange, logLabel: String) {
        if (healthManager.connectionState != SamsungHealthManager.ConnectionState.CONNECTED) {
            Toast.makeText(this, "Not connected to Samsung Health", Toast.LENGTH_SHORT).show(); return
        }
        setSyncButtonsEnabled(false); hideError()
        binding.tvSyncResult.text = "Reading health and screen-time data…"
        Log.i(TAG, "$logLabel; ${range}")
        healthManager.readAll(range) { result ->
            val stageCount = result.sleep.sumOf { it.stages.size }
            Log.i(TAG, "Records: steps=${result.steps.size}, heartRate=${result.heartRate.size}, sleep=${result.sleep.size}, stages=$stageCount, exercise=${result.exercise.size}, apps=${result.screenTime.apps.size}")
            runOnUiThread {
                val rangeLabel = "${range.startDateTime} → ${range.endDateTime} · ${range.zoneId}"
                RawJsonStore.update(rangeLabel, result)
                displayResult(rangeLabel, result)
                binding.tvSyncResult.text = "${result.totalRecords} health records · ${result.screenTime.apps.size} apps · ${result.exercise.size} workouts"
                binding.btnRawJson.visibility = View.VISIBLE
            }
            if (apiClient.hasAuthToken()) {
                syncManager.sync(range.startTimeMillis, range.endTimeMillis) { syncResult -> runOnUiThread {
                    binding.tvSyncResult.text = syncResult.toDisplayString()
                    updateLastSyncDisplay(); setSyncButtonsEnabled(true)
                    if (!syncResult.success) showError(syncResult.error ?: "Sync failed")
                } }
            } else runOnUiThread {
                binding.tvSyncResult.text = "${result.totalRecords} health records · ${result.screenTime.apps.size} apps · Not uploaded (no sign-in token)"
                setSyncButtonsEnabled(true)
            }
        }
    }

    private fun displayResult(rangeLabel: String, result: com.selfmanagement.samsungbridge.samsung.SamsungHealthReader.ReadAllResult) {
        binding.tvRangeLabel.text = rangeLabel
        binding.tvSnapshotSubtitle.text = "${result.steps.size} step days · ${result.heartRate.size} heart-rate samples · ${result.sleep.size} sleep sessions"
        setMetric(binding.metricSteps, number(result.steps.sumOf { it.count.toLong() }), "Steps", "${result.steps.size} daily records")
        val workoutDistance = result.exercise.sumOf { it.distanceM ?: 0.0 } / 1000.0
        setMetric(binding.metricWorkouts, number(result.exercise.size.toLong()), "Workouts", if (workoutDistance > 0) String.format(Locale.getDefault(), "%.1f km total", workoutDistance) else "Exercise sessions")
        val avgSleepMs = result.sleep.map { it.durationMs.toDouble() }.averageOrZero().toLong()
        setMetric(binding.metricSleep, if (result.sleep.isEmpty()) "—" else formatDuration(avgSleepMs), "Avg sleep", "${result.sleep.size} sessions")
        val bpm = result.heartRate.map { it.heartRate }.averageOrZero()
        setMetric(binding.metricHeartRate, if (result.heartRate.isEmpty()) "—" else "${bpm.roundToInt()} bpm", "Heart rate", "${result.heartRate.size} samples")
        val screen = result.screenTime
        setMetric(binding.metricScreenTime, if (!screen.accessGranted) "Off" else formatDuration(screen.totalScreenInteractiveMs), "Screen time", if (screen.accessGranted) "Device interactive" else "Usage access needed")
        setMetric(binding.metricApps, if (!screen.accessGranted) "—" else number(screen.apps.size.toLong()), "Apps used", if (screen.accessGranted) "Tap an app for daily totals" else "No access")
        renderCharts(result)
        exerciseRecords = result.exercise.sortedByDescending { it.timestampStart }
        appRecords = screen.apps.sortedByDescending { it.totalForegroundMs }
        expandedExercises = false; expandedApps = false
        renderExercises(); renderApps()
    }

    private fun renderCharts(result: com.selfmanagement.samsungbridge.samsung.SamsungHealthReader.ReadAllResult) {
        val days = result.steps.sortedBy { it.timestampStart }.takeLast(14).map {
            DashboardChartView.Entry(it.timestampStart.take(10).takeLast(5), it.count.toDouble())
        }
        binding.cardStepsChart.chart.setData(days, DashboardChartView.ChartType.BAR, getColor(R.color.accent_teal))
        binding.cardStepsChart.tvChartSubtitle.text = if (days.isEmpty()) "Daily step totals" else "${days.size} most recent days"

        val heartDays = result.heartRate.groupBy { it.timestampStart.take(10) }.toSortedMap().map { (date, records) ->
            DashboardChartView.Entry(date.takeLast(5), records.map { it.heartRate }.average())
        }.takeLast(14)
        binding.cardHeartChart.chart.setData(heartDays, DashboardChartView.ChartType.LINE, getColor(R.color.chart_blue))
        binding.cardHeartChart.tvChartSubtitle.text = if (heartDays.isEmpty()) "Daily average · BPM" else "Daily average · ${heartDays.size} days"

        val stageTotals = result.sleep.flatMap { it.stages }.groupBy { it.stageName.lowercase(Locale.ROOT) }
            .mapValues { (_, stages) -> stages.sumOf { it.durationMs } / 60_000.0 }
        val stageColors = mapOf("awake" to getColor(R.color.chart_orange), "light" to getColor(R.color.chart_blue), "deep" to getColor(R.color.chart_purple), "rem" to getColor(R.color.accent_teal))
        val stageEntries = listOf("awake", "light", "deep", "rem").mapNotNull { key -> stageTotals[key]?.let { DashboardChartView.Entry(key.replaceFirstChar { c -> c.titlecase(Locale.ROOT) }, it, stageColors[key]) } }
        binding.cardSleepChart.chart.setData(stageEntries, DashboardChartView.ChartType.DONUT, getColor(R.color.accent_teal))
        binding.cardSleepChart.tvChartSubtitle.text = if (stageEntries.isEmpty()) "Stage durations for selected range" else "Time by recorded stage"

        val appPoints = result.screenTime.apps.sortedByDescending { it.totalForegroundMs }.take(5).map {
            DashboardChartView.Entry(it.appName, it.totalForegroundMs / 60_000.0)
        }
        binding.cardScreenChart.chart.setData(appPoints, DashboardChartView.ChartType.HORIZONTAL_BAR, getColor(R.color.chart_blue))
        binding.cardScreenChart.tvChartSubtitle.text = "Foreground minutes · top apps"
    }

    private fun setEmptyCharts() {
        val empty = emptyList<DashboardChartView.Entry>()
        listOf(binding.cardStepsChart, binding.cardHeartChart, binding.cardSleepChart, binding.cardScreenChart).forEach {
            it.chart.setData(empty, DashboardChartView.ChartType.BAR)
        }
    }

    private fun renderExercises() {
        binding.llExerciseList.removeAllViews()
        binding.tvExerciseSubtitle.text = if (exerciseRecords.isEmpty()) "Session metrics, samples, and route points" else "${exerciseRecords.size} sessions · tap one to inspect the full session JSON"
        exerciseRecords.take(if (expandedExercises) exerciseRecords.size else 5).forEach { exercise ->
            val title = exercise.exerciseTypeName ?: exercise.customType ?: exercise.exerciseType?.let { ExerciseRecord.exerciseTypeName(it) } ?: "Exercise"
            val details = buildList {
                exercise.durationMs?.let { add(formatDuration(it)) }
                exercise.distanceM?.let { add(String.format(Locale.getDefault(), "%.2f km", it / 1000.0)) }
                exercise.caloriesKcal?.let { add("${it.roundToInt()} kcal") }
                val samples = exercise.sessionDetails["samples"] as? List<*>
                if (!samples.isNullOrEmpty()) add("${samples.size} samples")
                val route = exercise.sessionDetails["route"] as? List<*>
                if (!route.isNullOrEmpty()) add("${route.size} route points")
            }.joinToString(" · ")
            addRow(binding.llExerciseList, title, "${exercise.timestampStart.replace('T', ' ').take(16)}${if (details.isNotBlank()) " · $details" else ""}") {
                showJsonDialog(title, com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(exercise))
            }
        }
        binding.btnToggleExercises.visibility = if (exerciseRecords.size > 5) View.VISIBLE else View.GONE
        binding.btnToggleExercises.text = if (expandedExercises) "Show fewer sessions" else "Show all ${exerciseRecords.size} sessions"
    }

    private fun renderApps() {
        binding.llAppUsage.removeAllViews()
        val screenAccess = screenTimeReader.hasUsageAccess()
        binding.tvAppsSubtitle.text = when { !screenAccess -> "Grant Usage Access to read on-device screen time"; appRecords.isEmpty() -> "No app usage in this range"; else -> "${appRecords.size} apps · tap any app for its full daily breakdown" }
        appRecords.take(if (expandedApps) appRecords.size else 8).forEach { app ->
            val subtitle = "${formatDuration(app.totalForegroundMs)} foreground · ${app.dailyUsage.size} days · ${app.packageName}"
            addRow(binding.llAppUsage, app.appName, subtitle) { showAppDetails(app) }
        }
        binding.btnToggleApps.visibility = if (appRecords.size > 8) View.VISIBLE else View.GONE
        binding.btnToggleApps.text = if (expandedApps) "Show fewer apps" else "Show all ${appRecords.size} apps"
    }

    private fun showAppDetails(app: AppUsageRecord) {
        val details = buildString {
            appendLine("Package: ${app.packageName}")
            appendLine("Total foreground: ${formatDuration(app.totalForegroundMs)}")
            appendLine("Total visible: ${formatDuration(app.totalVisibleMs)}")
            appendLine("Foreground service: ${formatDuration(app.totalForegroundServiceMs)}")
            appendLine("Last used: ${app.lastUsedAt ?: "—"}")
            appendLine("Last visible: ${app.lastVisibleAt ?: "—"}")
            appendLine("Last service use: ${app.lastForegroundServiceUsedAt ?: "—"}")
            appendLine(); appendLine("Daily usage")
            app.dailyUsage.sortedBy { it.localDate }.forEach { day ->
                appendLine("${day.localDate}: foreground ${formatDuration(day.foregroundMs)}, visible ${formatDuration(day.visibleMs)}, service ${formatDuration(day.foregroundServiceMs)}")
                appendLine("  Last used ${day.lastUsedAt ?: "—"} · visible ${day.lastVisibleAt ?: "—"} · service ${day.lastForegroundServiceUsedAt ?: "—"}")
            }
        }
        showJsonDialog(app.appName, details)
    }

    private fun showJsonDialog(title: String, text: String) {
        val density = resources.displayMetrics.density
        val scroll = ScrollView(this).apply { setPadding((18 * density).toInt(), (8 * density).toInt(), (18 * density).toInt(), (8 * density).toInt()) }
        val content = TextView(this).apply {
            this.text = text; textSize = 12f; typeface = Typeface.MONOSPACE
            setTextIsSelectable(true); setTextColor(getColor(R.color.text_primary))
        }
        scroll.addView(content)
        AlertDialog.Builder(this).setTitle(title).setView(scroll)
            .setPositiveButton("Copy") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(title, text))
                Toast.makeText(this, "Details copied", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Close", null).show()
    }

    private fun addRow(parent: LinearLayout, title: String, subtitle: String, onClick: () -> Unit) {
        val density = resources.displayMetrics.density
        val card = MaterialCardView(this).apply {
            radius = 16 * density; cardElevation = 0f; strokeWidth = (1 * density).toInt()
            strokeColor = getColor(R.color.card_stroke); setCardBackgroundColor(getColor(R.color.card_surface))
            isClickable = true; isFocusable = true; setOnClickListener { onClick() }
        }
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding((14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt()) }
        column.addView(TextView(this).apply { text = title; textSize = 14f; setTypeface(typeface, Typeface.BOLD); setTextColor(getColor(R.color.text_primary) ) })
        column.addView(TextView(this).apply { text = subtitle.ifBlank { "Tap to inspect details" }; textSize = 12f; setTextColor(getColor(R.color.text_secondary)); setPadding(0, (3 * density).toInt(), 0, 0) })
        card.addView(column)
        parent.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = (8 * density).toInt() })
    }

    private fun setMetric(tile: ItemMetricBinding, value: String, label: String, detail: String) {
        tile.tvMetricValue.text = value; tile.tvMetricLabel.text = label; tile.tvMetricDetail.text = detail
    }

    private fun updatePermissionDisplay(state: SamsungPermissionManager.PermissionState) {
        binding.tvPermSteps.text = "Steps: ${permissionStatusIcon(state.steps)}"
        binding.tvPermHeartRate.text = "Heart rate: ${permissionStatusIcon(state.heartRate)}"
        binding.tvPermSleep.text = "Sleep: ${permissionStatusIcon(state.sleep)}"
        binding.tvPermExercise.text = "Exercise: ${permissionStatusIcon(state.exercise)}"
        binding.tvPermExerciseLocation.text = "Exercise routes: ${permissionStatusIcon(state.exerciseLocation)}"
        samsungPermissionCount = listOf(state.steps, state.heartRate, state.sleep, state.exercise, state.exerciseLocation).count { it == SamsungPermissionManager.PermissionStatus.GRANTED }
        updatePermissionSummary()
    }

    private fun updatePermissionSummary() {
        val screen = if (screenTimeReader.hasUsageAccess()) "Screen time ready" else "Screen time access needed"
        binding.tvPermissionSummary.text = "$samsungPermissionCount/5 Samsung data permissions · $screen"
    }

    private fun permissionStatusIcon(status: SamsungPermissionManager.PermissionStatus) = when (status) {
        SamsungPermissionManager.PermissionStatus.GRANTED -> "✓ Granted"
        SamsungPermissionManager.PermissionStatus.DENIED -> "✗ Not granted"
        SamsungPermissionManager.PermissionStatus.UNAVAILABLE -> "⚠ Unavailable"
        SamsungPermissionManager.PermissionStatus.UNKNOWN -> "— Unknown"
    }

    private fun updateLastSyncDisplay() {
        val lastSync = syncManager.syncStateStore.getLastSuccessfulSync()
        binding.tvLastSync.text = when (syncManager.syncStateStore.getSyncStatus()) {
            SyncStateStore.SyncStatus.NEVER_SYNCED -> "Last upload: Never"
            SyncStateStore.SyncStatus.IN_PROGRESS -> "Last upload: Syncing…"
            SyncStateStore.SyncStatus.SUCCESS -> "Last upload: ${lastSync ?: "Never"}"
            SyncStateStore.SyncStatus.FAILED -> "Last upload failed: ${syncManager.syncStateStore.getLastError() ?: "unknown error"}"
        }
    }

    private fun updateConnectionStatus(status: String, connected: Boolean) {
        binding.tvConnectionStatus.text = status
        binding.tvConnectionStatus.setTextColor(getColor(if (connected) R.color.accent_teal else R.color.error))
    }

    private fun setSyncButtonsEnabled(enabled: Boolean) {
        binding.btnSyncNow.isEnabled = enabled; binding.btnSyncHistorical.isEnabled = enabled
        binding.btnSyncNow.text = if (enabled) getString(R.string.btn_sync_now) else getString(R.string.sync_in_progress)
        binding.btnSyncHistorical.text = if (enabled) getString(R.string.btn_sync_historical) else getString(R.string.sync_in_progress)
    }

    private fun showError(message: String) { binding.tvError.text = message; binding.tvError.visibility = View.VISIBLE }
    private fun hideError() { binding.tvError.text = ""; binding.tvError.visibility = View.GONE }
    private fun number(value: Long) = NumberFormat.getIntegerInstance().format(value)
    private fun formatDuration(ms: Long): String {
        val mins = ms.coerceAtLeast(0) / 60_000
        val days = mins / (24 * 60); val hours = mins / 60 % 24; val minutes = mins % 60
        return when { days > 0 -> "${days}d ${hours}h"; hours > 0 -> "${hours}h ${minutes}m"; else -> "${minutes}m" }
    }
    private fun List<Double>.averageOrZero() = if (isEmpty()) 0.0 else average()
}

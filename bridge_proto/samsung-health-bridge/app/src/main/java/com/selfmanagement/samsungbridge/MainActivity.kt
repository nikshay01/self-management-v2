package com.selfmanagement.samsungbridge

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.selfmanagement.samsungbridge.databinding.ActivityMainBinding
import com.selfmanagement.samsungbridge.network.ApiClient
import com.selfmanagement.samsungbridge.network.HealthSyncApi
import com.selfmanagement.samsungbridge.samsung.SamsungHealthManager
import com.selfmanagement.samsungbridge.samsung.SamsungPermissionManager
import com.selfmanagement.samsungbridge.sync.SyncManager
import com.selfmanagement.samsungbridge.sync.SyncStateStore
import java.util.Calendar

/**
 * Main activity for the Samsung Health Bridge testing UI.
 *
 * This is a minimal development/testing interface, NOT a production UI.
 * It provides buttons to:
 * - Connect to Samsung Health
 * - Request permissions
 * - Sync today's data
 * - View retrieved data summaries
 * - View sync results
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var healthManager: SamsungHealthManager
    private lateinit var apiClient: ApiClient
    private lateinit var syncApi: HealthSyncApi
    private lateinit var syncManager: SyncManager

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize components
        healthManager = SamsungHealthManager(this)
        apiClient = ApiClient(this)
        syncApi = HealthSyncApi(apiClient)
        syncManager = SyncManager(this, healthManager, syncApi)

        // Set up button listeners
        binding.btnRequestPermissions.setOnClickListener { onRequestPermissions() }
        binding.btnSyncNow.setOnClickListener { onSyncNow() }

        // Check Samsung Health availability
        checkSamsungHealth()

        // Update last sync display
        updateLastSyncDisplay()

        Log.d(TAG, "Samsung Health Bridge initialized")
        Log.d(TAG, "API Base URL: ${ApiClient.BASE_URL}")
    }

    override fun onDestroy() {
        super.onDestroy()
        healthManager.disconnect()
    }

    /**
     * Check Samsung Health availability and attempt connection.
     */
    private fun checkSamsungHealth() {
        if (!healthManager.isAvailable()) {
            updateConnectionStatus("Samsung Health: Not Installed", false)
            showError("Samsung Health app is not installed on this device")
            return
        }

        updateConnectionStatus("Samsung Health: Connecting…", false)

        healthManager.connect { connected ->
            runOnUiThread {
                if (connected) {
                    updateConnectionStatus("Samsung Health: Connected", true)
                    // Automatically check permissions after connecting
                    checkPermissions()
                } else {
                    val error = healthManager.lastError ?: "Unknown error"
                    updateConnectionStatus("Samsung Health: $error", false)
                    showError(error)
                }
            }
        }
    }

    /**
     * Check and display current permission state.
     */
    private fun checkPermissions() {
        val state = healthManager.getPermissions()
        if (state != null) {
            runOnUiThread { updatePermissionDisplay(state) }
        }
    }

    /**
     * Handle "Request Permissions" button click.
     */
    private fun onRequestPermissions() {
        if (healthManager.connectionState != SamsungHealthManager.ConnectionState.CONNECTED) {
            Toast.makeText(this, "Not connected to Samsung Health", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnRequestPermissions.isEnabled = false

        healthManager.requestPermissions(this) { state ->
            runOnUiThread {
                updatePermissionDisplay(state)
                binding.btnRequestPermissions.isEnabled = true

                if (state.allGranted) {
                    Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
                } else if (state.anyDenied) {
                    Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Handle "Sync Now" button click.
     *
     * Reads all data from Samsung Health and optionally uploads to backend.
     */
    private fun onSyncNow() {
        if (healthManager.connectionState != SamsungHealthManager.ConnectionState.CONNECTED) {
            Toast.makeText(this, "Not connected to Samsung Health", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSyncNow.isEnabled = false
        binding.btnSyncNow.text = getString(R.string.sync_in_progress)
        hideError()

        // Get today's time range
        val now = System.currentTimeMillis()
        val startOfDay = getStartOfToday()

        Log.d(TAG, "Starting sync for today")

        // First, just read and display data (Milestone 1 & 2)
        healthManager.readAll(startOfDay, now) { readResult ->
            runOnUiThread {
                // Display the data summary
                binding.tvDataSummary.text = readResult.toDisplaySummary()
                Log.d(TAG, "Data summary:\n${readResult.toDisplaySummary()}")
            }

            // Then attempt to sync to backend (Milestone 3)
            if (apiClient.hasAuthToken()) {
                syncManager.sync(startOfDay, now) { syncResult ->
                    runOnUiThread {
                        binding.tvSyncResult.text = syncResult.toDisplayString()
                        updateLastSyncDisplay()
                        binding.btnSyncNow.isEnabled = true
                        binding.btnSyncNow.text = getString(R.string.btn_sync_now)

                        if (!syncResult.success) {
                            showError(syncResult.error ?: "Sync failed")
                        }
                    }
                }
            } else {
                // No auth token — just show the read results
                runOnUiThread {
                    binding.tvSyncResult.text = buildString {
                        appendLine("Records retrieved: ${readResult.totalRecords}")
                        appendLine("Records uploaded: — (no auth token)")
                        appendLine()
                        appendLine("Set an auth token to enable backend sync.")
                        appendLine("See README for instructions.")
                    }
                    binding.btnSyncNow.isEnabled = true
                    binding.btnSyncNow.text = getString(R.string.btn_sync_now)
                }
            }
        }
    }

    /**
     * Update the connection status display.
     */
    private fun updateConnectionStatus(status: String, connected: Boolean) {
        binding.tvConnectionStatus.text = status
        binding.tvConnectionStatus.setTextColor(
            if (connected) getColor(android.R.color.holo_green_dark)
            else getColor(android.R.color.holo_red_dark)
        )
    }

    /**
     * Update the permissions display.
     */
    private fun updatePermissionDisplay(state: SamsungPermissionManager.PermissionState) {
        binding.tvPermSteps.text = "Steps: ${permissionStatusIcon(state.steps)}"
        binding.tvPermHeartRate.text = "Heart Rate: ${permissionStatusIcon(state.heartRate)}"
        binding.tvPermSleep.text = "Sleep: ${permissionStatusIcon(state.sleep)}"
        binding.tvPermExercise.text = "Exercise: ${permissionStatusIcon(state.exercise)}"
    }

    /**
     * Map permission status to display icon.
     */
    private fun permissionStatusIcon(status: SamsungPermissionManager.PermissionStatus): String {
        return when (status) {
            SamsungPermissionManager.PermissionStatus.GRANTED -> "✓"
            SamsungPermissionManager.PermissionStatus.DENIED -> "✗"
            SamsungPermissionManager.PermissionStatus.UNAVAILABLE -> "⚠ Unavailable"
            SamsungPermissionManager.PermissionStatus.UNKNOWN -> "—"
        }
    }

    /**
     * Update the last sync time display.
     */
    private fun updateLastSyncDisplay() {
        val lastSync = syncManager.syncStateStore.getLastSuccessfulSync()
        val status = syncManager.syncStateStore.getSyncStatus()

        binding.tvLastSync.text = when (status) {
            SyncStateStore.SyncStatus.NEVER_SYNCED -> getString(R.string.never_synced)
            SyncStateStore.SyncStatus.IN_PROGRESS -> getString(R.string.sync_in_progress)
            SyncStateStore.SyncStatus.SUCCESS -> lastSync ?: getString(R.string.never_synced)
            SyncStateStore.SyncStatus.FAILED -> {
                val error = syncManager.syncStateStore.getLastError()
                "Failed: ${error ?: "unknown error"}"
            }
        }
    }

    /**
     * Show an error message.
     */
    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    /**
     * Hide the error display.
     */
    private fun hideError() {
        binding.tvError.text = ""
        binding.tvError.visibility = View.GONE
    }

    /**
     * Get the start of today (midnight) in UTC milliseconds.
     */
    private fun getStartOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

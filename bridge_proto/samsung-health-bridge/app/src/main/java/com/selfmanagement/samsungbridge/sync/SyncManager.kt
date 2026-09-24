package com.selfmanagement.samsungbridge.sync

import android.content.Context
import android.util.Log
import com.selfmanagement.samsungbridge.models.*
import com.selfmanagement.samsungbridge.network.ApiClient
import com.selfmanagement.samsungbridge.network.HealthSyncApi
import com.selfmanagement.samsungbridge.samsung.SamsungDataMapper
import com.selfmanagement.samsungbridge.samsung.SamsungHealthManager
import java.util.Calendar
import java.util.TimeZone

/**
 * Orchestrates the full Samsung Health → Backend synchronization pipeline.
 *
 * Flow:
 * 1. Determine sync time range (incremental from last sync, or full day)
 * 2. Read all health data from Samsung Health via [SamsungHealthManager]
 * 3. Normalize records to [HealthRecord] canonical format
 * 4. Build [HealthSyncPayload] JSON envelope
 * 5. POST to backend API via [HealthSyncApi]
 * 6. Persist sync state via [SyncStateStore]
 *
 * Supports idempotent synchronization through source_record_id deduplication.
 */
class SyncManager(
    private val context: Context,
    private val healthManager: SamsungHealthManager,
    private val syncApi: HealthSyncApi
) {
    companion object {
        private const val TAG = "SyncManager"
    }

    val syncStateStore = SyncStateStore(context)

    /**
     * Perform a full sync for the specified time range.
     *
     * @param startTimeMillis Start of the sync range (UTC milliseconds)
     * @param endTimeMillis End of the sync range (UTC milliseconds)
     * @param callback Called with the sync result
     */
    fun sync(
        startTimeMillis: Long,
        endTimeMillis: Long,
        callback: (SyncResult) -> Unit
    ) {
        val syncStartedAt = SamsungDataMapper.toIso8601(System.currentTimeMillis())
        syncStateStore.onSyncStarted(syncStartedAt)

        Log.d(TAG, "[Sync] Starting sync from ${SamsungDataMapper.toIso8601(startTimeMillis)} " +
                "to ${SamsungDataMapper.toIso8601(endTimeMillis)}")

        // Step 1: Read all data from Samsung Health
        healthManager.sync(startTimeMillis, endTimeMillis) { records ->
            Log.d(TAG, "[Sync] Retrieved ${records.size} normalized records")

            if (records.isEmpty()) {
                val result = SyncResult(
                    success = true,
                    recordsRetrieved = 0,
                    recordsUploaded = 0,
                    syncStartedAt = syncStartedAt
                )
                syncStateStore.onSyncSuccess(
                    SamsungDataMapper.toIso8601(System.currentTimeMillis()),
                    endTimeMillis
                )
                callback(result)
                return@sync
            }

            // Step 2: Build the sync payload
            val payload = HealthSyncPayload(
                source = HealthRecord.SOURCE_SAMSUNG_HEALTH,
                device = records.firstOrNull()?.sourceDevice,
                syncStartedAt = syncStartedAt,
                syncRange = SyncRange(
                    start = SamsungDataMapper.toIso8601(startTimeMillis),
                    end = SamsungDataMapper.toIso8601(endTimeMillis)
                ),
                records = records
            )

            // Step 3: Upload to backend
            Log.d(TAG, "[Sync] Uploading ${records.size} records")
            syncApi.uploadHealthData(payload) { apiResult ->
                val completedAt = SamsungDataMapper.toIso8601(System.currentTimeMillis())

                if (apiResult.success) {
                    Log.d(TAG, "[Sync] Success — uploaded ${records.size} records")
                    syncStateStore.onSyncSuccess(completedAt, endTimeMillis)
                    callback(SyncResult(
                        success = true,
                        recordsRetrieved = records.size,
                        recordsUploaded = apiResult.recordsAccepted,
                        recordsSkipped = apiResult.recordsSkipped,
                        recordsFailed = apiResult.recordsFailed,
                        syncStartedAt = syncStartedAt,
                        syncCompletedAt = completedAt
                    ))
                } else {
                    val error = apiResult.error ?: "Upload failed"
                    Log.e(TAG, "[Sync] Failed — $error")
                    syncStateStore.onSyncFailed(completedAt, error)
                    callback(SyncResult(
                        success = false,
                        recordsRetrieved = records.size,
                        error = error,
                        syncStartedAt = syncStartedAt,
                        syncCompletedAt = completedAt
                    ))
                }
            }
        }
    }

    /**
     * Perform an incremental sync starting from the last successful sync time.
     *
     * If no previous sync exists, syncs the last 24 hours.
     *
     * @param callback Called with the sync result
     */
    fun syncIncremental(callback: (SyncResult) -> Unit) {
        val endTimeMillis = System.currentTimeMillis()
        val startTimeMillis = syncStateStore.getLastSyncEndTimeMillis()
            ?: run {
                // No previous sync — default to last 24 hours
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                cal.add(Calendar.HOUR_OF_DAY, -24)
                cal.timeInMillis
            }

        Log.d(TAG, "Incremental sync from ${SamsungDataMapper.toIso8601(startTimeMillis)}")
        sync(startTimeMillis, endTimeMillis, callback)
    }

    /**
     * Sync today's data (from midnight local time to now).
     *
     * @param callback Called with the sync result
     */
    fun syncToday(callback: (SyncResult) -> Unit) {
        val now = System.currentTimeMillis()
        val startOfDay = getStartOfToday()

        Log.d(TAG, "Syncing today from ${SamsungDataMapper.toIso8601(startOfDay)}")
        sync(startOfDay, now, callback)
    }

    /**
     * Get the start of today (midnight) in UTC milliseconds,
     * using the device's local timezone.
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

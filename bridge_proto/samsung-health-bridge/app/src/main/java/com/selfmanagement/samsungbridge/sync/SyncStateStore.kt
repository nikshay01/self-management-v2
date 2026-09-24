package com.selfmanagement.samsungbridge.sync

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists synchronization state using encrypted SharedPreferences.
 *
 * Stores:
 * - Last successful sync time
 * - Last attempted sync time
 * - Sync status
 * - Last error message
 *
 * Uses Android's EncryptedSharedPreferences for secure storage
 * since sync state may contain timestamps related to health data.
 */
class SyncStateStore(context: Context) {

    companion object {
        private const val TAG = "SyncStateStore"
        private const val PREFS_NAME = "samsung_health_sync_state"

        private const val KEY_LAST_SUCCESSFUL_SYNC = "last_successful_sync"
        private const val KEY_LAST_ATTEMPTED_SYNC = "last_attempted_sync"
        private const val KEY_SYNC_STATUS = "sync_status"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_LAST_SYNC_END_TIME = "last_sync_end_time"
    }

    /**
     * Sync status values.
     */
    enum class SyncStatus {
        NEVER_SYNCED,
        IN_PROGRESS,
        SUCCESS,
        FAILED
    }

    private val prefs = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create encrypted prefs, falling back to regular prefs", e)
        // Fallback to unencrypted prefs if encryption fails
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Get the last successful sync time as ISO-8601 string.
     */
    fun getLastSuccessfulSync(): String? {
        return prefs.getString(KEY_LAST_SUCCESSFUL_SYNC, null)
    }

    /**
     * Get the last attempted sync time as ISO-8601 string.
     */
    fun getLastAttemptedSync(): String? {
        return prefs.getString(KEY_LAST_ATTEMPTED_SYNC, null)
    }

    /**
     * Get the current sync status.
     */
    fun getSyncStatus(): SyncStatus {
        val statusStr = prefs.getString(KEY_SYNC_STATUS, null) ?: return SyncStatus.NEVER_SYNCED
        return try {
            SyncStatus.valueOf(statusStr)
        } catch (e: IllegalArgumentException) {
            SyncStatus.NEVER_SYNCED
        }
    }

    /**
     * Get the last error message from a failed sync.
     */
    fun getLastError(): String? {
        return prefs.getString(KEY_LAST_ERROR, null)
    }

    /**
     * Get the end time of the last successful sync range.
     * Used for incremental sync — next sync starts from here.
     *
     * @return UTC milliseconds of the last sync end time, or null if never synced
     */
    fun getLastSyncEndTimeMillis(): Long? {
        val value = prefs.getLong(KEY_LAST_SYNC_END_TIME, -1L)
        return if (value == -1L) null else value
    }

    /**
     * Record that a sync attempt has started.
     */
    fun onSyncStarted(timestamp: String) {
        prefs.edit()
            .putString(KEY_LAST_ATTEMPTED_SYNC, timestamp)
            .putString(KEY_SYNC_STATUS, SyncStatus.IN_PROGRESS.name)
            .putString(KEY_LAST_ERROR, null)
            .apply()
        Log.d(TAG, "Sync started at $timestamp")
    }

    /**
     * Record that a sync completed successfully.
     *
     * @param timestamp ISO-8601 completion timestamp
     * @param endTimeMillis The end time of the sync range (UTC ms), for incremental sync
     */
    fun onSyncSuccess(timestamp: String, endTimeMillis: Long) {
        prefs.edit()
            .putString(KEY_LAST_SUCCESSFUL_SYNC, timestamp)
            .putString(KEY_SYNC_STATUS, SyncStatus.SUCCESS.name)
            .putLong(KEY_LAST_SYNC_END_TIME, endTimeMillis)
            .putString(KEY_LAST_ERROR, null)
            .apply()
        Log.d(TAG, "Sync succeeded at $timestamp")
    }

    /**
     * Record that a sync failed.
     *
     * @param timestamp ISO-8601 failure timestamp
     * @param error Error message
     */
    fun onSyncFailed(timestamp: String, error: String) {
        prefs.edit()
            .putString(KEY_SYNC_STATUS, SyncStatus.FAILED.name)
            .putString(KEY_LAST_ERROR, error)
            .apply()
        Log.e(TAG, "Sync failed at $timestamp: $error")
    }

    /**
     * Clear all sync state. Useful for testing/reset.
     */
    fun clear() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Sync state cleared")
    }
}

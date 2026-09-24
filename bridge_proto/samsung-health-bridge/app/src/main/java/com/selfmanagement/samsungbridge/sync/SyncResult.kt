package com.selfmanagement.samsungbridge.sync

/**
 * Result of a synchronization operation.
 */
data class SyncResult(
    /** Whether the sync completed successfully */
    val success: Boolean,
    /** Number of records retrieved from Samsung Health */
    val recordsRetrieved: Int = 0,
    /** Number of records successfully uploaded to the backend */
    val recordsUploaded: Int = 0,
    /** Number of records skipped (e.g., duplicates) */
    val recordsSkipped: Int = 0,
    /** Number of records that failed to upload */
    val recordsFailed: Int = 0,
    /** Error message if the sync failed */
    val error: String? = null,
    /** ISO-8601 timestamp when the sync started */
    val syncStartedAt: String? = null,
    /** ISO-8601 timestamp when the sync completed */
    val syncCompletedAt: String? = null
) {
    /**
     * Generate a human-readable summary for the testing UI.
     */
    fun toDisplayString(): String {
        return if (success) {
            buildString {
                appendLine("Records retrieved: $recordsRetrieved")
                appendLine("Records uploaded: $recordsUploaded")
                appendLine("Records skipped: $recordsSkipped")
                appendLine("Records failed: $recordsFailed")
            }.trimEnd()
        } else {
            "Sync failed: ${error ?: "Unknown error"}"
        }
    }
}

package com.selfmanagement.samsungbridge.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.selfmanagement.samsungbridge.models.HealthSyncPayload
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

/**
 * API interface for the Samsung Health sync endpoint.
 *
 * Sends normalized [HealthSyncPayload] to:
 * ```
 * POST {baseUrl}/api/integrations/samsung/health-data
 * ```
 *
 * Authentication is handled by the [ApiClient]'s interceptor.
 */
class HealthSyncApi(private val apiClient: ApiClient) {

    companion object {
        private const val TAG = "HealthSyncApi"
        private const val SYNC_ENDPOINT = "/api/integrations/samsung/health-data"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    /**
     * Result of an API upload operation.
     */
    data class UploadResult(
        val success: Boolean,
        val httpStatusCode: Int = 0,
        val recordsAccepted: Int = 0,
        val recordsSkipped: Int = 0,
        val recordsFailed: Int = 0,
        val error: String? = null,
        val responseBody: String? = null
    )

    /**
     * Upload health data to the backend API.
     *
     * Serializes the [HealthSyncPayload] to JSON and POSTs it to the
     * sync endpoint. The response is parsed to determine how many
     * records were accepted/skipped/failed.
     *
     * @param payload The normalized health data payload
     * @param callback Called with the upload result
     */
    fun uploadHealthData(
        payload: HealthSyncPayload,
        callback: (UploadResult) -> Unit
    ) {
        val url = "${ApiClient.BASE_URL}$SYNC_ENDPOINT"

        // Check auth token
        if (!apiClient.hasAuthToken()) {
            Log.w(TAG, "No auth token configured — upload will likely fail")
        }

        try {
            val jsonBody = gson.toJson(payload)
            Log.d(TAG, "Uploading ${payload.records.size} records to $url")
            // Log payload size, not content (health data is sensitive)
            Log.d(TAG, "Payload size: ${jsonBody.length} bytes")

            val requestBody = jsonBody.toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            apiClient.httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val error = when {
                        e.message?.contains("Unable to resolve host") == true ->
                            "Network unavailable — cannot reach backend"
                        e.message?.contains("timeout") == true ->
                            "Request timed out"
                        e.message?.contains("Connection refused") == true ->
                            "Backend unavailable — connection refused"
                        else ->
                            "Network error: ${e.message}"
                    }
                    Log.e(TAG, "Upload failed: $error", e)
                    callback(UploadResult(success = false, error = error))
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()

                    when (response.code) {
                        200, 201 -> {
                            Log.d(TAG, "Upload successful (${response.code})")
                            val result = parseSuccessResponse(
                                responseBody, payload.records.size, response.code
                            )
                            callback(result)
                        }
                        401 -> {
                            Log.e(TAG, "Authentication failure (401)")
                            callback(UploadResult(
                                success = false,
                                httpStatusCode = 401,
                                error = "Authentication failed — check your API token"
                            ))
                        }
                        403 -> {
                            Log.e(TAG, "Authorization failure (403)")
                            callback(UploadResult(
                                success = false,
                                httpStatusCode = 403,
                                error = "Not authorized to upload health data"
                            ))
                        }
                        409 -> {
                            // Conflict — likely all duplicates, which is acceptable
                            Log.d(TAG, "Conflict (409) — records may be duplicates")
                            callback(UploadResult(
                                success = true,
                                httpStatusCode = 409,
                                recordsAccepted = 0,
                                recordsSkipped = payload.records.size,
                                responseBody = responseBody
                            ))
                        }
                        422 -> {
                            Log.e(TAG, "Validation error (422): $responseBody")
                            callback(UploadResult(
                                success = false,
                                httpStatusCode = 422,
                                error = "Backend rejected the payload: $responseBody"
                            ))
                        }
                        in 500..599 -> {
                            Log.e(TAG, "Server error (${response.code})")
                            callback(UploadResult(
                                success = false,
                                httpStatusCode = response.code,
                                error = "Backend server error (${response.code})"
                            ))
                        }
                        else -> {
                            Log.e(TAG, "Unexpected response (${response.code}): $responseBody")
                            callback(UploadResult(
                                success = false,
                                httpStatusCode = response.code,
                                error = "Unexpected response: ${response.code}",
                                responseBody = responseBody
                            ))
                        }
                    }
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "Failed to build upload request", e)
            callback(UploadResult(
                success = false,
                error = "Failed to prepare upload: ${e.message}"
            ))
        }
    }

    /**
     * Generate the JSON payload as a string (for testing/debugging).
     *
     * @param payload The sync payload
     * @return JSON string
     */
    fun toJson(payload: HealthSyncPayload): String {
        return gson.toJson(payload)
    }

    /**
     * Parse a successful API response to extract record counts.
     */
    private fun parseSuccessResponse(
        responseBody: String?,
        totalRecords: Int,
        statusCode: Int
    ): UploadResult {
        if (responseBody == null) {
            return UploadResult(
                success = true,
                httpStatusCode = statusCode,
                recordsAccepted = totalRecords,
                responseBody = null
            )
        }

        return try {
            // Try to parse response JSON for record counts
            val responseMap = gson.fromJson(responseBody, Map::class.java)
            val accepted = (responseMap["records_accepted"] as? Number)?.toInt() ?: totalRecords
            val skipped = (responseMap["records_skipped"] as? Number)?.toInt() ?: 0
            val failed = (responseMap["records_failed"] as? Number)?.toInt() ?: 0

            UploadResult(
                success = true,
                httpStatusCode = statusCode,
                recordsAccepted = accepted,
                recordsSkipped = skipped,
                recordsFailed = failed,
                responseBody = responseBody
            )
        } catch (e: Exception) {
            // If response isn't parseable, assume all records were accepted
            Log.w(TAG, "Could not parse response body, assuming all accepted")
            UploadResult(
                success = true,
                httpStatusCode = statusCode,
                recordsAccepted = totalRecords,
                responseBody = responseBody
            )
        }
    }
}

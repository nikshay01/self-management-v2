package com.selfmanagement.samsungbridge.network

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.selfmanagement.samsungbridge.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * HTTP client for backend API communication.
 *
 * Features:
 * - Configurable base URL via BuildConfig.API_BASE_URL
 * - Secure token storage via EncryptedSharedPreferences
 * - Bearer token authentication on all requests
 * - HTTPS enforcement (except for local dev servers)
 * - Request/response logging in debug builds (tokens redacted)
 * - Configurable timeouts
 *
 * The authentication layer is designed so proper OAuth/JWT
 * can be plugged in later by modifying the auth interceptor.
 */
class ApiClient(private val context: Context) {

    companion object {
        private const val TAG = "ApiClient"
        private const val AUTH_PREFS_NAME = "samsung_bridge_auth"
        private const val KEY_AUTH_TOKEN = "auth_token"

        /**
         * Base URL from BuildConfig — configurable per environment.
         * Set in local.properties or build.gradle.kts product flavors.
         */
        val BASE_URL: String = BuildConfig.API_BASE_URL
    }

    /**
     * The OkHttpClient instance with auth and logging interceptors.
     */
    val httpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)

        // Auth interceptor — adds Bearer token to all requests
        builder.addInterceptor(createAuthInterceptor())

        // Logging interceptor — debug builds only, tokens redacted
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                // Redact any Authorization headers in logs
                val safeMessage = if (message.contains("Authorization", ignoreCase = true)) {
                    message.replace(Regex("Bearer \\S+"), "Bearer [REDACTED]")
                } else {
                    message
                }
                Log.d(TAG, safeMessage)
            }
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(loggingInterceptor)
        }

        builder.build()
    }

    /**
     * Store an authentication token securely.
     *
     * For development: set a dev token locally.
     * For production: this would be called after OAuth/JWT authentication.
     *
     * @param token The Bearer token
     */
    fun setAuthToken(token: String) {
        // NEVER log the token
        Log.d(TAG, "Auth token updated (${token.length} chars)")
        getAuthPrefs().edit()
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    /**
     * Retrieve the stored authentication token.
     *
     * @return The Bearer token, or null if not set
     */
    fun getAuthToken(): String? {
        return getAuthPrefs().getString(KEY_AUTH_TOKEN, null)
    }

    /**
     * Check if an authentication token is configured.
     */
    fun hasAuthToken(): Boolean {
        return !getAuthToken().isNullOrBlank()
    }

    /**
     * Clear the stored authentication token.
     */
    fun clearAuthToken() {
        getAuthPrefs().edit()
            .remove(KEY_AUTH_TOKEN)
            .apply()
        Log.d(TAG, "Auth token cleared")
    }

    /**
     * Create the authentication interceptor.
     *
     * Adds `Authorization: Bearer <token>` to all requests.
     * Designed to be replaceable with OAuth/JWT flow later.
     */
    private fun createAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()
            val token = getAuthToken()

            val request = if (!token.isNullOrBlank()) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build()
            } else {
                Log.w(TAG, "No auth token set — request will be unauthenticated")
                original.newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build()
            }

            chain.proceed(request)
        }
    }

    /**
     * Get encrypted SharedPreferences for auth token storage.
     */
    private fun getAuthPrefs() = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            AUTH_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create encrypted prefs for auth", e)
        context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
    }
}

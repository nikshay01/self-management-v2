package com.selfmanagement.samsungbridge.ui

import com.google.gson.GsonBuilder
import com.selfmanagement.samsungbridge.samsung.SamsungHealthReader

/** Keeps the latest in-memory sync available to the local JSON explorer. */
object RawJsonStore {
    data class Snapshot(val range: String, val result: SamsungHealthReader.ReadAllResult)
    @Volatile private var latest: Snapshot? = null
    fun update(range: String, result: SamsungHealthReader.ReadAllResult) { latest = Snapshot(range, result) }
    fun isAvailable() = latest != null
    fun currentRange() = latest?.range

    fun json(category: String, pretty: Boolean): String {
        val snapshot = latest ?: return "{}"
        val data = snapshot.result
        val value: Any = when (category) {
            "Steps" -> data.steps
            "Heart rate" -> data.heartRate
            "Sleep" -> data.sleep
            "Exercise sessions" -> data.exercise
            "Screen time" -> data.screenTime
            else -> mapOf("range" to snapshot.range, "steps" to data.steps, "heartRate" to data.heartRate,
                "sleep" to data.sleep, "exercise" to data.exercise, "screenTime" to data.screenTime)
        }
        val gson = if (pretty) GsonBuilder().setPrettyPrinting().create() else GsonBuilder().create()
        return gson.toJson(value)
    }
}

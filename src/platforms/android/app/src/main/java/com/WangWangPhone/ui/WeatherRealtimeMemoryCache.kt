package com.WangWangPhone.ui

import java.util.Locale

private const val WEATHER_MEMORY_CACHE_DEFAULT_MAX_AGE_MS = 2 * 60 * 60 * 1000L

object WeatherRealtimeMemoryCache {
    private data class Snapshot(
        val cityKey: String,
        val payload: String,
        val savedAtMillis: Long
    )

    private val lock = Any()
    private var snapshot: Snapshot? = null

    fun save(city: String, payload: String) {
        val normalizedCity = normalizeCity(city)
        if (normalizedCity.isEmpty() || payload.isBlank()) return
        synchronized(lock) {
            snapshot = Snapshot(
                cityKey = normalizedCity,
                payload = payload,
                savedAtMillis = System.currentTimeMillis()
            )
        }
    }

    fun load(city: String, maxAgeMillis: Long = WEATHER_MEMORY_CACHE_DEFAULT_MAX_AGE_MS): String? {
        val normalizedCity = normalizeCity(city)
        if (normalizedCity.isEmpty()) return null
        synchronized(lock) {
            val cached = snapshot ?: return null
            if (cached.cityKey != normalizedCity) return null
            if (System.currentTimeMillis() - cached.savedAtMillis > maxAgeMillis) return null
            return cached.payload
        }
    }

    private fun normalizeCity(city: String): String {
        return city.trim().lowercase(Locale.ROOT)
    }
}

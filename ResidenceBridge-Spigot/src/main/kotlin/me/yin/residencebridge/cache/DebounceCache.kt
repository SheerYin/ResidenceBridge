package me.yin.residencebridge.cache

object DebounceCache {

    private val cache = hashMapOf<String, Long>()
    private const val debounceInterval = 100L

    fun debounce(playerName: String): Boolean {
        val currentTime = System.currentTimeMillis()

        val timestamp = cache[playerName]
        if (timestamp == null || currentTime > timestamp) {
            cache[playerName] = currentTime + debounceInterval
            return true
        } else {
            return false
        }
    }

    fun remove(playerName: String): Long? {
        return cache.remove(playerName)
    }
}
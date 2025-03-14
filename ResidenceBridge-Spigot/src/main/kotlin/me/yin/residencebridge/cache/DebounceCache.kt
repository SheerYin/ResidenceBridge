package me.yin.residencebridge.cache

object DebounceCache {

    val cache = hashMapOf<String, Long>()
    const val debounceInterval = 1000L

    fun debounce(playerName: String, interval: Long = debounceInterval): Boolean {
        val currentTime = System.currentTimeMillis()

        val timestamp = cache[playerName]
        if (timestamp == null || currentTime > timestamp) {
            cache[playerName] = currentTime + interval
            return true
        } else {
            return false
        }
    }

}
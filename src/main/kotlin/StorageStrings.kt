import java.util.concurrent.ConcurrentHashMap


class StringStorage {
    // Data class to store value and expiration time
    private data class StoredValue(
        val value: String,
        val expirationTime: Long? = null
    ) {
        val isExpired: Boolean
            get() = expirationTime?.let { System.currentTimeMillis() > it } ?: false
    }

    // Thread-safe in-memory storage for key-value pairs with expiration
    private val storage: ConcurrentHashMap<String, StoredValue> = ConcurrentHashMap()

    fun set(key: String, value: String, expirationTimeMs: Long? = null) {
        val expirationTime = expirationTimeMs?.let { System.currentTimeMillis() + it }
        storage[key] = StoredValue(value, expirationTime)
    }

    fun get(key: String): String? {
        val storedValue = storage[key] ?: return null
        return if (storedValue.isExpired) {
            storage.remove(key)
            null
        } else {
            storedValue.value
        }
    }

    fun containsKey(key: String): Boolean {
        val storedValue = storage[key] ?: return false
        return if (storedValue.isExpired) {
            storage.remove(key)
            false
        } else {
            true
        }
    }
}

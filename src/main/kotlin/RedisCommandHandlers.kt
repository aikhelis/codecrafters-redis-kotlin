import java.util.concurrent.ConcurrentHashMap

// Data class to store value and expiration time
data class StoredValue(val value: String, val expirationTime: Long? = null)

// Thread-safe in-memory storage for key-value pairs with expiration
private val storage = ConcurrentHashMap<String, StoredValue>()

fun handleCommand(command: String, arguments: List<String>): String {
    // Simulate command handling
    return when (command.trim().uppercase()) {
        "COMMAND" -> respondOk()
        "PING" -> handlePing()
        "ECHO" -> handleEcho(arguments)
        "SET" -> handleSet(arguments)
        "GET" -> handleGet(arguments)
        else -> "-ERR unknown command '$command'\r\n"
    }
}

fun respondOk(): String {
    // Simulate a successful command response
    return "+OK\r\n"
}

fun handlePing(): String {
    // Simulate handling a PING command
    return "+PONG\r\n"
}

fun handleEcho(arguments: List<String>): String {
    // Simulate handling an ECHO command
    return if (arguments.isNotEmpty()) {
        "+${arguments.joinToString(" ")}\r\n"
    } else {
        "-ERR wrong number of arguments for 'echo' command\r\n"
    }
}

fun handleSet(arguments: List<String>): String {
    return when {
        arguments.size < 2 -> "-ERR wrong number of arguments for 'SET' command\r\n"
        arguments.size == 2 -> {
            // Simple SET key value
            val key = arguments[0]
            val value = arguments[1]
            storage[key] = StoredValue(value)
            "+OK\r\n"
        }
        arguments.size == 4 && arguments[2].uppercase() == "PX" -> {
            // SET key value PX milliseconds
            val key = arguments[0]
            val value = arguments[1]
            val milliseconds = arguments[3].toLongOrNull()

            if (milliseconds == null || milliseconds <= 0) {
                "-ERR invalid expire time in 'SET' command\r\n"
            } else {
                val expirationTime = System.currentTimeMillis() + milliseconds
                storage[key] = StoredValue(value, expirationTime)
                "+OK\r\n"
            }
        }
        else -> "-ERR syntax error in 'SET' command\r\n"
    }
}

fun handleGet(arguments: List<String>): String {
    return if (arguments.size == 1) {
        val key = arguments[0]
        val storedValue = storage[key]

        when {
            storedValue == null -> "$-1\r\n" // Key doesn't exist
            isExpired(storedValue) -> {
                // Remove expired key and return null
                storage.remove(key)
                "$-1\r\n"
            }
            else -> "+${storedValue.value}\r\n"
        }
    } else {
        "-ERR wrong number of arguments for 'GET' command\r\n"
    }
}

// Helper function to check if a stored value has expired
private fun isExpired(storedValue: StoredValue): Boolean {
    val expirationTime = storedValue.expirationTime ?: return false
    return System.currentTimeMillis() > expirationTime
}

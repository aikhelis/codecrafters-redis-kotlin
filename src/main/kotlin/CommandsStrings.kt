private val stringStorage = StringStorage()

// Helper function to check if a key is a string value key
fun isStringKey(key: String): Boolean {
    return stringStorage.containsKey(key)
}

fun set(arguments: List<String>): String {
    return when {
        arguments.size < 2 -> respError("ERR wrong number of arguments for 'SET' command")
        arguments.size == 2 -> {
            stringStorage.set(arguments[0], arguments[1])
            respString("OK")
        }
        arguments.size == 4 && arguments[2].uppercase() == "PX" -> {
            val milliseconds = arguments[3].toLongOrNull()
            if (milliseconds == null || milliseconds <= 0) {
                respError("ERR invalid expire time in 'SET' command")
            } else {
                stringStorage.set(arguments[0], arguments[1], milliseconds)
                respString("OK")
            }
        }
        else -> respError("ERR syntax error in 'SET' command")
    }
}

fun get(arguments: List<String>): String {
    return if (arguments.size == 1) {
        val value = stringStorage.get(arguments[0])
        if (value != null) respBulkString(value) else respNull()
    } else {
        respError("ERR wrong number of arguments for 'GET' command")
    }
}


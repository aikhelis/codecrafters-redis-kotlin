private val listStorage = ListStorage()

// Helper function to check if a key is a list key
fun isListKey(key: String): Boolean {
    return listStorage.containsKey(key)
}

// LIST command handlers
fun rPush(arguments: List<String>): String {
    return if (arguments.size >= 2) {
        val key = arguments[0]
        val elements = arguments.drop(1)
        val length = listStorage.rpush(key, elements)
        respInteger(length)
    } else {
        respError("ERR wrong number of arguments for 'RPUSH' command")
    }
}

fun lPush(arguments: List<String>): String {
    return if (arguments.size >= 2) {
        val key = arguments[0]
        val elements = arguments.drop(1)
        val length = listStorage.lpush(key, elements)
        respInteger(length)
    } else {
        respError("ERR wrong number of arguments for 'LPUSH' command")
    }
}

fun lRange(arguments: List<String>): String {
    return if (arguments.size == 3) {
        val key = arguments[0]
        val start = arguments[1].toIntOrNull()
        val stop = arguments[2].toIntOrNull()

        if (start == null || stop == null) {
            respError("ERR value is not an integer or out of range")
        } else {
            val elements = listStorage.lrange(key, start, stop)
            val respElements = elements.map { respBulkString(it) }
            respArray(respElements)
        }
    } else {
        respError("ERR wrong number of arguments for 'LRANGE' command")
    }
}

fun lLen(arguments: List<String>): String {
    return if (arguments.size == 1) {
        val key = arguments[0]
        val length = listStorage.llen(key)
        respInteger(length)
    } else {
        respError("ERR wrong number of arguments for 'LLEN' command")
    }
}

fun lPop(arguments: List<String>): String {
    return when (arguments.size) {
        1 -> {
            val key = arguments[0]
            val elements = listStorage.lpop(key, 1)
            if (elements.isEmpty()) {
                respNull()
            } else {
                respBulkString(elements[0])
            }
        }
        2 -> {
            val key = arguments[0]
            val count = arguments[1].toIntOrNull()
            if (count == null || count <= 0) {
                respError("ERR value is not an integer or out of range")
            } else {
                val elements = listStorage.lpop(key, count)
                val respElements = elements.map { respBulkString(it) }
                respArray(respElements)
            }
        }
        else -> respError("ERR wrong number of arguments for 'LPOP' command")
    }
}

fun blPop(arguments: List<String>): String {
    return if (arguments.size == 2) {
        val key = arguments[0]
        val timeout = arguments[1].toDoubleOrNull()

        if (timeout == null || timeout < 0) {
            respError("ERR timeout is not a float or out of range")
        } else {
            val result = listStorage.blpop(key, timeout)
            if (result != null) {
                val respElements = listOf(respBulkString(result.first), respBulkString(result.second))
                respArray(respElements)
            } else {
                respNull()
            }
        }
    } else {
        respError("ERR wrong number of arguments for 'BLPOP' command")
    }
}

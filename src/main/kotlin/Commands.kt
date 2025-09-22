// Handles a Redis command and returns the appropriate response.
fun handleCommand(command: String, arguments: List<String>): String {
    // Simulate command handling
    return when (command.trim().uppercase()) {
        // General commands
        "COMMAND"   -> respondOk()
        "PING"      -> ping()
        "ECHO"      -> echo(arguments)
        "TYPE"      -> type(arguments)
        // String storage commands
        "SET"       -> set(arguments)
        "GET"       -> get(arguments)
        // Stream operations
        "XADD"      -> xAdd(arguments)
        "XRANGE"    -> xRange(arguments)
        "XREAD"     -> xRead(arguments)
        // List operations
        "RPUSH"     -> rPush(arguments)
        "LPUSH"     -> lPush(arguments)
        "LRANGE"    -> lRange(arguments)
        "LLEN"      -> lLen(arguments)
        "LPOP"      -> lPop(arguments)
        "BLPOP"     -> blPop(arguments)
        // unsupported commands
        else        -> respError("ERR unknown command '$command'")
    }
}

// General commands handlers
fun respondOk(): String {
    return respString("OK")
}

fun ping(): String {
    return respString("PONG")
}

fun echo(arguments: List<String>): String {
    return if (arguments.isNotEmpty()) {
        respBulkString(arguments.joinToString(" "))
    } else {
        respError("ERR wrong number of arguments for 'echo' command")
    }
}

fun type(arguments: List<String>): String {
    return if (arguments.size == 1) {
        val key = arguments[0]
        when {
            isStringKey(key) -> respString("string")
            isStreamKey(key) -> respString("stream")
            isListKey(key) -> respString("list")
            else -> respString("none")
        }
    } else {
        respError("ERR wrong number of arguments for 'TYPE' command")
    }
}

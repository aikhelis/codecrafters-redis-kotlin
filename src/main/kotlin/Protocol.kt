fun parseRequest(message: String): Pair<String, List<String>> {
    val trimmed = message.trimEnd('\r', '\n')

    val tokens = if (trimmed.startsWith("*")) {
        parseRedisCommand(trimmed)
    } else {
        trimmed.split("\\s+".toRegex())
    }

    val command = tokens.firstOrNull()?.uppercase() ?: ""
    val arguments = tokens.drop(1)

    return command to arguments
}

fun parseRedisCommand(message: String): List<String> {
    val lines = message.split("\r\n")
    val paramCount = lines.firstOrNull()?.substringAfter("*")?.toIntOrNull() ?: 0

    val result = mutableListOf<String>()
    var lineIndex = 1

    repeat(paramCount) {
        // Skip length line (e.g., "$4")
        if (lineIndex < lines.size && lines[lineIndex].startsWith("$")) {
            lineIndex++
            // Get the actual value
            if (lineIndex < lines.size) {
                result.add(lines[lineIndex])
                lineIndex++
            }
        }
    }

    return result
}

// Individual RESP response functions
fun respString(value: String): String {
    return "+$value\r\n"
}

fun respError(message: String): String {
    return "-$message\r\n"
}

fun respBulkString(value: String): String {
    val length = value.length
    return if (length == 0) {
        "$0\r\n\r\n" // Empty bulk string
    } else {
        "$$length\r\n$value\r\n"
    }
}

fun respNull(): String {
    return "$-1\r\n"
}

fun respInteger(value: Int): String {
    return ":$value\r\n"
}

// Function to encode RESP arrays
fun respArray(elements: List<String>): String {
    val result = StringBuilder()
    result.append("*${elements.size}\r\n")
    for (element in elements) {
        result.append(element)
    }
    return result.toString()
}

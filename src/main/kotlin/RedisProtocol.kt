//package kot // Replace with your actual package name

fun parseRequest(message: String): Pair<String, List<String>> {
    val trimmed = message.trimEnd('\r', '\n')

    val tokens = if (trimmed.startsWith("*")) {
        parseRedisProtocol(trimmed)
    } else {
        trimmed.split("\\s+".toRegex())
    }

    val command = tokens.firstOrNull()?.uppercase() ?: ""
    val arguments = tokens.drop(1)

    return command to arguments
}

fun parseRedisProtocol(message: String): List<String> {
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
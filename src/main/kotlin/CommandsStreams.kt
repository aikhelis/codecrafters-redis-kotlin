private val streamStorage = StreamStorage()

// Helper function to check if a key is a stream
fun isStreamKey(key: String): Boolean {
    return streamStorage.containsKey(key)
}

fun xAdd(arguments: List<String>): String {
    // XADD requires at least 4 arguments: key, id, and at least one field-value pair
    // example: XADD stream_key 1526919030474-0 temperature 36 humidity 95
    if (arguments.size < 4 || arguments.size % 2 != 0) {
        return respError("ERR wrong number of arguments for 'XADD' command")
    }

    val key = arguments[0]
    var id = arguments[1]

    // Handle auto-generation of IDs
    if (id == "*") {
        // Generate both time and sequence number using current timestamp
        id = generateFullId(key)
    } else if (id.endsWith("-*")) {
        // Generate only sequence number for given time
        val generatedId = generateSequenceNumber(key, id)
        if (generatedId == null) {
            return respError("ERR Invalid stream ID specified as stream command argument")
        }
        id = generatedId
    }

    // Validate the entry ID
    val validationError = validateEntryId(key, id)
    if (validationError != null) {
        return respError(validationError)
    }

    // Parse field-value pairs
    val fields = mutableMapOf<String, String>()
    for (i in 2 until arguments.size step 2) {
        if (i + 1 < arguments.size) {
            fields[arguments[i]] = arguments[i + 1]
        }
    }

    // Add a new stream entry
    streamStorage.addEntry(key, id, fields)

    // Return the ID as a bulk string
    return respBulkString(id)
}

fun generateSequenceNumber(streamKey: String, idWithStar: String): String? {
    // Parse the time part from "time-*" format
    val parts = idWithStar.split("-")
    if (parts.size != 2 || parts[1] != "*") {
        return null
    }
    
    val timeString = parts[0]
    val millisecondsTime = timeString.toLongOrNull() ?: return null
    
    // Get all entries from the stream
    val entries = streamStorage.getEntries(streamKey)
    
    // Find the highest sequence number for the given time part
    var maxSequenceForTime = -1L
    var hasEntriesForTime = false
    
    for (entry in entries) {
        val entryParts = entry.id.split("-")
        if (entryParts.size == 2) {
            val entryTime = entryParts[0].toLongOrNull()
            val entrySequence = entryParts[1].toLongOrNull()
            
            if (entryTime == millisecondsTime && entrySequence != null) {
                hasEntriesForTime = true
                maxSequenceForTime = maxOf(maxSequenceForTime, entrySequence)
            }
        }
    }
    
    // Generate the sequence number based on Redis rules
    val sequenceNumber = if (hasEntriesForTime) {
        // Increment the highest sequence number found
        maxSequenceForTime + 1
    } else {
        // Default sequence number: 0 for most cases, 1 when time is 0
        if (millisecondsTime == 0L) 1L else 0L
    }
    
    return "$millisecondsTime-$sequenceNumber"
}

fun generateFullId(streamKey: String): String {
    // Generate current unix time in milliseconds
    val currentTimeMillis = System.currentTimeMillis()
    println("[DEBUG_LOG] generateFullId: currentTimeMillis = $currentTimeMillis")
    
    // Get all entries from the stream
    val entries = streamStorage.getEntries(streamKey)
    println("[DEBUG_LOG] generateFullId: entries.size = ${entries.size}")
    
    // Find the highest sequence number for the current time
    var maxSequenceForTime = -1L
    var hasEntriesForTime = false
    
    for (entry in entries) {
        val entryParts = entry.id.split("-")
        if (entryParts.size == 2) {
            val entryTime = entryParts[0].toLongOrNull()
            val entrySequence = entryParts[1].toLongOrNull()
            
            if (entryTime == currentTimeMillis && entrySequence != null) {
                hasEntriesForTime = true
                maxSequenceForTime = maxOf(maxSequenceForTime, entrySequence)
                println("[DEBUG_LOG] generateFullId: found matching time, maxSequence = $maxSequenceForTime")
            }
        }
    }
    
    // Generate the sequence number based on Redis rules
    val sequenceNumber = if (hasEntriesForTime) {
        // Increment the highest sequence number found for this time
        maxSequenceForTime + 1
    } else {
        // Default sequence number is 0
        0L
    }
    
    val result = "$currentTimeMillis-$sequenceNumber"
    println("[DEBUG_LOG] generateFullId: result = $result")
    return result
}

fun validateEntryId(streamKey: String, id: String): String? {
    // Parse the ID into millisecondsTime and sequenceNumber
    val parts = id.split("-")
    if (parts.size != 2) {
        return "ERR Invalid stream ID specified as stream command argument"
    }
    
    val millisecondsTime = parts[0].toLongOrNull()
    val sequenceNumber = parts[1].toLongOrNull()
    
    if (millisecondsTime == null || sequenceNumber == null) {
        return "ERR Invalid stream ID specified as stream command argument"
    }
    
    // Check if ID is greater than 0-0
    if (millisecondsTime == 0L && sequenceNumber == 0L) {
        return "ERR The ID specified in XADD must be greater than 0-0"
    }
    
    // Get the last entry from the stream
    val entries = streamStorage.getEntries(streamKey)
    if (entries.isEmpty()) {
        // Stream is empty, ID just needs to be greater than 0-0 (already checked above)
        return null
    }
    
    val lastEntry = entries.last()
    val lastIdParts = lastEntry.id.split("-")
    val lastMillisecondsTime = lastIdParts[0].toLong()
    val lastSequenceNumber = lastIdParts[1].toLong()
    
    // Check if the new ID is greater than the last entry's ID
    return when {
        millisecondsTime > lastMillisecondsTime -> null // Valid
        millisecondsTime < lastMillisecondsTime -> "ERR The ID specified in XADD is equal or smaller than the target stream top item"
        else -> if (sequenceNumber > lastSequenceNumber) {
            null // Valid
        } else {
            "ERR The ID specified in XADD is equal or smaller than the target stream top item"
        }
    }
}

fun xRange(arguments: List<String>): String {
    // XRANGE requires exactly 3 arguments: key, start, end
    if (arguments.size != 3) {
        return respError("ERR wrong number of arguments for 'XRANGE' command")
    }

    val key = arguments[0]
    val startId = arguments[1]
    val endId = arguments[2]

    // Get all entries from the stream
    val entries = streamStorage.getEntries(key)
    
    // Filter entries within the range
    val filteredEntries = entries.filter { entry ->
        isIdInRange(entry.id, startId, endId)
    }

    // Format response as RESP array
    val responseElements = mutableListOf<String>()
    for (entry in filteredEntries) {
        // Each entry is an array with ID and field-value pairs
        val entryElements = mutableListOf<String>()
        
        // Add the entry ID as a bulk string
        entryElements.add(respBulkString(entry.id))

        // Add field-value pairs as an array
        val fieldValueElements = mutableListOf<String>()
        for ((field, value) in entry.fields) {
            fieldValueElements.add(respBulkString(field))
            fieldValueElements.add(respBulkString(value))
        }
        entryElements.add(respArray(fieldValueElements))
        
        responseElements.add(respArray(entryElements))
    }

    return respArray(responseElements)
}

// Helper function to check if an ID is within the specified range
fun isIdInRange(id: String, startId: String, endId: String): Boolean {
    val idParts = parseId(id)
    
    // Handle special case for "-" (start from beginning)
    val startCheck = if (startId == "-") {
        true // Always include if start is "-"
    } else {
        val startParts = parseId(startId)
        compareIds(idParts, startParts) >= 0
    }
    
    // Handle special case for "+" (end at the end of stream)
    val endCheck = if (endId == "+") {
        true // Always include if end is "+"
    } else {
        val endParts = parseId(endId)
        compareIds(idParts, endParts) <= 0
    }
    
    return startCheck && endCheck
}

// Helper function to parse an ID into time and sequence parts
fun parseId(id: String): Pair<Long, Long> {
    // Handle special cases
    if (id == "-") return Long.MIN_VALUE to Long.MIN_VALUE
    if (id == "+") return Long.MAX_VALUE to Long.MAX_VALUE
    
    val parts = id.split("-")
    val time = parts[0].toLongOrNull() ?: 0L
    val sequence = if (parts.size > 1) parts[1].toLongOrNull() ?: 0L else 0L
    return time to sequence
}

// Helper function to compare two IDs
// Returns: negative if id1 < id2, zero if id1 == id2, positive if id1 > id2
fun compareIds(id1: Pair<Long, Long>, id2: Pair<Long, Long>): Int {
    val timeComparison = id1.first.compareTo(id2.first)
    return if (timeComparison != 0) {
        timeComparison
    } else {
        id1.second.compareTo(id2.second)
    }
}

fun xRead(arguments: List<String>): String {
    // XREAD requires at least 3 arguments and an odd number of arguments
    // Format: XREAD streams key1 key2 ... keyN id1 id2 ... idN
    // Example: XREAD streams stream_key 0-0 (3 args for 1 stream)
    // Example: XREAD streams stream_key other_stream_key 0-0 0-1 (5 args for 2 streams)
    if (arguments.size < 3 || arguments.size % 2 == 0) {
        return respError("ERR wrong number of arguments for 'XREAD' command")
    }

    // First argument must be "streams"
    if (arguments[0].lowercase() != "streams") {
        return respError("ERR syntax error")
    }

    // Calculate number of streams
    val numStreams = (arguments.size - 1) / 2
    
    // Extract stream keys and IDs
    val streamKeys = arguments.subList(1, 1 + numStreams)
    val streamIds = arguments.subList(1 + numStreams, arguments.size)

    // Build response for all streams
    val allStreamResults = mutableListOf<String>()
    
    for (i in 0 until numStreams) {
        val key = streamKeys[i]
        val afterId = streamIds[i]

        // Get all entries from the stream
        val entries = streamStorage.getEntries(key)
        
        // Filter entries with IDs greater than the provided ID (exclusive)
        val afterIdParts = parseId(afterId)
        val filteredEntries = entries.filter { entry ->
            val entryIdParts = parseId(entry.id)
            compareIds(entryIdParts, afterIdParts) > 0
        }

        // Only include streams that have matching entries
        if (filteredEntries.isNotEmpty()) {
            // Build entries array for this stream
            val entriesArray = mutableListOf<String>()
            for (entry in filteredEntries) {
                // Each entry is an array with ID and field-value pairs
                val entryElements = mutableListOf<String>()
                
                // Add the entry ID as a bulk string
                entryElements.add(respBulkString(entry.id))

                // Add field-value pairs as an array
                val fieldValueElements = mutableListOf<String>()
                for ((field, value) in entry.fields) {
                    fieldValueElements.add(respBulkString(field))
                    fieldValueElements.add(respBulkString(value))
                }
                entryElements.add(respArray(fieldValueElements))
                
                entriesArray.add(respArray(entryElements))
            }

            // Build stream element: [stream_key, [entries...]]
            val streamElements = mutableListOf<String>()
            streamElements.add(respBulkString(key))
            streamElements.add(respArray(entriesArray))
            
            allStreamResults.add(respArray(streamElements))
        }
    }

    // Return array of all stream results
    return respArray(allStreamResults)
}

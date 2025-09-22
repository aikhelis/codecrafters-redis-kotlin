import java.util.concurrent.ConcurrentHashMap


class StreamStorage {
    // Data class to represent a stream entry
    data class StreamEntry(val id: String, val fields: Map<String, String>)

    // Data class to represent a stream
    private data class Stream(val entries: MutableList<StreamEntry> = mutableListOf())

    // Thread-safe in-memory storage for streams
    private val storage: ConcurrentHashMap<String, Stream> = ConcurrentHashMap()

    // Add an entry to a stream with a specific key
    fun addEntry(streamKey: String, id: String, fields: Map<String, String>) {
        val stream = storage.getOrPut(streamKey) { Stream() }
        stream.entries.add(StreamEntry(id, fields))
    }

    fun getEntries(streamKey: String): List<StreamEntry> {
        // Return the entries for the specified stream key, or an empty list if the stream does not exist
        return storage[streamKey]?.entries ?: emptyList()
    }

    fun getKeys(): Set<String> {
        return storage.keys
    }

    fun containsKey(streamKey: String): Boolean {
        // Check if the stream exists in the storage
        return storage.containsKey(streamKey)
    }
}

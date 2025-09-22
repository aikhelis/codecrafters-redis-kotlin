import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit


class ListStorage {
    // Data class to represent a list with blocking clients
    private data class RedisList(
        val elements: MutableList<String> = mutableListOf(),
        val blockingClients: MutableList<BlockingClient> = mutableListOf()
    )

    // Data class to represent a blocking client
    private data class BlockingClient(
        val responseQueue: BlockingQueue<Pair<String, String>>,
        val listKey: String,
        val timeoutMs: Long
    )

    // Thread-safe in-memory storage for lists
    private val storage: ConcurrentHashMap<String, RedisList> = ConcurrentHashMap()

    // RPUSH - append elements to the right of the list
    fun rpush(key: String, elements: List<String>): Int {
        val list = this.storage.getOrPut(key) { RedisList() }
        synchronized(list) {
            list.elements.addAll(elements)
            val newSize = list.elements.size
            this.notifyBlockingClients(list, key)
            return newSize
        }
    }

    // LPUSH - prepend elements to the left of the list
    fun lpush(key: String, elements: List<String>): Int {
        val list = this.storage.getOrPut(key) { RedisList() }
        synchronized(list) {
            list.elements.addAll(0, elements.reversed())
            this.notifyBlockingClients(list, key)
            return list.elements.size
        }
    }

    // LRANGE - get elements in a range
    fun lrange(key: String, start: Int, stop: Int): List<String> {
        val list = this.storage[key] ?: return emptyList()
        synchronized(list) {
            val size = list.elements.size
            if (size == 0) return emptyList()
            
            // Convert negative indices
            val startIndex = if (start < 0) maxOf(0, size + start) else minOf(start, size)
            val stopIndex = if (stop < 0) maxOf(-1, size + stop) else minOf(stop, size - 1)
            
            if (startIndex > stopIndex || startIndex >= size) return emptyList()
            
            return list.elements.subList(startIndex, stopIndex + 1).toList()
        }
    }

    // LLEN - get list length
    fun llen(key: String): Int {
        val list = this.storage[key] ?: return 0
        synchronized(list) {
            return list.elements.size
        }
    }

    // LPOP - remove and return elements from the left
    fun lpop(key: String, count: Int = 1): List<String> {
        val list = this.storage[key] ?: return emptyList()
        synchronized(list) {
            val result = mutableListOf<String>()
            val actualCount = minOf(count, list.elements.size)
            repeat(actualCount) {
                if (list.elements.isNotEmpty()) {
                    result.add(list.elements.removeAt(0))
                }
            }
            if (list.elements.isEmpty() && list.blockingClients.isEmpty()) {
                this.storage.remove(key)
            }
            return result
        }
    }

    // BLPOP - blocking left pop - returns the element or null if timeout
    fun blpop(key: String, timeoutSeconds: Double): Pair<String, String>? {
        val responseQueue = LinkedBlockingQueue<Pair<String, String>>()
        val redisList = this.storage.getOrPut(key) { RedisList() }
        
        synchronized(redisList) {
            // If list has elements, pop immediately
            if (redisList.elements.isNotEmpty()) {
                val element = redisList.elements.removeAt(0)
                if (redisList.elements.isEmpty() && redisList.blockingClients.isEmpty()) {
                    this.storage.remove(key)
                }
                return Pair(key, element)
            }
            
            // Otherwise, block - add this client's responseQueue to the blocking list
            val timeoutMs = if (timeoutSeconds == 0.0) Long.MAX_VALUE else (timeoutSeconds * 1000).toLong()
            val blockingClient = BlockingClient(responseQueue, key, timeoutMs)
            redisList.blockingClients.add(blockingClient)
        }
        
        // Wait for response outside the synchronized block using this client's own responseQueue
        return if (timeoutSeconds == 0.0) {
            responseQueue.take() // Block indefinitely
        } else {
            responseQueue.poll((timeoutSeconds * 1000).toLong(), TimeUnit.MILLISECONDS)
        }
    }

    // Helper method to notify blocking clients when elements are added
    private fun notifyBlockingClients(list: RedisList, key: String) {
        while (list.elements.isNotEmpty() && list.blockingClients.isNotEmpty()) {
            val client = list.blockingClients.removeAt(0) // FIFO - first blocked client gets the element
            val element = list.elements.removeAt(0)
            client.responseQueue.offer(Pair(key, element))
        }
    }

    fun containsKey(key: String): Boolean {
        return this.storage.containsKey(key)
    }
}
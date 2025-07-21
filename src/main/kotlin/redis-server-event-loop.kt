import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap

class RedisServer {
    private val selector = Selector.open()
    private val clientBuffers = ConcurrentHashMap<SocketChannel, ByteBuffer>()

    fun start(port: Int) {
        val serverChannel = ServerSocketChannel.open()
        serverChannel.configureBlocking(false)
        serverChannel.bind(InetSocketAddress(port))
        serverChannel.register(selector, SelectionKey.OP_ACCEPT)

        println("Redis server started on port $port with event loop")

        // Event loop
        while (true) {
            selector.select() // Block until events are available

            val selectedKeys = selector.selectedKeys().iterator()
            while (selectedKeys.hasNext()) {
                val key = selectedKeys.next()
                selectedKeys.remove()

                when {
                    key.isAcceptable -> handleAccept(key)
                    key.isReadable -> handleRead(key)
                    key.isWritable -> handleWrite(key)
                }
            }
        }
    }

    private fun handleAccept(key: SelectionKey) {
        val serverChannel = key.channel() as ServerSocketChannel
        val clientChannel = serverChannel.accept()

        if (clientChannel != null) {
            clientChannel.configureBlocking(false)
            clientChannel.register(selector, SelectionKey.OP_READ)
            clientBuffers[clientChannel] = ByteBuffer.allocate(1024)
            println("Accepted new connection from ${clientChannel.remoteAddress}")
        }
    }

    private fun handleRead(key: SelectionKey) {
        val clientChannel = key.channel() as SocketChannel
        val buffer = clientBuffers[clientChannel] ?: return

        try {
            buffer.clear()
            val bytesRead = clientChannel.read(buffer)

            if (bytesRead == -1) {
                // Client disconnected
                println("Client ${clientChannel.remoteAddress} disconnected")
                closeClient(clientChannel, key)
                return
            }

            if (bytesRead > 0) {
                buffer.flip()
                val receivedData = ByteArray(buffer.remaining())
                buffer.get(receivedData)
                println("Received from ${clientChannel.remoteAddress}: ${String(receivedData)}")

                // Prepare response
                val response = "+PONG\r\n"
                val responseBuffer = ByteBuffer.wrap(response.toByteArray())
                key.attach(responseBuffer)

                // Switch to write mode
                key.interestOps(SelectionKey.OP_WRITE)
            }
        } catch (e: Exception) {
            println("Error reading from client ${clientChannel.remoteAddress}: ${e.message}")
            closeClient(clientChannel, key)
        }
    }

    private fun handleWrite(key: SelectionKey) {
        val clientChannel = key.channel() as SocketChannel
        val responseBuffer = key.attachment() as? ByteBuffer ?: return

        try {
            val bytesWritten = clientChannel.write(responseBuffer)
            println("Sent ${bytesWritten} bytes to ${clientChannel.remoteAddress}")

            if (!responseBuffer.hasRemaining()) {
                // Response fully sent, switch back to read mode
                key.interestOps(SelectionKey.OP_READ)
                key.attach(null) // Clear the attachment
            }
        } catch (e: Exception) {
            println("Error writing to client ${clientChannel.remoteAddress}: ${e.message}")
            closeClient(clientChannel, key)
        }
    }

    private fun closeClient(clientChannel: SocketChannel, key: SelectionKey) {
        try {
            clientBuffers.remove(clientChannel)
            key.cancel()
            clientChannel.close()
            println("Connection with ${clientChannel.remoteAddress} closed")
        } catch (e: Exception) {
            println("Error closing client connection: ${e.message}")
        }
    }
}

fun main(args: Array<String>) {
    System.err.println("Logs from your program will appear here!")
    val server = RedisServer()
    server.start(6379)
}

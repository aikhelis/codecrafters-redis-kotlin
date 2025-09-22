import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
/**
 * A simple Redis-like server implementation in Kotlin.
 * This server listens for incoming connections on port 6379 and responds to PING commands with PONG.
 */

fun handleClient(socket: Socket) {
    val clientAddress = socket.getPort() // socket.remoteSocketAddress
    println("=Accepted new connection from $clientAddress")
    try {
        val input = socket.getInputStream()
        val output = socket.getOutputStream()

        // Respond to multiple commands from the same client connection
        while (true) {
            val buffer = ByteArray(1024)
            val bytesRead = input.read(buffer)
            if (bytesRead == -1) {
                println("Client $clientAddress disconnected")
                break // End of stream
            }
            val request = String(buffer, 0, bytesRead)
            print("Received from $clientAddress: \n$request")

            val (command, arguments) = parseRequest(request)
            val response = handleCommand(command, arguments)
            print("Responding to $clientAddress: \n$response")
            output.write(response.toByteArray())
        }
    } catch (e: Exception) {
        println("Error handling client $clientAddress: ${e.message}")
    } finally {
        socket.close()
        println("=Connection with $clientAddress closed")
        println()
    }
}

fun main() {
    System.err.println("Logs from your program will appear here!")
    val serverSocket = ServerSocket(6379)
    serverSocket.reuseAddress = true

    println("Redis server started on port 6379")
    println("")

    // Handle concurrent clients
    while (true) {
        try {
            val clientSocket = serverSocket.accept() // Wait for connection from a client
            // Handle each client in a separate thread
            thread {
                handleClient(clientSocket)
            }
        } catch (e: Exception) {
            println("Error accepting client connection: ${e.message}")
        }
    }
}

package com.freekof.tvbrowser

import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class LocalHttpProxyServer(
    private val port: Int,
    private val upstream: HttpProxySettings,
) {
    private val running = AtomicBoolean(false)
    private val acceptExecutor = Executors.newSingleThreadExecutor()
    private val tunnelExecutor = Executors.newCachedThreadPool()
    private var serverSocket: ServerSocket? = null

    fun start(): Boolean {
        if (!upstream.isUsable()) return false
        if (!running.compareAndSet(false, true)) return true
        return runCatching {
            serverSocket = ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"))
            acceptExecutor.execute { acceptLoop() }
            true
        }.getOrElse {
            running.set(false)
            Log.e(TAG, "Failed to start local proxy", it)
            false
        }
    }

    fun stop() {
        running.set(false)
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private fun acceptLoop() {
        while (running.get()) {
            val client = runCatching { serverSocket?.accept() }.getOrNull() ?: break
            tunnelExecutor.execute { handle(client) }
        }
    }

    private fun handle(client: Socket) {
        client.use { clientSocket ->
            val clientInput = BufferedInputStream(clientSocket.getInputStream())
            val headerBytes = readHeader(clientInput) ?: return
            val firstLine = headerBytes.decodeToString().lineSequence().firstOrNull().orEmpty()
            val request = ProxyRequestLine.parse(firstLine) ?: return

            Socket(upstream.host, upstream.port).use { upstreamSocket ->
                val upstreamOutput = BufferedOutputStream(upstreamSocket.getOutputStream())
                upstreamOutput.write(headerBytes)
                upstreamOutput.flush()

                val upstreamInput = BufferedInputStream(upstreamSocket.getInputStream())
                val clientOutput = BufferedOutputStream(clientSocket.getOutputStream())

                if (request.method.equals("CONNECT", ignoreCase = true)) {
                    val responseHeader = readHeader(upstreamInput) ?: return
                    clientOutput.write(responseHeader)
                    clientOutput.flush()
                }

                val upstreamToClient = tunnelExecutor.submit { pipe(upstreamInput, clientOutput) }
                pipe(clientInput, upstreamOutput)
                upstreamToClient.cancel(true)
            }
        }
    }

    private fun readHeader(input: BufferedInputStream): ByteArray? {
        val output = ArrayList<Byte>(1024)
        var previous3 = -1
        var previous2 = -1
        var previous1 = -1
        while (output.size < MAX_HEADER_BYTES) {
            val current = input.read()
            if (current == -1) return null
            output.add(current.toByte())
            if (previous3 == '\r'.code && previous2 == '\n'.code && previous1 == '\r'.code && current == '\n'.code) {
                return output.toByteArray()
            }
            previous3 = previous2
            previous2 = previous1
            previous1 = current
        }
        return null
    }

    private fun pipe(input: BufferedInputStream, output: BufferedOutputStream) {
        val buffer = ByteArray(32 * 1024)
        while (running.get()) {
            val read = try {
                input.read(buffer)
            } catch (_: Exception) {
                -1
            }
            if (read <= 0) break
            try {
                output.write(buffer, 0, read)
                output.flush()
            } catch (_: Exception) {
                break
            }
        }
    }

    private companion object {
        const val TAG = "LocalHttpProxy"
        const val MAX_HEADER_BYTES = 64 * 1024
    }
}

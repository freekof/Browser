package com.freekof.tvbrowser

import android.os.Handler
import android.os.Looper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class LanInputServer(
    private val port: Int,
    private val onUrlReceived: (String) -> Unit,
) {
    private val running = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        executor.execute {
            try {
                serverSocket = ServerSocket(port)
                while (running.get()) {
                    val socket = serverSocket?.accept() ?: break
                    handle(socket)
                }
            } catch (_: Exception) {
                running.set(false)
            }
        }
    }

    fun stop() {
        running.set(false)
        serverSocket?.close()
        serverSocket = null
    }

    private fun handle(socket: Socket) {
        socket.use {
            val reader = BufferedReader(InputStreamReader(it.getInputStream()))
            val requestLine = reader.readLine().orEmpty()
            while (!reader.readLine().isNullOrEmpty()) {
                // Drain headers so the browser receives a complete response.
            }

            val url = parseSubmittedUrl(requestLine)
            if (url != null) {
                mainHandler.post { onUrlReceived(url) }
                respond(it, submittedPage())
            } else {
                respond(it, formPage())
            }
        }
    }

    private fun parseSubmittedUrl(requestLine: String): String? {
        val path = requestLine.substringAfter(' ', "").substringBefore(' ')
        if (!path.startsWith("/open?")) return null
        return path.substringAfter("url=", "")
            .substringBefore('&')
            .takeIf { it.isNotBlank() }
            ?.let { URLDecoder.decode(it, "UTF-8") }
    }

    private fun respond(socket: Socket, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        socket.getOutputStream().write(
            "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                .toByteArray(Charsets.UTF_8),
        )
        socket.getOutputStream().write(bytes)
    }

    private fun formPage(): String = """
        <!doctype html>
        <html lang="zh-CN">
        <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>TV Browser</title></head>
        <body style="font-family:sans-serif;padding:24px;background:#101820;color:#fff">
        <h2>输入网址</h2>
        <form action="/open" method="get">
        <input name="url" autofocus placeholder="https://example.com" style="box-sizing:border-box;width:100%;font-size:20px;padding:12px">
        <button type="submit" style="margin-top:16px;width:100%;font-size:20px;padding:12px">发送到电视</button>
        </form>
        </body></html>
    """.trimIndent()

    private fun submittedPage(): String = """
        <!doctype html>
        <html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>TV Browser</title></head>
        <body style="font-family:sans-serif;padding:24px;background:#101820;color:#fff"><h2>已发送到电视</h2></body></html>
    """.trimIndent()
}

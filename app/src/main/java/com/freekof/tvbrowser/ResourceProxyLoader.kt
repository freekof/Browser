package com.freekof.tvbrowser

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.net.InetSocketAddress
import java.net.Proxy
import okhttp3.OkHttpClient
import okhttp3.Request

class ResourceProxyLoader {
    fun load(request: WebResourceRequest, settings: HttpProxySettings): WebResourceResponse? {
        if (!settings.isUsable()) return null
        if (request.isForMainFrame) return null
        if (request.method != "GET" && request.method != "HEAD") return null

        val url = request.url.toString()
        val scheme = request.url.scheme ?: return null
        if (scheme != "http" && scheme != "https") return null

        return runCatching {
            val client = OkHttpClient.Builder()
                .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(settings.host, settings.port)))
                .build()
            val builder = Request.Builder().url(url).method(request.method, null)
            copyHeaders(request, builder, settings)
            client.newCall(builder.build()).execute().use { response ->
                val bodyBytes = response.body?.bytes() ?: ByteArray(0)
                val contentType = response.header("content-type")?.substringBefore(';') ?: ResourceMimeTypes.fromUrl(url)
                val encoding = response.header("content-type")
                    ?.substringAfter("charset=", "")
                    ?.takeIf { it.isNotBlank() } ?: "utf-8"
                WebResourceResponse(
                    contentType,
                    encoding,
                    response.code,
                    response.message.ifBlank { "OK" },
                    response.headers.toMultimap().mapValues { it.value.joinToString(",") },
                    ByteArrayInputStream(bodyBytes),
                )
            }
        }.getOrNull()
    }

    private fun copyHeaders(request: WebResourceRequest, builder: Request.Builder, settings: HttpProxySettings) {
        request.requestHeaders.forEach { (name, value) ->
            if (!name.equals("host", ignoreCase = true)) {
                builder.header(name, value)
            }
        }
        builder.header("User-Agent", UserAgentSettings.effective(settings.userAgent))
    }
}

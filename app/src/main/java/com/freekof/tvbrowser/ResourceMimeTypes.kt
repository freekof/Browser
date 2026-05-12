package com.freekof.tvbrowser

object ResourceMimeTypes {
    fun fromUrl(url: String): String {
        val path = url.substringBefore('?').lowercase()
        return when {
            path.endsWith(".js") -> "application/javascript"
            path.endsWith(".mjs") -> "application/javascript"
            path.endsWith(".css") -> "text/css"
            path.endsWith(".html") || path.endsWith(".htm") -> "text/html"
            path.endsWith(".json") -> "application/json"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".webp") -> "image/webp"
            path.endsWith(".gif") -> "image/gif"
            path.endsWith(".svg") -> "image/svg+xml"
            path.endsWith(".woff") -> "font/woff"
            path.endsWith(".woff2") -> "font/woff2"
            path.endsWith(".mp4") -> "video/mp4"
            path.endsWith(".m3u8") -> "application/vnd.apple.mpegurl"
            path.endsWith(".ts") -> "video/mp2t"
            else -> "application/octet-stream"
        }
    }
}

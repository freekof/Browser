package com.freekof.tvbrowser

data class VideoItem(
    val url: String,
    val type: String,
)

class VideoSniffer {
    private val seen = linkedSetMapOf<String, VideoItem>()

    val items: List<VideoItem>
        get() = seen.values.toList()

    fun record(url: String) {
        val cleanUrl = url.substringBefore('#')
        val type = detectType(cleanUrl) ?: return
        seen.putIfAbsent(cleanUrl, VideoItem(cleanUrl, type))
    }

    private fun detectType(url: String): String? {
        val path = url.substringBefore('?').lowercase()
        return VIDEO_EXTENSIONS.firstOrNull { path.endsWith(".$it") }
    }

    private companion object {
        val VIDEO_EXTENSIONS = listOf("m3u8", "mp4", "webm", "mov", "flv", "ts", "m3u")
    }
}

private fun <K, V> linkedSetMapOf(): LinkedHashMap<K, V> = LinkedHashMap()

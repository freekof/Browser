package com.freekof.tvbrowser

object UserAgentSettings {
    // Desktop Chrome UA — critical for sites like njavtv.com that detect TV/mobile UAs
    // and serve degraded pages or broken video players to non-desktop clients.
    const val DEFAULT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0.0.0 Safari/537.36"

    fun effective(value: String): String = value.trim().ifEmpty { DEFAULT }
}

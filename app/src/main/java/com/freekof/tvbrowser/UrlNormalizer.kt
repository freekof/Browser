package com.freekof.tvbrowser

import java.net.URLEncoder

object UrlNormalizer {
    fun normalize(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        if (looksLikeHost(trimmed)) {
            return "https://$trimmed"
        }
        val query = URLEncoder.encode(trimmed, "UTF-8").replace("%20", "+")
        return "https://www.google.com/search?q=$query"
    }

    private fun looksLikeHost(value: String): Boolean {
        return !value.contains(' ') && value.contains('.')
    }
}

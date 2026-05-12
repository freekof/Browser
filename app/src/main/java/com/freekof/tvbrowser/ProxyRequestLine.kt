package com.freekof.tvbrowser

data class ProxyRequestLine(
    val method: String,
    val target: String,
    val version: String,
) {
    companion object {
        fun parse(line: String): ProxyRequestLine? {
            val parts = line.trim().split(' ', limit = 3)
            if (parts.size != 3) return null
            return ProxyRequestLine(parts[0], parts[1], parts[2])
        }
    }
}

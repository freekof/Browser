package com.freekof.tvbrowser

object UserAgentSettings {
    const val DEFAULT = "Mozilla/5.0 (Linux; Android 9; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"

    fun effective(value: String): String = value.trim().ifEmpty { DEFAULT }
}

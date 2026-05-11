package com.freekof.tvbrowser

import android.content.Context

class Socks5SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("socks5", Context.MODE_PRIVATE)

    fun load(): Socks5Settings = Socks5Settings(
        enabled = preferences.getBoolean("enabled", false),
        host = preferences.getString("host", "").orEmpty(),
        port = preferences.getInt("port", 1080),
        username = preferences.getString("username", "").orEmpty(),
        password = preferences.getString("password", "").orEmpty(),
        proxyDns = preferences.getBoolean("proxyDns", true),
    )

    fun save(settings: Socks5Settings) {
        preferences.edit()
            .putBoolean("enabled", settings.enabled)
            .putString("host", settings.host)
            .putInt("port", settings.port)
            .putString("username", settings.username)
            .putString("password", settings.password)
            .putBoolean("proxyDns", settings.proxyDns)
            .apply()
    }
}

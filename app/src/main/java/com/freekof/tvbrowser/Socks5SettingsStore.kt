package com.freekof.tvbrowser

import android.content.Context

class Socks5SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("socks5", Context.MODE_PRIVATE)

    fun load(): Socks5Settings = Socks5Settings(
        enabled = preferences.getBoolean("enabled", false),
        proxyType = runCatching { ProxyType.valueOf(preferences.getString("proxyType", ProxyType.Socks5.name).orEmpty()) }
            .getOrDefault(ProxyType.Socks5),
        host = preferences.getString("host", "").orEmpty(),
        port = preferences.getInt("port", 1080),
        username = preferences.getString("username", "").orEmpty(),
        password = preferences.getString("password", "").orEmpty(),
        proxyDns = preferences.getBoolean("proxyDns", true),
        userAgent = UserAgentSettings.effective(preferences.getString("userAgent", UserAgentSettings.DEFAULT).orEmpty()),
    )

    fun save(settings: Socks5Settings) {
        preferences.edit()
            .putBoolean("enabled", settings.enabled)
            .putString("proxyType", settings.proxyType.name)
            .putString("host", settings.host)
            .putInt("port", settings.port)
            .putString("username", settings.username)
            .putString("password", settings.password)
            .putBoolean("proxyDns", settings.proxyDns)
            .putString("userAgent", UserAgentSettings.effective(settings.userAgent))
            .apply()
    }
}

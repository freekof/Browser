package com.freekof.tvbrowser

import android.content.Context

class HttpProxySettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("http_proxy", Context.MODE_PRIVATE)

    fun load(): HttpProxySettings = HttpProxySettings(
        enabled = preferences.getBoolean("enabled", false),
        host = preferences.getString("host", "").orEmpty(),
        port = preferences.getInt("port", 1080),
        username = preferences.getString("username", "").orEmpty(),
        password = preferences.getString("password", "").orEmpty(),
        proxyDns = preferences.getBoolean("proxyDns", true),
        userAgent = UserAgentSettings.effective(preferences.getString("userAgent", UserAgentSettings.DEFAULT).orEmpty()),
    )

    fun save(settings: HttpProxySettings) {
        preferences.edit()
            .putBoolean("enabled", settings.enabled)
            .putString("host", settings.host)
            .putInt("port", settings.port)
            .putString("username", settings.username)
            .putString("password", settings.password)
            .putBoolean("proxyDns", settings.proxyDns)
            .putString("userAgent", UserAgentSettings.effective(settings.userAgent))
            .apply()
    }
}

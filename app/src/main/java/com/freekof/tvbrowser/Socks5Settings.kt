package com.freekof.tvbrowser

data class Socks5Settings(
    val enabled: Boolean = false,
    val proxyType: ProxyType = ProxyType.Socks5,
    val host: String = "",
    val port: Int = 1080,
    val username: String = "",
    val password: String = "",
    val proxyDns: Boolean = true,
    val userAgent: String = UserAgentSettings.DEFAULT,
) {
    fun isUsable(): Boolean = enabled && host.isNotBlank() && port in 1..65535
}

enum class ProxyType(val scheme: String) {
    Socks5("socks"),
    Http("http"),
}

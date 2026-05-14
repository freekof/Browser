package com.freekof.tvbrowser

object GeckoProxyConfig {
    fun prefs(settings: HttpProxySettings): Map<String, Any> {
        if (!settings.isUsable()) {
            return mapOf(
                "network.proxy.type" to 0,
            )
        }

        return mapOf(
            "network.proxy.type" to 1,
            "network.proxy.http" to settings.host,
            "network.proxy.http_port" to settings.port,
            "network.proxy.ssl" to settings.host,
            "network.proxy.ssl_port" to settings.port,
            "network.proxy.share_proxy_settings" to true,
            "network.proxy.no_proxies_on" to "",
            "network.proxy.socks_remote_dns" to settings.proxyDns,
        )
    }

    fun yaml(settings: HttpProxySettings): String {
        val prefLines = prefs(settings).entries.joinToString("\n") { (key, value) ->
            val renderedValue = when (value) {
                is String -> "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
                else -> value.toString()
            }
            "  $key: $renderedValue"
        }
        return "prefs:\n$prefLines\n"
    }
}

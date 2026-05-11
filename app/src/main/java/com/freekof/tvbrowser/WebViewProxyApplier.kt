package com.freekof.tvbrowser

import android.content.Context
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature

object WebViewProxyApplier {
    fun apply(context: Context, settings: HttpProxySettings): Boolean {
        return if (settings.isUsable()) {
            applyProxy(context, "http://${settings.host}:${settings.port}")
        } else {
            clearProxy(context)
        }
    }

    private fun applyProxy(context: Context, proxyUrl: String): Boolean {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return false
        return runCatching {
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule(proxyUrl)
                .addBypassRule("127.0.0.1")
                .addBypassRule("localhost")
                .build()
            ProxyController.getInstance().setProxyOverride(proxyConfig, context.mainExecutor) {}
        }.isSuccess
    }

    private fun clearProxy(context: Context): Boolean {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return false
        return runCatching {
            ProxyController.getInstance().clearProxyOverride(context.mainExecutor) {}
        }.isSuccess
    }
}

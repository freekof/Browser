package com.freekof.tvbrowser

import android.content.Context
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature

object WebViewProxyApplier {
    fun apply(context: Context, settings: HttpProxySettings, onApplied: (Boolean) -> Unit = {}): Boolean {
        return if (settings.isUsable()) {
            applyProxy(context, "http://${settings.host}:${settings.port}", onApplied)
        } else {
            clearProxy(context, onApplied)
        }
    }

    private fun applyProxy(context: Context, proxyUrl: String, onApplied: (Boolean) -> Unit): Boolean {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            onApplied(false)
            return false
        }
        return runCatching {
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule(proxyUrl)
                .addBypassRule("127.0.0.1")
                .addBypassRule("localhost")
                .build()
            ProxyController.getInstance().setProxyOverride(proxyConfig, context.mainExecutor) { onApplied(true) }
            true
        }.getOrElse {
            onApplied(false)
            false
        }
    }

    private fun clearProxy(context: Context, onApplied: (Boolean) -> Unit): Boolean {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            onApplied(false)
            return false
        }
        return runCatching {
            ProxyController.getInstance().clearProxyOverride(context.mainExecutor) { onApplied(true) }
            true
        }.getOrElse {
            onApplied(false)
            false
        }
    }
}

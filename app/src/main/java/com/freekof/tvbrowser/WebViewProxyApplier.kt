package com.freekof.tvbrowser

import android.content.Context
import android.util.Log
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature

object WebViewProxyApplier {

    private const val TAG = "WebViewProxyApplier"

    fun apply(context: Context, settings: HttpProxySettings, onApplied: (Boolean) -> Unit = {}): Boolean {
        val featureSupported = WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)
        Log.i(TAG, "PROXY_OVERRIDE feature supported: $featureSupported")

        return if (settings.isUsable()) {
            applyProxy(context, settings, featureSupported, onApplied)
        } else {
            clearProxy(context, featureSupported, onApplied)
        }
    }

    private fun applyProxy(
        context: Context,
        settings: HttpProxySettings,
        featureSupported: Boolean,
        onApplied: (Boolean) -> Unit,
    ): Boolean {
        // ── Method 1: androidx.webkit.ProxyController (API 28+ WebView) ──────
        if (featureSupported) {
            return runCatching {
                val proxyUrl = buildProxyUrl(settings)
                Log.i(TAG, "Applying proxy via ProxyController: $proxyUrl")

                val proxyConfig = ProxyConfig.Builder()
                    .addProxyRule(proxyUrl)
                    .addBypassRule("127.0.0.1")
                    .addBypassRule("localhost")
                    .addBypassRule("<-loopback>")
                    .also { if (settings.proxyDns) it.setBypassSimpleHostnames(false) }
                    .build()

                ProxyController.getInstance().setProxyOverride(
                    proxyConfig,
                    context.mainExecutor,
                ) {
                    Log.i(TAG, "ProxyController callback fired — proxy is now active")
                    // Also set system properties as belt-and-suspenders fallback
                    setSystemProperties(settings)
                    onApplied(true)
                }
                true
            }.getOrElse { e ->
                Log.e(TAG, "ProxyController.setProxyOverride failed", e)
                // Fall through to system properties method
                applyViaSystemProperties(settings, onApplied)
            }
        }

        // ── Method 2: JVM system properties fallback ──────────────────────────
        Log.w(TAG, "ProxyController not available, using system properties fallback")
        return applyViaSystemProperties(settings, onApplied)
    }

    /**
     * System properties are read by the WebView's network stack on older
     * Android / WebView versions where ProxyController is unavailable.
     * Also serves as a belt-and-suspenders backup alongside ProxyController.
     */
    private fun applyViaSystemProperties(
        settings: HttpProxySettings,
        onApplied: (Boolean) -> Unit,
    ): Boolean {
        return runCatching {
            setSystemProperties(settings)
            Log.i(TAG, "System proxy properties set: ${settings.host}:${settings.port}")
            onApplied(true)
            true
        }.getOrElse { e ->
            Log.e(TAG, "System properties fallback failed", e)
            onApplied(false)
            false
        }
    }

    private fun setSystemProperties(settings: HttpProxySettings) {
        listOf("http", "https").forEach { scheme ->
            System.setProperty("$scheme.proxyHost", settings.host)
            System.setProperty("$scheme.proxyPort", settings.port.toString())
            if (settings.username.isNotBlank()) {
                System.setProperty("$scheme.proxyUser", settings.username)
                System.setProperty("$scheme.proxyPassword", settings.password)
            }
        }
        Log.d(TAG, "System properties: http.proxyHost=${System.getProperty("http.proxyHost")} " +
                "http.proxyPort=${System.getProperty("http.proxyPort")}")
    }

    private fun clearProxy(
        context: Context,
        featureSupported: Boolean,
        onApplied: (Boolean) -> Unit,
    ): Boolean {
        // Clear system properties first regardless
        clearSystemProperties()

        if (!featureSupported) {
            onApplied(true)
            return true
        }
        return runCatching {
            ProxyController.getInstance().clearProxyOverride(context.mainExecutor) {
                Log.i(TAG, "ProxyController cleared")
                onApplied(true)
            }
            true
        }.getOrElse { e ->
            Log.e(TAG, "Failed to clear proxy", e)
            onApplied(false)
            false
        }
    }

    private fun clearSystemProperties() {
        listOf("http", "https").forEach { scheme ->
            System.clearProperty("$scheme.proxyHost")
            System.clearProperty("$scheme.proxyPort")
            System.clearProperty("$scheme.proxyUser")
            System.clearProperty("$scheme.proxyPassword")
        }
        Log.i(TAG, "System proxy properties cleared")
    }

    private fun buildProxyUrl(settings: HttpProxySettings): String =
        if (settings.username.isNotBlank() && settings.password.isNotBlank()) {
            "http://${settings.username}:${settings.password}@${settings.host}:${settings.port}"
        } else {
            "http://${settings.host}:${settings.port}"
        }
}

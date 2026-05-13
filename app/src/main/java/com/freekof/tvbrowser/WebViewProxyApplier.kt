package com.freekof.tvbrowser

import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy as JProxy
import java.util.Properties
import java.util.concurrent.Executor

object WebViewProxyApplier {

    fun apply(context: Context, settings: HttpProxySettings): Boolean {
        return if (settings.isUsable()) {
            applyProxy(context, settings.host, settings.port)
        } else {
            clearProxy(context)
        }
    }

    private fun applyProxy(context: Context, host: String, port: Int): Boolean {
        // 1) 优先用 androidx.webkit (需要 WebView 内核支持 PROXY_OVERRIDE)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            val ok = runCatching {
                val proxyUrl = "$host:$port"
                val builder = ProxyConfig.Builder()
                    .addProxyRule(proxyUrl)
                    .addBypassRule("127.0.0.1")
                    .addBypassRule("localhost")
                ProxyController.getInstance()
                    .setProxyOverride(builder.build(), Executor { it.run() }) {}
                true
            }.getOrDefault(false)
            if (ok) return true
        }
        // 2) TV 盒子上 WebView 内核常常老旧、不支持 PROXY_OVERRIDE，
        //    这时回落到反射设置系统属性 + ProxyChangeListener 的老办法
        return applyProxyReflect(context, host, port.toString())
    }

    private fun clearProxy(context: Context): Boolean {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            runCatching {
                ProxyController.getInstance()
                    .clearProxyOverride(Executor { it.run() }) {}
            }
        }
        // 反射代理也一并清掉
        return clearProxyReflect(context)
    }

    /**
     * 反射方案：适配 KitKat 及以上，含 Android 9 (Pie) 的 TV 盒子。
     * 通过设置 JVM 系统属性 + 触发 WebView 的 ProxyChangeListener#onReceive 让内核读取代理。
     */
    private fun applyProxyReflect(context: Context, host: String, port: String): Boolean {
        return runCatching {
            // 系统属性（OkHttp / WebView 内核都会参考）
            System.setProperty("http.proxyHost", host)
            System.setProperty("http.proxyPort", port)
            System.setProperty("https.proxyHost", host)
            System.setProperty("https.proxyPort", port)

            // 让 WebView 重新读取系统代理
            triggerProxyChange(context)
            true
        }.getOrDefault(false)
    }

    private fun clearProxyReflect(context: Context): Boolean {
        return runCatching {
            System.clearProperty("http.proxyHost")
            System.clearProperty("http.proxyPort")
            System.clearProperty("https.proxyHost")
            System.clearProperty("https.proxyPort")
            triggerProxyChange(context)
            true
        }.getOrDefault(false)
    }

    /**
     * 反射触发 Chromium WebView 的 ProxyChangeListener#proxySettingsChanged
     * 让内核立刻应用刚设置的系统属性。
     */
    private fun triggerProxyChange(context: Context) {
        val appContext = context.applicationContext
        // 不同 WebView 版本，类名可能在以下几个位置
        val candidateClasses = listOf(
            "android.webkit.WebViewClassic\$Proxy",
            "org.chromium.net.ProxyChangeListener",
            "com.android.org.chromium.net.ProxyChangeListener",
        )

        for (className in candidateClasses) {
            val cls = runCatching { Class.forName(className) }.getOrNull() ?: continue
            // 老路径：直接发 Proxy.PROXY_CHANGE_ACTION 广播
            runCatching {
                val intent = android.content.Intent("android.intent.action.PROXY_CHANGE")
                appContext.sendBroadcast(intent)
            } 
        }

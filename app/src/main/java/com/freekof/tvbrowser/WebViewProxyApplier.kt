package com.freekof.tvbrowser

import android.content.Context
import android.os.Build

object WebViewProxyApplier {
    fun apply(context: Context, settings: Socks5Settings): Boolean {
        return if (settings.isUsable()) {
            applyProxy(context, "${settings.proxyType.scheme}://${settings.host}:${settings.port}")
        } else {
            clearProxy(context)
        }
    }

    private fun applyProxy(context: Context, proxyUrl: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return runCatching {
            val proxyConfigClass = Class.forName("android.webkit.ProxyConfig")
            val proxyControllerClass = Class.forName("android.webkit.ProxyController")
            val executor = context.mainExecutor
            val listener = Runnable {}

            val builderClass = Class.forName("android.webkit.ProxyConfig\$Builder")
            val builder = builderClass.getConstructor().newInstance()
            builder.javaClass.getMethod("addProxyRule", String::class.java).invoke(builder, proxyUrl)
            builder.javaClass.getMethod("addBypassRule", String::class.java).invoke(builder, "127.0.0.1")
            builder.javaClass.getMethod("addBypassRule", String::class.java).invoke(builder, "localhost")
            val proxyConfig = builder.javaClass.getMethod("build").invoke(builder)
            val controller = proxyControllerClass.getMethod("getInstance").invoke(null)
            proxyControllerClass.getMethod("setProxyOverride", proxyConfigClass, java.util.concurrent.Executor::class.java, Runnable::class.java)
                .invoke(controller, proxyConfig, executor, listener)
        }.isSuccess
    }

    private fun clearProxy(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return runCatching {
            val proxyControllerClass = Class.forName("android.webkit.ProxyController")
            val controller = proxyControllerClass.getMethod("getInstance").invoke(null)
            proxyControllerClass.getMethod("clearProxyOverride", java.util.concurrent.Executor::class.java, Runnable::class.java)
                .invoke(controller, context.mainExecutor, Runnable {})
        }.isSuccess
    }
}

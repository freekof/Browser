package com.freekof.tvbrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeckoProxyConfigTest {
    @Test
    fun `disabled settings turn gecko proxy off`() {
        assertThat(GeckoProxyConfig.prefs(HttpProxySettings(enabled = false)))
            .containsExactly("network.proxy.type", 0)
    }

    @Test
    fun `enabled http proxy is applied to http and ssl traffic`() {
        val prefs = GeckoProxyConfig.prefs(
            HttpProxySettings(enabled = true, host = "192.168.2.1", port = 1080, proxyDns = true),
        )

        assertThat(prefs).containsEntry("network.proxy.type", 1)
        assertThat(prefs).containsEntry("network.proxy.http", "192.168.2.1")
        assertThat(prefs).containsEntry("network.proxy.http_port", 1080)
        assertThat(prefs).containsEntry("network.proxy.ssl", "192.168.2.1")
        assertThat(prefs).containsEntry("network.proxy.ssl_port", 1080)
        assertThat(prefs).containsEntry("network.proxy.share_proxy_settings", true)
        assertThat(prefs).containsEntry("network.proxy.no_proxies_on", "")
    }

    @Test
    fun `yaml quotes string prefs`() {
        assertThat(GeckoProxyConfig.yaml(HttpProxySettings(enabled = true, host = "proxy.local", port = 8080)))
            .contains("  network.proxy.http: \"proxy.local\"")
    }
}

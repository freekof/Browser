package com.freekof.tvbrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HttpProxySettingsTest {
    @Test
    fun `disabled settings are invalid for proxy use`() {
        assertThat(HttpProxySettings(enabled = false, host = "127.0.0.1", port = 8080).isUsable()).isFalse()
    }

    @Test
    fun `enabled settings require host and valid port`() {
        assertThat(HttpProxySettings(enabled = true, host = "127.0.0.1", port = 8080).isUsable()).isTrue()
        assertThat(HttpProxySettings(enabled = true, host = "", port = 8080).isUsable()).isFalse()
        assertThat(HttpProxySettings(enabled = true, host = "127.0.0.1", port = 70000).isUsable()).isFalse()
    }
}

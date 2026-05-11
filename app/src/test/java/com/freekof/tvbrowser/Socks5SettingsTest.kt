package com.freekof.tvbrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Socks5SettingsTest {
    @Test
    fun `disabled settings are invalid for proxy use`() {
        assertThat(Socks5Settings(enabled = false, host = "127.0.0.1", port = 1080).isUsable()).isFalse()
    }

    @Test
    fun `enabled settings require host and valid port`() {
        assertThat(Socks5Settings(enabled = true, host = "127.0.0.1", port = 1080).isUsable()).isTrue()
        assertThat(Socks5Settings(enabled = true, host = "", port = 1080).isUsable()).isFalse()
        assertThat(Socks5Settings(enabled = true, host = "127.0.0.1", port = 70000).isUsable()).isFalse()
    }
}

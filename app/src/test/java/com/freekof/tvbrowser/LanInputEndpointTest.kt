package com.freekof.tvbrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LanInputEndpointTest {
    @Test
    fun `formats qr content as ip and port`() {
        assertThat(LanInputEndpoint.qrContent("192.168.1.20", 8787)).isEqualTo("192.168.1.20:8787")
    }
}

package com.freekof.tvbrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LanInputEndpointTest {
    @Test
    fun `formats qr content as http url`() {
        assertThat(LanInputEndpoint.qrContent("192.168.1.20", 8787)).isEqualTo("http://192.168.1.20:8787")
    }
}

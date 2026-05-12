package com.freekof.tvbrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProxyRequestLineTest {
    @Test
    fun `parses connect request`() {
        val request = ProxyRequestLine.parse("CONNECT example.com:443 HTTP/1.1")

        assertThat(request).isEqualTo(ProxyRequestLine(method = "CONNECT", target = "example.com:443", version = "HTTP/1.1"))
    }

    @Test
    fun `parses absolute http request`() {
        val request = ProxyRequestLine.parse("GET http://example.com/path HTTP/1.1")

        assertThat(request).isEqualTo(ProxyRequestLine(method = "GET", target = "http://example.com/path", version = "HTTP/1.1"))
    }
}

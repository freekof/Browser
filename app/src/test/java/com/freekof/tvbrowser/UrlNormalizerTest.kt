package com.freekof.tvbrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UrlNormalizerTest {
    @Test
    fun `adds https scheme to bare host`() {
        assertThat(UrlNormalizer.normalize("example.com/movie")).isEqualTo("https://example.com/movie")
    }

    @Test
    fun `keeps explicit http scheme`() {
        assertThat(UrlNormalizer.normalize("http://example.com")).isEqualTo("http://example.com")
    }

    @Test
    fun `turns search text into google query`() {
        assertThat(UrlNormalizer.normalize("hello tv browser"))
            .isEqualTo("https://www.google.com/search?q=hello+tv+browser")
    }
}

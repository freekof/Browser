package com.freekof.tvbrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ResourceMimeTypesTest {
    @Test
    fun `detects javascript mime type`() {
        assertThat(ResourceMimeTypes.fromUrl("https://example.com/app.js")).isEqualTo("application/javascript")
    }

    @Test
    fun `detects css mime type with query`() {
        assertThat(ResourceMimeTypes.fromUrl("https://example.com/style.css?v=1")).isEqualTo("text/css")
    }

    @Test
    fun `falls back to octet stream`() {
        assertThat(ResourceMimeTypes.fromUrl("https://example.com/file.unknown")).isEqualTo("application/octet-stream")
    }
}

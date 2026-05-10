package com.freekof.tvbrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VideoSnifferTest {
    @Test
    fun `detects common video URLs`() {
        val sniffer = VideoSniffer()

        sniffer.record("https://cdn.example.com/live/index.m3u8?token=abc")
        sniffer.record("https://cdn.example.com/file.mp4")

        assertThat(sniffer.items.map { it.type }).containsExactly("m3u8", "mp4")
    }

    @Test
    fun `ignores non video URLs`() {
        val sniffer = VideoSniffer()

        sniffer.record("https://example.com/style.css")

        assertThat(sniffer.items).isEmpty()
    }

    @Test
    fun `deduplicates URLs without fragment`() {
        val sniffer = VideoSniffer()

        sniffer.record("https://cdn.example.com/live/index.m3u8#one")
        sniffer.record("https://cdn.example.com/live/index.m3u8#two")

        assertThat(sniffer.items).hasSize(1)
    }
}

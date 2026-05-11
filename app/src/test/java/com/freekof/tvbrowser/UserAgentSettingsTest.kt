package com.freekof.tvbrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UserAgentSettingsTest {
    @Test
    fun `default user agent uses chrome 124`() {
        assertThat(UserAgentSettings.DEFAULT).contains("Chrome/124.0.0.0")
    }

    @Test
    fun `blank user agent falls back to default`() {
        assertThat(UserAgentSettings.effective("   ")).isEqualTo(UserAgentSettings.DEFAULT)
    }
}

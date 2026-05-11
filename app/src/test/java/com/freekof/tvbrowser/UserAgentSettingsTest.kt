package com.freekof.tvbrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UserAgentSettingsTest {
    @Test
    fun `default user agent uses chrome 138`() {
        assertThat(UserAgentSettings.DEFAULT).contains("Chrome/138.0.0.0")
    }

    @Test
    fun `blank user agent falls back to default`() {
        assertThat(UserAgentSettings.effective("   ")).isEqualTo(UserAgentSettings.DEFAULT)
    }
}

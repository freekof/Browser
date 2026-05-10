package com.freekof.tvbrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackKeyPolicyTest {
    @Test
    fun `shows controls when controls are hidden`() {
        assertThat(BackKeyPolicy.decide(controlsVisible = false, canGoBack = true))
            .isEqualTo(BackKeyAction.ShowControls)
    }

    @Test
    fun `goes back when controls are visible and page can go back`() {
        assertThat(BackKeyPolicy.decide(controlsVisible = true, canGoBack = true))
            .isEqualTo(BackKeyAction.NavigateBack)
    }

    @Test
    fun `exits when controls are visible and page cannot go back`() {
        assertThat(BackKeyPolicy.decide(controlsVisible = true, canGoBack = false))
            .isEqualTo(BackKeyAction.Exit)
    }
}

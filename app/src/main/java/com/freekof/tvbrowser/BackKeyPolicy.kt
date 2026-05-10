package com.freekof.tvbrowser

enum class BackKeyAction {
    ShowControls,
    NavigateBack,
    Exit,
}

object BackKeyPolicy {
    fun decide(controlsVisible: Boolean, canGoBack: Boolean): BackKeyAction = when {
        !controlsVisible -> BackKeyAction.ShowControls
        canGoBack -> BackKeyAction.NavigateBack
        else -> BackKeyAction.Exit
    }
}

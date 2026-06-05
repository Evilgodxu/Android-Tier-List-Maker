package com.tdds.jh.ui.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.window.core.layout.WindowSizeClass

/** CompositionLocal 用于在整个 Compose 树中传递 WindowSizeClass */
val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> {
    error("WindowSizeClass not provided")
}

/** 将当前窗口的 WindowSizeClass 注入到 CompositionLocal */
@Composable
fun ProvideWindowSizeClass(content: @Composable () -> Unit) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
        content()
    }
}

/** 获取当前窗口尺寸分类 */
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    return LocalWindowSizeClass.current
}

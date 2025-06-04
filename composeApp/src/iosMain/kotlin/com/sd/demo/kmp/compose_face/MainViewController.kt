package com.sd.demo.kmp.compose_face

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.ComposeUIViewController
import com.sd.lib.kmp.face.FaceManager

fun MainViewController() = ComposeUIViewController {
  DisposableEffect(Unit) {
    FaceManager.init()
    onDispose { FaceManager.release() }
  }
  ComposeApp()
}
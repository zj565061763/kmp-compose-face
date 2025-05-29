package com.sd.lib.kmp.compose_face

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
actual fun FaceView(
  modifier: Modifier,
  vm: FaceViewModel,
) {
  val state by vm.stateFlow.collectAsStateWithLifecycle()

  val detector = remember { FaceInfoDetector() }
  DisposableEffect(detector) {
    onDispose { detector.release() }
  }

  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    if (state.stage !is FaceViewModel.Stage.Finished) {
      CameraPreviewView(modifier = Modifier.matchParentSize()) {
        val bitmap = it.toBitmap()
        val faceInfo = detector.detect(bitmap)
        vm.process(faceInfo)
      }
    }
  }
}
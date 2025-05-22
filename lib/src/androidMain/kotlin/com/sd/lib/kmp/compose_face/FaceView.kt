package com.sd.lib.kmp.compose_face

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FaceView(
  modifier: Modifier = Modifier,
  vm: FaceViewModel,
) {
  val state by vm.stateFlow.collectAsStateWithLifecycle()

  val detector = remember { FaceInfoDetector() }
  DisposableEffect(detector) {
    onDispose { detector.release() }
  }

  Box(
    modifier = modifier
      .clip(CircleShape)
      .background(Color.Black),
    contentAlignment = Alignment.Center,
  ) {
    if (state.stage !is FaceViewModel.Stage.Finished) {
      CameraPreviewView {
        val bitmap = it.toBitmap()
        val faceInfo = detector.detect(bitmap)
        vm.process(faceInfo)
      }
    }
  }
}
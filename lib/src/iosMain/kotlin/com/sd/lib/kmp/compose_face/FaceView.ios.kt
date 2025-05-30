package com.sd.lib.kmp.compose_face

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress
import platform.CoreVideo.kCVPixelBufferLock_ReadOnly

@OptIn(ExperimentalForeignApi::class)
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
        val imageBuffer = CMSampleBufferGetImageBuffer(it)
        if (imageBuffer != null) {
          try {
            // lock
            CVPixelBufferLockBaseAddress(imageBuffer, kCVPixelBufferLock_ReadOnly)
            val faceInfo = detector.detect(imageBuffer)
            vm.process(faceInfo)
          } finally {
            // unlock
            CVPixelBufferUnlockBaseAddress(imageBuffer, kCVPixelBufferLock_ReadOnly)
          }
        } else {
          FaceManager.log { "CMSampleBufferGetImageBuffer returns null" }
        }
      }
    }
  }
}
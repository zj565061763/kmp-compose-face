package com.sd.lib.kmp.compose_face

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.CoreMedia.CMSampleBufferRef
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
@Composable
internal fun CameraPreviewView(
  modifier: Modifier = Modifier,
  onData: (CMSampleBufferRef) -> Unit,
) {
  val onDataUpdated by rememberUpdatedState(onData)

  val captureSession = remember {
    runCatching {
      createAVCaptureSession { buffer ->
        onDataUpdated(buffer)
      }
    }.getOrElse {
      FaceManager.log { "createAVCaptureSession error $it" }
      null
    }
  } ?: return

  var previewView by remember { mutableStateOf<UIView?>(null) }

  val pv = previewView
  if (pv != null) {
    val previewLayer = remember { AVCaptureVideoPreviewLayer.layerWithSession(captureSession) }
    LaunchedEffect(pv) {
      previewLayer.frame = pv.bounds
      previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
      previewLayer.session = captureSession
      previewLayer.removeFromSuperlayer()
      pv.layer.addSublayer(previewLayer)
    }
    LifecycleResumeEffect(pv) {
      captureSession.startRunning()
      onPauseOrDispose { captureSession.stopRunning() }
    }
  }

  UIKitView(
    modifier = modifier,
    factory = { UIView().also { previewView = it } },
  )
}
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
import com.sd.lib.kmp.face.FaceManager
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.CoreMedia.CMSampleBufferRef
import platform.UIKit.UIView
import platform.darwin.DISPATCH_QUEUE_SERIAL
import platform.darwin.NSObject
import platform.darwin.dispatch_queue_create

@OptIn(ExperimentalForeignApi::class)
@Composable
internal fun CameraPreviewView(
  modifier: Modifier = Modifier,
  onData: (CMSampleBufferRef) -> Unit,
) {
  val onDataUpdated by rememberUpdatedState(onData)

  val queue = remember {
    dispatch_queue_create("VideoDataOutputQueue", DISPATCH_QUEUE_SERIAL as? NSObject)
  }

  val delegate = remember {
    object : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
      override fun captureOutput(output: AVCaptureOutput, didOutputSampleBuffer: CMSampleBufferRef?, fromConnection: AVCaptureConnection) {
        if (didOutputSampleBuffer != null) {
          onDataUpdated(didOutputSampleBuffer)
        }
      }
    }
  }

  val captureSession = remember {
    runCatching {
      createAVCaptureSession(queue = queue, delegate = delegate)
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
package com.sd.lib.kmp.compose_face

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.camera.view.RotationProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sd.lib.kmp.face.FaceManager

@Composable
internal fun CameraPreviewView(
  modifier: Modifier = Modifier,
  onImageProxy: (ImageProxy) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val executor = remember { ContextCompat.getMainExecutor(context) }

  val previewUseCase = remember {
    Preview.Builder()
      .setResolutionSelector(
        ResolutionSelector.Builder()
          .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
          .build()
      )
      .build()
  }
  val imageAnalyzerUseCase = remember {
    ImageAnalysis.Builder()
      .setOutputImageRotationEnabled(true)
      .build()
  }

  var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
  var previewView by remember { mutableStateOf<PreviewView?>(null) }

  val pv = previewView
  if (pv != null) {
    val onImageProxyUpdated by rememberUpdatedState(onImageProxy)
    LaunchedEffect(pv, lifecycleOwner) {
      previewUseCase.surfaceProvider = pv.surfaceProvider

      imageAnalyzerUseCase.setAnalyzer(executor) { imageProxy ->
        try {
          onImageProxyUpdated(imageProxy)
        } finally {
          imageProxy.close()
        }
      }

      val cp = cameraProvider ?: ProcessCameraProvider.awaitInstance(context).also { cameraProvider = it }
      cp.unbindAll()

      runCatching {
        cp.bindToLifecycle(
          lifecycleOwner,
          CameraSelector.DEFAULT_FRONT_CAMERA,
          previewUseCase, imageAnalyzerUseCase,
        )
      }.onFailure {
        FaceManager.log { "CameraProvider bindToLifecycle error:$it" }
      }
    }
  }

  DisposableEffect(Unit) {
    val rotationProvider = RotationProvider(context)
    val listener = object : RotationProvider.Listener {
      override fun onRotationChanged(rotation: Int) {
        imageAnalyzerUseCase.targetRotation = rotation
      }
    }
    val addListener = rotationProvider.addListener(executor, listener)
    onDispose {
      if (addListener) {
        rotationProvider.removeListener(listener)
      }
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { PreviewView(it).also { previewView = it } },
  )
}
package com.sd.demo.kmp.compose_face

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.sd.lib.kmp.compose_face.FaceManager
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType

fun MainViewController() = ComposeUIViewController {
  if (checkPermissions()) {
    DisposableEffect(Unit) {
      FaceManager.init()
      onDispose { FaceManager.release() }
    }
    ComposeApp()
  }
}

@Composable
private fun checkPermissions(): Boolean {
  var hasPermissions by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
    when (status) {
      AVAuthorizationStatusNotDetermined -> {
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
          hasPermissions = granted
        }
      }

      AVAuthorizationStatusAuthorized -> {
        hasPermissions = true
      }

      AVAuthorizationStatusDenied,
      AVAuthorizationStatusRestricted,
        -> {
        hasPermissions = false
      }

      else -> {
        hasPermissions = false
      }
    }
  }
  return hasPermissions
}
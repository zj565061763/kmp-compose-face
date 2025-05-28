package com.sd.demo.kmp.compose_face.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType

@Composable
actual fun rememberPermissionUtils(): PermissionUtils {
  return remember { PermissionUtilsImpl() }.also { it.Init() }
}

private class PermissionUtilsImpl : PermissionUtils {
  private var _hasPermissions by mutableStateOf(hasPermissions())

  override val hasPermissions: Boolean get() = _hasPermissions

  @Composable
  fun Init() {
    LifecycleResumeEffect(Unit) {
      _hasPermissions = hasPermissions()
      onPauseOrDispose { }
    }
  }

  override fun requestPermissions() {
    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
      _hasPermissions = granted
    }
  }
}

private fun hasPermissions(): Boolean {
  return AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized
}
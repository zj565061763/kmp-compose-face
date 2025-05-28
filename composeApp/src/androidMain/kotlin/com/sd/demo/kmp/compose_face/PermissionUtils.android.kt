package com.sd.demo.kmp.compose_face

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

@Composable
actual fun rememberPermissionUtils(): PermissionUtils {
  val context = LocalContext.current
  return remember(context) { PermissionUtilsImpl(context) }.apply { Init() }
}

private class PermissionUtilsImpl(
  private val context: Context,
) : PermissionUtils {
  private lateinit var _launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
  private var _hasPermissions by mutableStateOf(hasPermission(context))

  override val hasPermissions: Boolean get() = _hasPermissions

  @Composable
  fun Init() {
    _launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      _hasPermissions = it.all { it.value }
    }
    LifecycleResumeEffect(Unit) {
      _hasPermissions = hasPermission(context)
      onPauseOrDispose { }
    }
  }

  override fun requestPermissions() {
    _launcher.launch(PERMISSIONS_REQUIRED)
  }
}

private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

private fun hasPermission(context: Context): Boolean {
  return PERMISSIONS_REQUIRED.all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
  }
}
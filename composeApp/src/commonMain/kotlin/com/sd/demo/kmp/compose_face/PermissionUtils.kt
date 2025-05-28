package com.sd.demo.kmp.compose_face

import androidx.compose.runtime.Composable

interface PermissionUtils {
  val hasPermissions: Boolean
  fun requestPermissions()
}

@Composable
expect fun rememberPermissionUtils(): PermissionUtils
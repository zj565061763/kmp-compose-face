package com.sd.demo.kmp.compose_face

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(
  isLight: Boolean = true,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (isLight) lightColorScheme() else darkColorScheme(),
    content = content
  )
}
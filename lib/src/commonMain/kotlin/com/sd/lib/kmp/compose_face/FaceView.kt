package com.sd.lib.kmp.compose_face

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun FaceView(
  modifier: Modifier = Modifier,
  vm: FaceViewModel,
)
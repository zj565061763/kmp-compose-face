package com.sd.demo.kmp.compose_face.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sd.lib.kmp.compose_face.FaceImage

@Composable
expect fun FaceImageView(
  modifier: Modifier = Modifier,
  faceImage: FaceImage,
)
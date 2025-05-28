package com.sd.demo.kmp.compose_face

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.sd.lib.kmp.compose_face.FaceImage
import com.sd.lib.kmp.compose_face.FaceImageWithBitmap

@Composable
actual fun FaceImageView(
  modifier: Modifier,
  faceImage: FaceImage,
) {
  val image = remember(faceImage) { (faceImage as FaceImageWithBitmap).src.asImageBitmap() }
  Image(
    modifier = modifier,
    bitmap = image,
    contentDescription = null,
    contentScale = ContentScale.FillWidth,
  )
}
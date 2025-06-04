package com.sd.demo.kmp.compose_face.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.sd.lib.kmp.face.FaceImage
import com.sd.lib.kmp.face.FaceImageWithUIImage
import platform.UIKit.UIImageView

@Composable
actual fun FaceImageView(
  modifier: Modifier,
  faceImage: FaceImage,
) {
  val image = remember(faceImage) { (faceImage as FaceImageWithUIImage).getUIImage() }
  UIKitView(
    modifier = modifier,
    factory = { UIImageView() },
    update = { it.image = image },
  )
}
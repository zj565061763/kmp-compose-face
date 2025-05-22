package com.sd.lib.kmp.compose_face

import android.graphics.Bitmap

class BitmapFaceImage(
  val bitmap: Bitmap?,
) : FaceImage {
  override fun release() {
    bitmap?.recycle()
  }
}
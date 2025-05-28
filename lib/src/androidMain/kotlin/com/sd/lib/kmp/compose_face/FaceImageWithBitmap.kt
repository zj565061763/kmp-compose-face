package com.sd.lib.kmp.compose_face

import android.graphics.Bitmap

class FaceImageWithBitmap internal constructor(
  val src: Bitmap,
) : FaceImage {
  override fun close() {
    src.recycle()
  }
}
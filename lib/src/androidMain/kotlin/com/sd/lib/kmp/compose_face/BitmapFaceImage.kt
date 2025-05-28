package com.sd.lib.kmp.compose_face

import android.graphics.Bitmap

class BitmapFaceImage internal constructor(
  val src: Bitmap,
) : FaceImage {
  override fun close() {
    src.recycle()
  }
}
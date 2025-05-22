package com.sd.lib.kmp.compose_face

import android.graphics.Bitmap

class BitmapFaceImage(
  val src: Bitmap,
  val crop: Bitmap?,
) : FaceImage {
  override fun release() {
    src.recycle()
    crop?.recycle()
  }
}
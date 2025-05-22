package com.sd.lib.kmp.compose_face

data class FaceBounds(
  val srcWidth: Int,
  val srcHeight: Int,
  val faceWidth: Int,
  val faceHeight: Int,
) {
  val faceWidthScale: Float get() = (if (srcWidth > 0) faceWidth / srcWidth.toFloat() else 0f).coerceIn(0f, 1f)
}

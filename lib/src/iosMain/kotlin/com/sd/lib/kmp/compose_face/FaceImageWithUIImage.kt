package com.sd.lib.kmp.compose_face

class FaceImageWithUIImage internal constructor(
  private val imageData: BGRImageData,
) : FaceImage {

  override fun close() = Unit
}
package com.sd.lib.kmp.compose_face

class FaceImageWithUIImage internal constructor(
  private val imageDataHolder: HFImageBitmapDataHolder,
) : FaceImage {

  override fun close() {
    imageDataHolder.close()
  }
}
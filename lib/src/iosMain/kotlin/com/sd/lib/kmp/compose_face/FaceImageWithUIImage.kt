package com.sd.lib.kmp.compose_face

import InspireFace.HFImageData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.set
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageRelease
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class)
class FaceImageWithUIImage internal constructor(
  private var imageData: HFImageData?,
) : FaceImage {

  private var _uiImage: UIImage? = null

  fun getUIImage(): UIImage? {
    return _uiImage
  }

  override fun init() {
    _uiImage = imageData?.toUIImage()
    close()
  }

  override fun close() {
    imageData?.also {
      imageData = null
      nativeHeap.free(it.rawPtr)
    }
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun HFImageData.toUIImage(): UIImage? {
  val data = data ?: return null
  return memScoped {
    val pixelCount = width * height
    val rgba = allocArray<UByteVar>(pixelCount * 4)
    var m = 0
    var n = 0
    repeat(pixelCount) {
      val b = data[n++]
      val g = data[n++]
      val r = data[n++]
      val a = data[n++]
      rgba[m++] = r
      rgba[m++] = g
      rgba[m++] = b
      rgba[m++] = a
    }

    val colorSpace = CGColorSpaceCreateDeviceRGB() ?: return null

    val context = CGBitmapContextCreate(
      data = rgba,
      width = width.toULong(),
      height = height.toULong(),
      bitsPerComponent = 8.toULong(),
      bytesPerRow = (4 * width).toULong(),
      space = colorSpace,
      bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
    )
    if (context == null) {
      CGColorSpaceRelease(colorSpace)
      return null
    }

    val image = CGBitmapContextCreateImage(context)
    if (image == null) {
      CGContextRelease(context)
      CGColorSpaceRelease(colorSpace)
      return null
    }

    UIImage.imageWithCGImage(image).also {
      CGImageRelease(image)
      CGContextRelease(context)
      CGColorSpaceRelease(colorSpace)
    }
  }
}
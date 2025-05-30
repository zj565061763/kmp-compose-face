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
import platform.posix.size_t

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
      it.data?.also { nativeHeap.free(it.rawValue) }
      nativeHeap.free(it.rawPtr)
    }
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun HFImageData.toUIImage(): UIImage? {
  val data = data ?: return null
  return memScoped {
    val pixelCount = width * height
    val rgbaSize = pixelCount * 4
    val rgba = allocArray<UByteVar>(rgbaSize)

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

    val bitsPerComponent: size_t = 8uL
    val bytesPerRow: size_t = (4 * width).toULong()

    val colorSpace = CGColorSpaceCreateDeviceRGB()
    val bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast

    val context = CGBitmapContextCreate(
      data = rgba,
      width = width.toULong(),
      height = height.toULong(),
      bitsPerComponent = bitsPerComponent,
      bytesPerRow = bytesPerRow,
      space = colorSpace,
      bitmapInfo = bitmapInfo.value,
    )

    val cgImage = CGBitmapContextCreateImage(context) ?: return null
    val uiImage = UIImage.imageWithCGImage(cgImage)

    CGContextRelease(context)
    CGImageRelease(cgImage)
    CGColorSpaceRelease(colorSpace)

    uiImage
  }
}
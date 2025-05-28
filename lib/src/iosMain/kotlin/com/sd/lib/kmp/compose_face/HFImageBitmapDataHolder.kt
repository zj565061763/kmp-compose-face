package com.sd.lib.kmp.compose_face

import InspireFace.HFImageBitmapData
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.CVPixelBufferGetBaseAddress
import platform.CoreVideo.CVPixelBufferGetHeight
import platform.CoreVideo.CVPixelBufferGetWidth
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress

@OptIn(ExperimentalForeignApi::class)
class HFImageBitmapDataHolder(
  private val buffer: CMSampleBufferRef,
) : AutoCloseable {
  private var _bgr: CArrayPointer<UByteVar>? = null
  private var _data: HFImageBitmapData? = null

  fun get(): HFImageBitmapData? = _data

  private fun init() {
    val imageBuffer = CMSampleBufferGetImageBuffer(buffer) ?: return
    try {
      CVPixelBufferLockBaseAddress(imageBuffer, 0.toULong())
      val baseAddress = CVPixelBufferGetBaseAddress(imageBuffer)?.reinterpret<UByteVar>() ?: return

      val width = CVPixelBufferGetWidth(imageBuffer).toInt()
      val height = CVPixelBufferGetHeight(imageBuffer).toInt()

      val pixelCount = width * height
      val bgrSize = pixelCount * 3
      val bgr = nativeHeap.allocArray<UByteVar>(bgrSize).also { _bgr = it }

      var m = 0
      var n = 0
      repeat(pixelCount) {
        bgr[m++] = baseAddress[n++] // B
        bgr[m++] = baseAddress[n++] // G
        bgr[m++] = baseAddress[n++] // R
        n++ // skip Alpha
      }

      _data = nativeHeap.alloc<HFImageBitmapData>().apply {
        this.channels = 3
        this.width = width
        this.height = height
        this.data = bgr
      }
    } finally {
      CVPixelBufferUnlockBaseAddress(imageBuffer, 0.toULong())
    }
  }

  init {
    init()
  }

  override fun close() {
    _bgr?.also {
      _bgr = null
      nativeHeap.free(it.rawValue)
    }
    _data?.also {
      _data = null
      nativeHeap.free(it.rawPtr)
    }
  }
}
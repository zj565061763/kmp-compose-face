package com.sd.lib.kmp.compose_face

internal class BGRImageData(
  val data: UByteArray,
  val width: Int,
  val height: Int,
) {
  val channels: Int = 3
}
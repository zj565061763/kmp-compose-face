package com.sd.lib.kmp.compose_face

import InspireFace.HFFaceComparison
import InspireFace.HFFaceFeature
import InspireFace.HSUCCEED
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.FloatVarOf
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.set
import kotlinx.cinterop.value

@OptIn(ExperimentalForeignApi::class)
actual fun faceCompare(a: FloatArray, b: FloatArray): Float {
  if (a.isEmpty() || b.isEmpty()) return 0f
  return memScoped {
    val fa = floatArrayToHFFaceFeature(a)
    val fb = floatArrayToHFFaceFeature(b)

    val similarityPtr = alloc<FloatVarOf<Float>>()
    val ret = HFFaceComparison(fa.readValue(), fb.readValue(), similarityPtr.ptr).toInt()

    if (ret == HSUCCEED) {
      similarityPtr.value
    } else {
      FaceManager.log { "faceCompare HFFaceComparison failed $ret" }
      0f
    }
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun MemScope.floatArrayToHFFaceFeature(array: FloatArray): HFFaceFeature {
  val dataPtr = allocArray<FloatVar>(array.size)
  for (i in array.indices) {
    dataPtr[i] = array[i]
  }
  return alloc<HFFaceFeature>().apply {
    this.data = dataPtr
    this.size = array.size
  }
}
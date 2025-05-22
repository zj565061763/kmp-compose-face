package com.sd.lib.kmp.compose_face

import com.insightface.sdk.inspireface.InspireFace
import com.insightface.sdk.inspireface.base.FaceFeature

internal actual fun faceCompare(a: FloatArray, b: FloatArray): Float {
  val fa = FaceFeature().apply { this.data = a }
  val fb = FaceFeature().apply { this.data = b }
  return InspireFace.FaceComparison(fa, fb)
}
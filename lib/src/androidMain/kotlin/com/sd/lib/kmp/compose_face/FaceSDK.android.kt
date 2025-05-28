package com.sd.lib.kmp.compose_face

import com.insightface.sdk.inspireface.InspireFace
import com.insightface.sdk.inspireface.base.FaceFeature

actual fun faceCompare(a: FloatArray, b: FloatArray): Float {
  if (a.isEmpty() || b.isEmpty()) return 0f
  val fa = FaceFeature().apply { this.data = a }
  val fb = FaceFeature().apply { this.data = b }
  return InspireFace.FaceComparison(fa, fb)
}
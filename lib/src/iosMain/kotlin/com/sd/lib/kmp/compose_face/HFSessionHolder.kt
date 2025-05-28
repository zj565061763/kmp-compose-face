package com.sd.lib.kmp.compose_face

import InspireFace.HFCreateInspireFaceSessionOptional
import InspireFace.HFDetectMode
import InspireFace.HFReleaseInspireFaceSession
import InspireFace.HFSession
import InspireFace.HFSessionSetFaceDetectThreshold
import InspireFace.HFSessionSetFilterMinimumFacePixelSize
import InspireFace.HFSessionSetTrackPreviewSize
import InspireFace.HF_ENABLE_FACE_RECOGNITION
import InspireFace.HF_ENABLE_INTERACTION
import InspireFace.HF_ENABLE_LIVENESS
import InspireFace.HF_ENABLE_QUALITY
import InspireFace.HSUCCEED
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value

@OptIn(ExperimentalForeignApi::class)
internal class HFSessionHolder {
  private var _sessionPtr: CPointerVarOf<HFSession>? = null
  private var _session: HFSession? = null

  fun get(): HFSession? = _session

  private fun init() {
    val sessionPtr = nativeHeap.alloc<CPointerVarOf<HFSession>>().also { _sessionPtr = it }
    HFCreateInspireFaceSessionOptional(
      customOption = HF_ENABLE_FACE_RECOGNITION or HF_ENABLE_QUALITY or HF_ENABLE_INTERACTION or HF_ENABLE_LIVENESS,
      detectMode = HFDetectMode.HF_DETECT_MODE_LIGHT_TRACK,
      maxDetectFaceNum = 2,
      detectPixelLevel = -1,
      trackByDetectModeFPS = -1,
      handle = sessionPtr.ptr,
    ).toInt().also { ret ->
      if (ret != HSUCCEED) {
        FaceManager.log { "HFSessionHolder init HFCreateInspireFaceSessionOptional failed ret:$ret" }
        close()
        return
      }
    }

    val session = sessionPtr.value.also { _session = it }
    if (session == null) {
      FaceManager.log { "HFSessionHolder init session is null" }
      close()
      return
    }

    HFSessionSetTrackPreviewSize(session, 320)
    HFSessionSetFaceDetectThreshold(session, 0.5f)
    HFSessionSetFilterMinimumFacePixelSize(session, 0)
  }

  fun close() {
    _session?.also {
      _session = null
      val ret = HFReleaseInspireFaceSession(it).toInt()
      if (ret != HSUCCEED) FaceManager.log { "HFSessionHolder HFReleaseInspireFaceSession failed ret:$ret" }
    }
    _sessionPtr?.also {
      _sessionPtr = null
      runCatching { nativeHeap.free(it.rawPtr) }
        .onFailure { e -> FaceManager.log { "HFSessionHolder free _sessionPtr error $e" } }
    }
  }

  init {
    init()
  }
}
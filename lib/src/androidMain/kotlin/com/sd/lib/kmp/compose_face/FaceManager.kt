package com.sd.lib.kmp.compose_face

import android.content.Context
import android.util.Log
import com.insightface.sdk.inspireface.InspireFace

object FaceManager {
  private var _hasInit = false

  fun init(context: Context) {
    if (_hasInit) return
    _hasInit = InspireFace.GlobalLaunch(context.applicationContext, InspireFace.PIKACHU)
    log { "init:$_hasInit" }
  }

  fun compare(a: FloatArray, b: FloatArray): Float {
    return faceCompare(a, b)
  }

  internal inline fun log(block: () -> String) {
    val msg = block()
    if (msg.isNotEmpty()) {
      Log.i("kmp-compose-face", msg)
    }
  }
}
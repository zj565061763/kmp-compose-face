package com.sd.lib.kmp.compose_face

import android.content.Context
import android.util.Log
import com.insightface.sdk.inspireface.InspireFace
import com.sd.lib.kmp.compose_face.FaceManager.init

object FaceManager {
  private var _hasInit = false

  /** 初始化 */
  @JvmStatic
  fun init(context: Context) {
    if (_hasInit) return
    _hasInit = InspireFace.GlobalLaunch(context.applicationContext, InspireFace.PIKACHU)
    log { "init:$_hasInit" }
  }

  @JvmStatic
  fun compare(a: FloatArray, b: FloatArray): Float {
    return faceCompare(a, b)
  }

  /** 释放，释放后需要重新调用[init]初始化，才可以继续使用SDK */
  @JvmStatic
  fun release() {
    if (_hasInit) {
      _hasInit = false
      val release = InspireFace.GlobalTerminate()
      log { "release:$release" }
    }
  }

  internal inline fun log(block: () -> String) {
    val msg = block()
    if (msg.isNotEmpty()) {
      Log.i("kmp-compose-face", msg)
    }
  }
}
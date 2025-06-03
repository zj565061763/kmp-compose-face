package com.sd.lib.kmp.compose_face

import InspireFace.HFLaunchInspireFace
import InspireFace.HFTerminateInspireFace
import InspireFace.HSUCCEED
import com.sd.lib.kmp.compose_face.FaceManager.init
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle

@OptIn(ExperimentalForeignApi::class)
object FaceManager {
  private var _hasInit = false

  /** 初始化 */
  fun init(modelPath: String = "Pikachu") {
    if (_hasInit) return
    val path = NSBundle.mainBundle.pathForResource(name = modelPath, ofType = null)
    val ret = HFLaunchInspireFace(path).toInt()
    _hasInit = ret == HSUCCEED
    log { "init:$_hasInit" }
  }

  /** 释放，释放后需要重新调用[init]初始化，才可以继续使用SDK */
  fun release() {
    if (_hasInit) {
      _hasInit = false
      val ret = HFTerminateInspireFace().toInt()
      log { "release:$ret" }
    }
  }

  internal inline fun log(block: () -> String) {
    logMsg(block)
  }
}
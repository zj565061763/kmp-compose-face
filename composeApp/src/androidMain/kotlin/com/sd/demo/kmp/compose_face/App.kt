package com.sd.demo.kmp.compose_face

import android.app.Application
import com.sd.lib.kmp.face.FaceManager

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    FaceManager.init(this)
  }
}
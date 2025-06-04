package com.sd.demo.kmp.compose_face

import android.app.Application

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    FaceManager.init(this)
  }
}
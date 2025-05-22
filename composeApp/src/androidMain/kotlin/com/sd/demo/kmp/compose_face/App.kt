package com.sd.demo.kmp.compose_face

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import com.sd.lib.kmp.compose_face.FaceManager
import java.io.File

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    context = this
    FaceManager.init(this)
  }

  companion object {
    private lateinit var context: Context
    private val dataFile: File get() = context.filesDir.resolve("face.json")

    /** 保存脸部数据 */
    fun saveFaceData(data: FloatArray) {
      val text = data.joinToString(separator = ",")
      runCatching { dataFile.writeText(text) }
        .onSuccess { logMsg { "saveFaceData onSuccess" } }
        .onFailure { logMsg { "saveFaceData onFailure $it" } }
    }

    /** 获取保存的脸部数据 */
    fun getSavedFaceData(): FloatArray {
      val text = runCatching { dataFile.readText() }.getOrElse { "" }
      val items = text.split(",")

      val size = items.size
      if (size <= 1) return FloatArray(0)

      val ret = FloatArray(size)
      repeat(size) { index ->
        ret[index] = items[index].toFloatOrNull() ?: 0f
      }
      return ret
    }

    /** 保存图片 */
    fun saveBitmap(bitmap: Bitmap, name: String) {
      runCatching {
        context.filesDir.resolve("${name}.jpg").outputStream().use {
          bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
      }.onSuccess { logMsg { "saveBitmap onSuccess $name" } }
        .onFailure { logMsg { "saveFaceData onFailure $name $it" } }
    }
  }
}
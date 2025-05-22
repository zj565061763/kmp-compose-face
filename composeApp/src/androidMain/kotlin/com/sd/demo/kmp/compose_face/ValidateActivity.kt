package com.sd.demo.kmp.compose_face

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.sd.lib.kmp.compose_face.BitmapFaceImage
import com.sd.lib.kmp.compose_face.FaceInteractionType
import com.sd.lib.kmp.compose_face.FaceManager
import com.sd.lib.kmp.compose_face.FaceResult
import com.sd.lib.kmp.compose_face.FaceViewModel

class ValidateActivity : ComponentActivity() {
  private val _vm = FaceViewModel(
    coroutineScope = lifecycleScope,
    listInteractionType = listOf(FaceInteractionType.entries.random()),
    onSuccess = { handleFaceResult(it) },
  )

  private lateinit var _savedData: FloatArray
  private var _faceImage by mutableStateOf<ImageBitmap?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val savedData = App.getSavedFaceData().also { _savedData = it }
    if (savedData.isEmpty()) {
      Toast.makeText(this, "请先录入人脸", Toast.LENGTH_SHORT).show()
      finish()
      return
    }
    setContent {
      AppTheme {
        Content()
      }
    }
  }

  @Composable
  private fun Content() {
    val faceImage = _faceImage
    if (faceImage == null) {
      ValidateContent()
    } else {
      SuccessContent(faceImage = faceImage)
    }
  }

  @Composable
  private fun ValidateContent() {
    FacePageView(
      vm = _vm,
      onClickExit = { finish() },
    )
  }

  @Composable
  private fun SuccessContent(
    faceImage: ImageBitmap,
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.height(64.dp))
      Image(
        modifier = Modifier.fillMaxWidth(),
        bitmap = faceImage,
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
      )
      Button(onClick = {
        _faceImage = null
        _vm.restart()
      }) {
        Text(text = "重新验证")
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    _vm.finish()
  }

  private fun handleFaceResult(result: FaceResult) {
    val bitmap = (result.image as BitmapFaceImage).bitmap
    _faceImage = bitmap?.asImageBitmap()
    if (bitmap != null) {
      val savedFace = _savedData
      val resultFace = result.data
      val similarity = FaceManager.compare(savedFace, resultFace)
      logMsg { "similarity:${similarity}" }
      if (similarity > 0.8f) {
        Toast.makeText(this, "验证成功", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(this, "验证失败", Toast.LENGTH_SHORT).show()
      }
    } else {
      Toast.makeText(this, "获取图片失败", Toast.LENGTH_SHORT).show()
    }
  }

  companion object {
    fun start(activity: Activity) {
      val intent = Intent(activity, ValidateActivity::class.java)
      activity.startActivity(intent)
    }
  }
}
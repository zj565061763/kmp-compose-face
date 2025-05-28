package com.sd.demo.kmp.compose_face

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sd.demo.kmp.compose_face.platform.FaceImageView
import com.sd.lib.kmp.compose_face.FaceImage
import com.sd.lib.kmp.compose_face.FaceInteractionType
import com.sd.lib.kmp.compose_face.FaceViewModel
import com.sd.lib.kmp.compose_face.faceCompare
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 验证
 */
@Composable
fun SampleValidate(
  onClickBack: () -> Unit,
) {
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  var showSnackbarJob by remember { mutableStateOf<Job?>(null) }

  var faceImage by remember { mutableStateOf<FaceImage?>(null) }

  val vm = remember {
    FaceViewModel(
      coroutineScope = coroutineScope,
      getInteractionTypes = { listOf(FaceInteractionType.entries.random()) },
      onSuccess = { result ->
        faceImage = result.image

        val savedFace = ComposeApp.faceData
        val resultFace = result.data
        val similarity = faceCompare(savedFace, resultFace)
        logMsg { "similarity:${similarity}" }

        showSnackbarJob?.cancel()
        showSnackbarJob = coroutineScope.launch {
          val text = if (similarity > 0.8f) "验证成功" else "验证失败"
          snackbarHostState.showSnackbar(text)
        }
      },
    )
  }

  RouteScaffold(
    title = "验证",
    onClickBack = onClickBack,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
  ) {
    val image = faceImage
    if (image != null) {
      SuccessContent(
        faceImage = image,
        onClickValidate = {
          showSnackbarJob?.cancel()
          faceImage = null
          vm.restart()
        },
      )
    } else {
      FacePageView(
        modifier = Modifier.fillMaxSize(),
        vm = vm,
        onClickExit = onClickBack,
      )
    }
  }
}

@Composable
private fun SuccessContent(
  modifier: Modifier = Modifier,
  faceImage: FaceImage,
  onClickValidate: () -> Unit,
) {
  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    FaceImageView(
      modifier = Modifier.fillMaxWidth(),
      faceImage = faceImage,
    )
    Button(onClick = onClickValidate) {
      Text(text = "重新验证")
    }
  }
}
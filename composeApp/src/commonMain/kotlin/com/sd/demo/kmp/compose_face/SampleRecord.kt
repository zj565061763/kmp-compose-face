package com.sd.demo.kmp.compose_face

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.demo.kmp.compose_face.platform.FaceImageView
import com.sd.lib.kmp.compose_face.FaceViewModel
import com.sd.lib.kmp.face.FaceImage

/**
 * 录入
 */
@Composable
fun SampleRecord(
  onClickBack: () -> Unit,
) {
  val coroutineScope = rememberCoroutineScope()
  var faceImage by remember { mutableStateOf<FaceImage?>(null) }

  val vm = remember { FaceViewModel(coroutineScope = coroutineScope) }
  val state by vm.stateFlow.collectAsStateWithLifecycle()

  val stage = state.stage
  if (stage is FaceViewModel.StageFinished.Success) {
    LaunchedEffect(Unit) {
      val result = stage.result
      faceImage = result.image
      ComposeApp.faceData = result.data
      logMsg { "onSuccess size:${result.data} data:${result.data.joinToString()}" }
    }
  }

  DisposableEffect(vm) {
    onDispose { vm.finish() }
  }

  RouteScaffold(
    title = "录入",
    onClickBack = onClickBack,
  ) {
    val image = faceImage
    if (image != null) {
      SuccessContent(
        faceImage = image,
        onClickRecord = {
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
  onClickRecord: () -> Unit,
) {
  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    FaceImageView(
      modifier = Modifier.fillMaxWidth().aspectRatio(1f),
      faceImage = faceImage,
    )
    Button(onClick = onClickRecord) {
      Text(text = "重新录制")
    }
  }
}
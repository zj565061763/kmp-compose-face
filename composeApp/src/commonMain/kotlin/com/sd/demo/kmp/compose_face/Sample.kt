package com.sd.demo.kmp.compose_face

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.sd.lib.kmp.compose_face.FaceInteractionType
import com.sd.lib.kmp.compose_face.FaceViewModel

@Composable
fun Sample(
  onClickBack: () -> Unit,
) {
  val coroutineScope = rememberCoroutineScope()
  val vm = remember {
    FaceViewModel(
      coroutineScope = coroutineScope,
      getInteractionTypes = { FaceInteractionType.entries },
      onSuccess = { result ->
        println("onSuccess size:${result.data}")
      },
    )
  }

  RouteScaffold(
    title = "Sample",
    onClickBack = onClickBack,
  ) {
    FacePageView(
      modifier = Modifier.fillMaxSize(),
      vm = vm,
      onClickExit = onClickBack,
    )
  }
}
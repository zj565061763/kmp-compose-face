package com.sd.demo.kmp.compose_face

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.lib.kmp.compose_face.FaceInteractionStage
import com.sd.lib.kmp.compose_face.FaceInteractionType
import com.sd.lib.kmp.compose_face.FaceView
import com.sd.lib.kmp.compose_face.FaceViewModel

@Composable
fun FacePageView(
  modifier: Modifier = Modifier,
  vm: FaceViewModel,
  onClickExit: () -> Unit,
) {
  val state by vm.stateFlow.collectAsStateWithLifecycle()
  if (state.isFinishedWithTimeout) {
    AlertDialog(
      onDismissRequest = {},
      text = {
        Text(text = "超时")
      },
      confirmButton = {
        TextButton(onClick = { vm.restart() }) {
          Text(text = "再试一次")
        }
      },
      dismissButton = {
        TextButton(onClick = onClickExit) {
          Text(text = "退出")
        }
      },
    )
  }

  val invalidType by vm.invalidTypeFlow.collectAsStateWithLifecycle(null)
  val invalidTips = faceInvalidTips(invalidType = invalidType)

  val tips = when (val stage = state.stage) {
    is FaceViewModel.Stage.Preparing -> invalidTips
    is FaceViewModel.Stage.Interacting -> invalidTips.ifEmpty {
      faceInteractingTips(
        interactionType = stage.interactionType,
        interactionStage = stage.interactionStage,
      )
    }
    else -> ""
  }

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(Modifier.height(64.dp))
    Text(
      text = tips.ifEmpty { " " },
      color = MaterialTheme.colorScheme.onSurface,
      fontSize = 18.sp,
      fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(16.dp))
    FaceView(
      modifier = Modifier
        .size(160.dp)
//        .clip(CircleShape)
        .background(Color.Red),
      vm = vm,
    )
  }
}

private fun faceInvalidTips(invalidType: FaceViewModel.InvalidType?): String {
  return when (invalidType) {
    FaceViewModel.InvalidType.NoFace -> "未检测到人脸"
    FaceViewModel.InvalidType.MultiFace -> "检测到多张人脸"
    FaceViewModel.InvalidType.SmallFace -> "请将脸部靠近一点"
    FaceViewModel.InvalidType.LowFaceQuality -> "请保持正脸，五官清晰可见"
    FaceViewModel.InvalidType.FaceInteraction -> "请保持正脸，五官自然"
    null -> ""
  }
}

private fun faceInteractingTips(
  interactionType: FaceInteractionType,
  interactionStage: FaceInteractionStage,
): String {
  return when (interactionStage) {
    FaceInteractionStage.Start -> when (interactionType) {
      FaceInteractionType.Blink -> "请缓慢眨眼"
      FaceInteractionType.Shake -> "请缓慢摇头"
      FaceInteractionType.MouthOpen -> "请缓慢张嘴"
      FaceInteractionType.RaiseHead -> "请缓慢抬头"
    }
    FaceInteractionStage.Stop -> "请保持正脸"
  }
}
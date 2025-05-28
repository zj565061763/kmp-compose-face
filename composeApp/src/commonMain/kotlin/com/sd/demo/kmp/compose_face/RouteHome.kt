package com.sd.demo.kmp.compose_face

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sd.demo.kmp.compose_face.platform.rememberPermissionUtils
import kotlinx.coroutines.launch

@Composable
fun RouteHome(
  onClickRecord: () -> Unit,
  onClickValidate: () -> Unit,
) {
  val permissionUtils = rememberPermissionUtils()
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  Scaffold(
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (permissionUtils.hasPermissions) {
        Button(onClick = onClickRecord) {
          Text(text = "录入")
        }
        Button(onClick = {
          if (ComposeApp.faceData.isEmpty()) {
            coroutineScope.launch {
              snackbarHostState.showSnackbar("请先录入人脸")
            }
          } else {
            onClickValidate()
          }
        }) {
          Text(text = "验证")
        }
      } else {
        Button(onClick = { permissionUtils.requestPermissions() }) {
          Text(text = "申请权限")
        }
      }
    }
  }
}
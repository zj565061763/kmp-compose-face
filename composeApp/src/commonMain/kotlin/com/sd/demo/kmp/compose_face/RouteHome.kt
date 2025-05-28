package com.sd.demo.kmp.compose_face

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sd.demo.kmp.compose_face.platform.rememberPermissionUtils

@Composable
fun RouteHome(
  onClickRecord: () -> Unit,
  onClickValidate: () -> Unit,
) {
  val permissionUtils = rememberPermissionUtils()
  Scaffold { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (permissionUtils.hasPermissions) {
        Button(onClick = onClickRecord) {
          Text(text = "录入")
        }
        Button(onClick = onClickValidate) {
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
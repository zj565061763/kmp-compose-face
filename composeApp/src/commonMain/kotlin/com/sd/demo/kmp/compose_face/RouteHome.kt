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

@Composable
fun RouteHome(
  onClickSample: () -> Unit,
) {
  Scaffold { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Button(onClick = onClickSample) { Text(text = "Sample") }
    }
  }
}
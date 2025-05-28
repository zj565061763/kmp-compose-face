package com.sd.demo.kmp.compose_face

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScaffold(
  modifier: Modifier = Modifier,
  title: String,
  onClickBack: () -> Unit,
  isLight: Boolean = true,
  contentPadding: PaddingValues = PaddingValues(),
  content: @Composable ColumnScope.() -> Unit,
) {
  AppTheme(isLight = isLight) {
    Scaffold(
      modifier = modifier.fillMaxSize(),
      topBar = {
        TopAppBar(
          title = { Text(text = title) },
          navigationIcon = {
            IconButton(onClick = onClickBack) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
              )
            }
          },
        )
      },
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
      )
    }
  }
}
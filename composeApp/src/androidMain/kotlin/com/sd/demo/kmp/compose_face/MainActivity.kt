package com.sd.demo.kmp.compose_face

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
  private var _hasPermissions by mutableStateOf(false)
  private val _requestPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
      if (!isGranted) {
        Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          if (_hasPermissions) {
            Button(onClick = {
              RecordActivity.start(this@MainActivity)
            }) {
              Text(text = "录入")
            }

            Button(onClick = {
              ValidateActivity.start(this@MainActivity)
            }) {
              Text(text = "验证")
            }

//            Button(onClick = {
//
//            }) {
//              Text(text = "Test")
//            }
          } else {
            Button(onClick = {
              if (hasPermissions(this@MainActivity)) {
                _hasPermissions = true
              } else {
                _requestPermissionLauncher.launch(Manifest.permission.CAMERA)
              }
            }) {
              Text(text = "申请权限")
            }
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    _hasPermissions = hasPermissions(this)
  }

  companion object {
    private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
      ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
  }
}
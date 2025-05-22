package com.sd.demo.kmp.compose_face

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun ComposeApp() {
  MaterialTheme {
    val navController = rememberNavController()
    NavHost(
      navController = navController,
      startDestination = AppRoute.Home,
    ) {
      composable<AppRoute.Home> {
        RouteHome(
          onClickSample = { navController.navigate(AppRoute.Sample) },
        )
      }
      composable<AppRoute.Sample> {
        Sample(onClickBack = { navController.popBackStack() })
      }
    }
  }
}

fun logMsg(tag: String = "kmp-compose-face", block: () -> String) {
  println("$tag ${block()}")
}
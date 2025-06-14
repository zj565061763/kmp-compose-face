package com.sd.demo.kmp.compose_face

import kotlinx.serialization.Serializable

sealed interface AppRoute {
  @Serializable
  data object Home : AppRoute

  @Serializable
  data object SampleRecord : AppRoute

  @Serializable
  data object SampleValidate : AppRoute
}
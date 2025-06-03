package com.sd.lib.kmp.compose_face

internal inline fun logMsg(block: () -> String) {
  val msg = block()
  if (msg.isNotEmpty()) {
    println("kmp-compose-face $msg")
  }
}
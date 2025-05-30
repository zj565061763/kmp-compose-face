package com.sd.lib.kmp.compose_face

/** 脸部信息 */
sealed interface FaceInfo

/** 合法脸部信息 */
interface ValidFaceInfo : FaceInfo {
  /** 脸部数据 */
  val faceData: FloatArray
  /** 脸部状态 */
  val faceState: FaceState
  /** 脸部区域 */
  val faceBounds: FaceBounds
  /** 脸部图片 */
  fun getFaceImage(): FaceImage
  /** 释放资源 */
  fun close()
}

/** 非法脸部数量 */
data class InvalidFaceCountFaceInfo(val faceCount: Int) : FaceInfo

/** 获取脸部信息错误 */
data class ErrorGetFaceInfo(
  val code: Int = SDK_ERROR,
) : FaceInfo {
  companion object {
    const val SDK_ERROR = 1
    const val RUNTIME_ERROR = 2
  }
}

/** 脸部状态 */
data class FaceState(
  /** 脸部质量[0-1] */
  val faceQuality: Float,

  /** 是否眨眼 */
  val blink: Boolean,
  /** 是否摇头 */
  val shake: Boolean,
  /** 是否张嘴 */
  val mouthOpen: Boolean,
  /** 是否抬头 */
  val raiseHead: Boolean,
) {
  val hasInteraction: Boolean get() = blink || shake || mouthOpen || raiseHead

  companion object {
    val Empty = FaceState(
      faceQuality = 0f,
      blink = false,
      shake = false,
      mouthOpen = false,
      raiseHead = false,
    )
  }
}

/** 脸部区域 */
data class FaceBounds(
  /** 源图宽度 */
  val srcWidth: Int,
  /** 源图高度 */
  val srcHeight: Int,
  /** 脸部宽度 */
  val faceWidth: Int,
  /** 脸部高度 */
  val faceHeight: Int,
) {
  /** 脸部宽度占源图宽度的比例[0-1] */
  val faceWidthScale: Float get() = (if (srcWidth > 0) faceWidth / srcWidth.toFloat() else 0f).coerceIn(0f, 1f)

  companion object {
    val Empty = FaceBounds(
      srcWidth = 0,
      srcHeight = 0,
      faceWidth = 0,
      faceHeight = 0,
    )
  }
}
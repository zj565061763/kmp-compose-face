package com.sd.lib.kmp.compose_face

import com.sd.lib.kmp.face.FaceImage

class FaceResult(
  /** 脸部数据 */
  val data: FloatArray,
  /** 脸部图片 */
  val image: FaceImage,
)
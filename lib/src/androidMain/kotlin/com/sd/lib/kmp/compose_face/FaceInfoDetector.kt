package com.sd.lib.kmp.compose_face

import android.graphics.Bitmap
import com.insightface.sdk.inspireface.InspireFace
import com.insightface.sdk.inspireface.base.FaceBasicToken
import com.insightface.sdk.inspireface.base.ImageStream
import com.insightface.sdk.inspireface.base.Session

internal class FaceInfoDetector {
  private var _session: Session? = null
  private val _facePipelineParams = InspireFace.CreateCustomParameter()
    .enableFaceQuality(true)
    .enableLiveness(true)
    .enableInteractionLiveness(true)

  fun detect(bitmap: Bitmap): FaceInfo {
    val session = _session ?: newSession().also { _session = it }
    val stream = InspireFace.CreateImageStreamFromBitmap(bitmap, InspireFace.CAMERA_ROTATION_0)
    try {
      return detect(session, stream, bitmap)
    } catch (e: Throwable) {
      e.printStackTrace()
      return ErrorGetFaceInfo(code = ErrorGetFaceInfo.RUNTIME_ERROR)
    } finally {
      InspireFace.ReleaseImageStream(stream)
    }
  }

  fun release() {
    _session?.also {
      _session = null
      InspireFace.ReleaseSession(it)
    }
  }

  private fun detect(
    session: Session,
    stream: ImageStream,
    src: Bitmap,
  ): FaceInfo {
    val multipleFaceData = InspireFace.ExecuteFaceTrack(session, stream)
    if (multipleFaceData == null) return InvalidFaceCountFaceInfo(faceCount = 0)

    val faceCount = multipleFaceData.detectedNum.coerceAtLeast(0)
    if (faceCount != 1) return InvalidFaceCountFaceInfo(faceCount = faceCount)

    val token = multipleFaceData.tokens?.firstOrNull()
    if (token == null) {
      FaceManager.log { "token is null" }
      return ErrorGetFaceInfo()
    }

    val faceFeature = InspireFace.ExtractFaceFeature(session, stream, token)
    if (faceFeature == null) {
      FaceManager.log { "ExtractFaceFeature returns null" }
      return ErrorGetFaceInfo()
    }

    val faceData = faceFeature.data
    if (faceData == null || faceData.isEmpty()) {
      FaceManager.log { "FaceFeature.data is null or empty" }
      return ErrorGetFaceInfo()
    }

    val successPipeline = InspireFace.MultipleFacePipelineProcess(session, stream, multipleFaceData, _facePipelineParams)
    if (!successPipeline) {
      FaceManager.log { "MultipleFacePipelineProcess failed" }
      return ErrorGetFaceInfo()
    }

    val faceQualityConfidence = InspireFace.GetFaceQualityConfidence(session)
    val faceQuality = faceQualityConfidence.confidence[0]

    val faceInteractionState = InspireFace.GetFaceInteractionStateResult(session)
    val leftEyeOpen = faceInteractionState.leftEyeStatusConfidence[0]
    val rightEyeOpen = faceInteractionState.rightEyeStatusConfidence[0]

    val faceInteractionsActions = InspireFace.GetFaceInteractionActionsResult(session)
    val blink = faceInteractionsActions.blink[0]
    val shake = faceInteractionsActions.shake[0]
    val mouthOpen = faceInteractionsActions.jawOpen[0]
    val raiseHead = faceInteractionsActions.headRaise[0]

    FaceManager.log { "faceQuality${faceQuality}" }

    val faceState = FaceState(
      faceQuality = faceQuality,
      leftEyeOpen = leftEyeOpen > 0.9f,
      rightEyeOpen = rightEyeOpen > 0.9f,
      blink = blink > 0,
      shake = shake > 0,
      mouthOpen = mouthOpen > 0,
      raiseHead = raiseHead > 0,
    )

    return SDKFaceInfo(
      session = session,
      stream = stream,
      src = src,
      token = token,
      faceState = faceState,
      faceData = faceData,
    )
  }

  private fun newSession(): Session {
    val parameter = InspireFace.CreateCustomParameter()
      .enableRecognition(true)
      .enableFaceQuality(true)
      .enableInteractionLiveness(true)
      .enableLiveness(true)
    return InspireFace.CreateSession(parameter, InspireFace.DETECT_MODE_LIGHT_TRACK, 2, -1, -1)
      .also { session ->
        InspireFace.SetTrackPreviewSize(session, 320)
        InspireFace.SetFaceDetectThreshold(session, 0.5f)
        InspireFace.SetFilterMinimumFacePixelSize(session, 0)
      }
  }

  private class SDKFaceInfo(
    private val session: Session,
    private val stream: ImageStream,
    private val src: Bitmap,
    private val token: FaceBasicToken,
    override val faceState: FaceState,
    override val faceData: FloatArray,
  ) : ValidFaceInfo {
    override fun getFaceImage(): FaceImage {
      val crop = runCatching {
        InspireFace.GetFaceAlignmentImage(session, stream, token)
      }.getOrElse {
        FaceManager.log { "getFaceImage error:$it" }
        null
      }
      return BitmapFaceImage(crop = crop, src = src)
    }
  }
}
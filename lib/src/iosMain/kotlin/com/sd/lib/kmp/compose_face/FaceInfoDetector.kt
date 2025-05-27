package com.sd.lib.kmp.compose_face

import InspireFace.HFCreateImageBitmap
import InspireFace.HFCreateImageStreamFromImageBitmap
import InspireFace.HFCreateInspireFaceSessionOptional
import InspireFace.HFDetectMode
import InspireFace.HFExecuteFaceTrack
import InspireFace.HFFaceFeature
import InspireFace.HFFaceFeatureExtract
import InspireFace.HFFaceInteractionsActions
import InspireFace.HFFaceQualityConfidence
import InspireFace.HFGetFaceInteractionActionsResult
import InspireFace.HFGetFaceQualityConfidence
import InspireFace.HFGetFeatureLength
import InspireFace.HFImageBitmap
import InspireFace.HFImageBitmapData
import InspireFace.HFImageStream
import InspireFace.HFMultipleFaceData
import InspireFace.HFMultipleFacePipelineProcessOptional
import InspireFace.HFReleaseImageBitmap
import InspireFace.HFReleaseImageStream
import InspireFace.HFReleaseInspireFaceSession
import InspireFace.HFSession
import InspireFace.HFSessionSetFaceDetectThreshold
import InspireFace.HFSessionSetFilterMinimumFacePixelSize
import InspireFace.HFSessionSetTrackPreviewSize
import InspireFace.HF_CAMERA_ROTATION_0
import InspireFace.HF_ENABLE_FACE_RECOGNITION
import InspireFace.HF_ENABLE_INTERACTION
import InspireFace.HF_ENABLE_LIVENESS
import InspireFace.HF_ENABLE_QUALITY
import InspireFace.HSUCCEED
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVarOf
import kotlinx.cinterop.IntVarOf
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.value
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.CVPixelBufferGetBaseAddress
import platform.CoreVideo.CVPixelBufferGetHeight
import platform.CoreVideo.CVPixelBufferGetWidth
import platform.CoreVideo.CVPixelBufferLockBaseAddress

@OptIn(ExperimentalForeignApi::class)
class FaceInfoDetector {
  private var _session: HFSession? = null

  fun detect(buffer: CMSampleBufferRef): FaceInfo {
    val session = _session ?: newSession().also { _session = it }
    if (session == null) {
      FaceManager.log { "detect session is null" }
      return ErrorGetFaceInfo()
    }
    return memScoped { detect(buffer, session) }
  }

  /** 不需要检测时，释放 */
  fun release() {
    _session?.also {
      _session = null
      HFReleaseInspireFaceSession(it)
    }
  }

  private fun MemScope.detect(
    buffer: CMSampleBufferRef,
    session: HFSession,
  ): FaceInfo {
    // HFImageBitmapData
    val imageData = bufferToBitmapData(buffer)
    if (imageData == null) {
      FaceManager.log { "detect getHFImageBitmapData returns null" }
      return ErrorGetFaceInfo()
    }

    val srcWidth = imageData.width
    val srcHeight = imageData.height
    if (srcWidth <= 0 || srcHeight <= 0) {
      FaceManager.log { "detect src width or height <= 0" }
      return ErrorGetFaceInfo()
    }

    // HFImageBitmap
    val imageBitmap = alloc<CPointerVarOf<HFImageBitmap>>().ptr
    run {
      HFCreateImageBitmap(
        data = imageData.ptr,
        handle = imageBitmap,
      ).toInt().also { ret ->
        if (ret != HSUCCEED) {
          FaceManager.log { "detect HFCreateImageBitmap failed $ret" }
          return ErrorGetFaceInfo()
        }
      }
    }

    // HFImageStream
    val imageStream = alloc<CPointerVarOf<HFImageStream>>().ptr
    run {
      HFCreateImageStreamFromImageBitmap(
        handle = imageBitmap,
        rotation = HF_CAMERA_ROTATION_0,
        streamHandle = imageStream,
      ).toInt().also { ret ->
        if (ret != HSUCCEED) {
          FaceManager.log { "detect HFCreateImageStreamFromImageBitmap failed $ret" }
          return ErrorGetFaceInfo()
        }
      }
    }

    try {
      return detect(
        session = session,
        imageStream = imageStream,
        imageData = imageData,
      )
    } catch (e: Throwable) {
      e.printStackTrace()
      return ErrorGetFaceInfo(code = ErrorGetFaceInfo.RUNTIME_ERROR)
    } finally {
      HFReleaseImageBitmap(imageBitmap)
      HFReleaseImageStream(imageStream)
    }
  }

  private fun MemScope.detect(
    session: HFSession,
    imageStream: HFImageStream,
    imageData: HFImageBitmapData,
  ): FaceInfo {
    // HFMultipleFaceData
    val multipleFaceData = alloc<HFMultipleFaceData>()
    run {
      HFExecuteFaceTrack(
        session = session,
        streamHandle = imageStream,
        results = multipleFaceData.ptr,
      ).toInt().also { ret ->
        if (ret != HSUCCEED) {
          FaceManager.log { "detect HFExecuteFaceTrack failed $ret" }
          return ErrorGetFaceInfo()
        }
      }
    }

    val faceCount = multipleFaceData.detectedNum.coerceAtLeast(0)
    if (faceCount != 1) return InvalidFaceCountFaceInfo(faceCount = faceCount)

    val faceRect = multipleFaceData.rects?.get(0)
    if (faceRect == null) {
      FaceManager.log { "detect faceRect is null" }
      return ErrorGetFaceInfo()
    }

    val token = multipleFaceData.tokens?.get(0)
    if (token == null) {
      FaceManager.log { "detect token is null" }
      return ErrorGetFaceInfo()
    }

    // HFFaceFeature
    val faceFeature = alloc<HFFaceFeature>()
    run {
      HFFaceFeatureExtract(
        session = session,
        streamHandle = imageStream,
        singleFace = token.readValue(),
        feature = faceFeature.ptr,
      ).toInt().also { ret ->
        if (ret != HSUCCEED) {
          FaceManager.log { "detect HFFaceFeatureExtract failed $ret" }
          return ErrorGetFaceInfo()
        }
      }
    }

    // HFGetFeatureLength
    val featureLengthVar = alloc<IntVarOf<Int>>()
    run {
      val ret = HFGetFeatureLength(featureLengthVar.ptr).toInt()
      if (ret != HSUCCEED) {
        FaceManager.log { "detect HFGetFeatureLength failed $ret" }
        return ErrorGetFaceInfo()
      }
    }

    val featureLength = featureLengthVar.value
    if (featureLength <= 0) {
      FaceManager.log { "detect HFGetFeatureLength <= 0 $featureLength" }
      return ErrorGetFaceInfo()
    }

    val faceData = faceFeature.data.toFloatArray(featureLength)
    if (faceData == null || faceData.isEmpty()) {
      FaceManager.log { "detect FaceFeature.data is null or empty" }
      return ErrorGetFaceInfo()
    }

    // HFMultipleFacePipelineProcessOptional
    run {
      HFMultipleFacePipelineProcessOptional(
        session = session,
        streamHandle = imageStream,
        faces = multipleFaceData.ptr,
        customOption = HF_ENABLE_QUALITY or HF_ENABLE_LIVENESS or HF_ENABLE_INTERACTION,
      ).toInt().also { ret ->
        if (ret != HSUCCEED) {
          FaceManager.log { "detect HFMultipleFacePipelineProcessOptional failed $ret" }
          return ErrorGetFaceInfo()
        }
      }
    }

    // HFGetFaceQualityConfidence
    val faceQualityConfidence = alloc<HFFaceQualityConfidence>()
    run {
      HFGetFaceQualityConfidence(
        session = session,
        confidence = faceQualityConfidence.ptr,
      ).toInt().also { ret ->
        if (ret != HSUCCEED) {
          FaceManager.log { "detect HFGetFaceQualityConfidence failed $ret" }
          return ErrorGetFaceInfo()
        }
      }
    }

    val faceQuality = faceQualityConfidence.confidence.toFloatArray(1)?.firstOrNull()
    if (faceQuality == null) {
      FaceManager.log { "detect faceQualityConfidence is null" }
      return ErrorGetFaceInfo()
    }

    val actions = alloc<HFFaceInteractionsActions>()
    run {
      HFGetFaceInteractionActionsResult(
        session = session,
        actions = actions.ptr,
      ).toInt().also { ret ->
        if (ret != HSUCCEED) {
          FaceManager.log { "detect HFGetFaceInteractionActionsResult failed $ret" }
          return ErrorGetFaceInfo()
        }
      }
    }

    val blink = actions.blink?.get(0)
    if (blink == null) {
      FaceManager.log { "detect actions.blink is null" }
      return ErrorGetFaceInfo()
    }

    val shake = actions.shake?.get(0)
    if (shake == null) {
      FaceManager.log { "detect actions.shake is null" }
      return ErrorGetFaceInfo()
    }

    val jawOpen = actions.jawOpen?.get(0)
    if (jawOpen == null) {
      FaceManager.log { "detect actions.jawOpen is null" }
      return ErrorGetFaceInfo()
    }

    val headRaise = actions.headRaise?.get(0)
    if (headRaise == null) {
      FaceManager.log { "detect actions.headRaise is null" }
      return ErrorGetFaceInfo()
    }

    val faceState = FaceState(
      faceQuality = faceQuality,
      leftEyeOpen = true,
      rightEyeOpen = true,
      blink = blink > 0,
      shake = shake > 0,
      mouthOpen = jawOpen > 0,
      raiseHead = headRaise > 0,
    )

    val faceBounds = FaceBounds(
      srcWidth = imageData.width,
      srcHeight = imageData.height,
      faceWidth = faceRect.width,
      faceHeight = faceRect.height,
    )

    return SDKFaceInfo(
      faceData = faceData,
      faceState = faceState,
      faceBounds = faceBounds,
    )
  }

  private fun newSession(): HFSession? {
    val customOption = HF_ENABLE_FACE_RECOGNITION or HF_ENABLE_QUALITY or HF_ENABLE_INTERACTION or HF_ENABLE_LIVENESS
    val detectPixelLevel = 320
    val session = nativeHeap.alloc<CPointerVarOf<HFSession>>().ptr

    val ret = HFCreateInspireFaceSessionOptional(
      customOption = customOption,
      detectMode = HFDetectMode.HF_DETECT_MODE_LIGHT_TRACK,
      maxDetectFaceNum = 2,
      detectPixelLevel = detectPixelLevel,
      trackByDetectModeFPS = 16,
      handle = session,
    ).toInt()

    if (ret != HSUCCEED) {
      nativeHeap.free(session.rawValue)
      return null
    }

    HFSessionSetTrackPreviewSize(session, detectPixelLevel)
    HFSessionSetFilterMinimumFacePixelSize(session, 0)
    HFSessionSetFaceDetectThreshold(session, 0.5f)
    return session
  }

  private class SDKFaceInfo(
    override val faceData: FloatArray,
    override val faceState: FaceState,
    override val faceBounds: FaceBounds,
  ) : ValidFaceInfo {
    override fun getFaceImage(): FaceImage {
      return object : FaceImage {
        override fun release() {
          // TODO release
        }
      }
    }
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun MemScope.bufferToBitmapData(buffer: CMSampleBufferRef): HFImageBitmapData? {
  val imageBuffer = CMSampleBufferGetImageBuffer(buffer)
  if (imageBuffer == null) return null

  CVPixelBufferLockBaseAddress(imageBuffer, 0.convert())
  val baseAddress = CVPixelBufferGetBaseAddress(imageBuffer)?.reinterpret<UByteVar>() ?: return null

  val width = CVPixelBufferGetWidth(imageBuffer).toInt()
  val height = CVPixelBufferGetHeight(imageBuffer).toInt()

  val pixelCount = width * height
  val bgrSize = pixelCount * 3
  val bgr = allocArray<UByteVar>(bgrSize)

  var m = 0
  var n = 0
  repeat(pixelCount) {
    bgr[m++] = baseAddress[n++] // B
    bgr[m++] = baseAddress[n++] // G
    bgr[m++] = baseAddress[n++] // R
    n++ // skip Alpha
  }

  return alloc<HFImageBitmapData>().apply {
    this.channels = 3
    this.width = width
    this.height = height
    this.data = bgr
  }
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<FloatVarOf<Float>>?.toFloatArray(length: Int): FloatArray? {
  if (this == null) return null
  return FloatArray(length) { index -> this[index] }
}
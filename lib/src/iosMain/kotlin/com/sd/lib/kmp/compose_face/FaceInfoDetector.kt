package com.sd.lib.kmp.compose_face

import InspireFace.HFCreateImageStream
import InspireFace.HFExecuteFaceTrack
import InspireFace.HFFaceFeature
import InspireFace.HFFaceFeatureExtract
import InspireFace.HFFaceInteractionsActions
import InspireFace.HFFaceQualityConfidence
import InspireFace.HFGetFaceInteractionActionsResult
import InspireFace.HFGetFaceQualityConfidence
import InspireFace.HFGetFeatureLength
import InspireFace.HFImageData
import InspireFace.HFImageStream
import InspireFace.HFMultipleFaceData
import InspireFace.HFMultipleFacePipelineProcessOptional
import InspireFace.HFReleaseImageStream
import InspireFace.HFSession
import InspireFace.HF_CAMERA_ROTATION_0
import InspireFace.HF_ENABLE_INTERACTION
import InspireFace.HF_ENABLE_QUALITY
import InspireFace.HF_STREAM_BGRA
import InspireFace.HSUCCEED
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVarOf
import kotlinx.cinterop.IntVarOf
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.readValue
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.value
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.CVPixelBufferGetBaseAddress
import platform.CoreVideo.CVPixelBufferGetHeight
import platform.CoreVideo.CVPixelBufferGetWidth
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress
import kotlin.time.measureTimedValue

@OptIn(ExperimentalForeignApi::class)
internal class FaceInfoDetector {
  private var _sessionHolder: HFSessionHolder? = null

  fun detect(buffer: CMSampleBufferRef): FaceInfo {
    val session = (_sessionHolder ?: HFSessionHolder().also { _sessionHolder = it }).get()
    if (session == null) {
      FaceManager.log { "detect session is null" }
      release()
      return ErrorGetFaceInfo()
    }

    val (imageData, _) = measureTimedValue {
      sampleBufferToImageData(buffer)
    }.also {
      FaceManager.log { "detect sampleBufferToImageData time:${it.duration.inWholeMilliseconds}" }
    }

    if (imageData == null) {
      FaceManager.log { "detect sampleBufferToImageData returns null" }
      return ErrorGetFaceInfo()
    }

    return memScoped {
      detect(
        session = session,
        imageData = imageData,
      )
    }
  }

  /** 不需要检测时，释放 */
  fun release() {
    _sessionHolder?.also {
      FaceManager.log { "detect release" }
      _sessionHolder = null
      it.close()
    }
  }

  private fun MemScope.detect(
    session: HFSession,
    imageData: ImageData,
  ): FaceInfo {
    if (imageData.width <= 0 || imageData.height <= 0) {
      FaceManager.log { "detect src width or height <= 0" }
      return ErrorGetFaceInfo()
    }

    // HFImageStream
    val imageStream = run {
      val hfImageData = imageDataToHFImageData(imageData)
      val imageStreamPtr = alloc<CPointerVarOf<HFImageStream>>()
      HFCreateImageStream(
        data = hfImageData.ptr,
        handle = imageStreamPtr.ptr,
      ).toInt().also { ret ->
        if (ret != HSUCCEED) {
          FaceManager.log { "detect HFCreateImageStreamFromImageBitmap failed $ret" }
          return ErrorGetFaceInfo()
        }
      }
      imageStreamPtr.value
    } ?: return ErrorGetFaceInfo().also {
      FaceManager.log { "detect HFCreateImageStreamFromImageBitmap value is null" }
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
      HFReleaseImageStream(imageStream)
    }
  }

  private fun MemScope.detect(
    session: HFSession,
    imageStream: HFImageStream,
    imageData: ImageData,
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
    val featureLength = run {
      val featureLengthPtr = alloc<IntVarOf<Int>>()
      val ret = HFGetFeatureLength(featureLengthPtr.ptr).toInt()
      if (ret != HSUCCEED) {
        FaceManager.log { "detect HFGetFeatureLength failed $ret" }
        return ErrorGetFaceInfo()
      }
      featureLengthPtr.value
    }.also {
      if (it <= 0) {
        FaceManager.log { "detect HFGetFeatureLength <= 0 $it" }
        return ErrorGetFaceInfo()
      }
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
        customOption = HF_ENABLE_QUALITY or HF_ENABLE_INTERACTION,
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

    val faceQuality = faceQualityConfidence.confidence?.get(0)
    if (faceQuality == null) {
      FaceManager.log { "detect faceQuality is null" }
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

    FaceManager.log { "$faceState $faceBounds" }

    return SDKFaceInfo(
      faceData = faceData,
      faceState = faceState,
      faceBounds = faceBounds,
      imageData = imageData,
    )
  }

  private class SDKFaceInfo(
    override val faceData: FloatArray,
    override val faceState: FaceState,
    override val faceBounds: FaceBounds,
    private val imageData: ImageData,
  ) : ValidFaceInfo {
    override fun getFaceImage(): FaceImage {
      return FaceImageWithUIImage(imageData = imageData)
    }
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun sampleBufferToImageData(buffer: CMSampleBufferRef): ImageData? {
  val imageBuffer = CMSampleBufferGetImageBuffer(buffer) ?: return null
  try {
    CVPixelBufferLockBaseAddress(imageBuffer, 0.toULong())
    val baseAddress = CVPixelBufferGetBaseAddress(imageBuffer)?.reinterpret<UByteVar>() ?: return null

    val width = CVPixelBufferGetWidth(imageBuffer).toInt()
    val height = CVPixelBufferGetHeight(imageBuffer).toInt()

    val size = width * height * 4
    val data = baseAddress.readBytes(size).toUByteArray()

    return ImageData(
      width = width,
      height = height,
      data = data,
    )
  } catch (e: Throwable) {
    FaceManager.log { "sampleBufferToImageData error $e" }
    return null
  } finally {
    CVPixelBufferUnlockBaseAddress(imageBuffer, 0.toULong())
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun MemScope.imageDataToHFImageData(imageData: ImageData): HFImageData {
  return alloc<HFImageData>().apply {
    this.data = imageData.data.toCValues().ptr
    this.width = imageData.width
    this.height = imageData.height
    this.format = HF_STREAM_BGRA
    this.rotation = HF_CAMERA_ROTATION_0
  }
}

@OptIn(ExperimentalForeignApi::class)
fun CPointer<FloatVarOf<Float>>?.toFloatArray(length: Int): FloatArray? {
  if (this == null) return null
  return FloatArray(length) { index -> this[index] }
}
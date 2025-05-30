package com.sd.lib.kmp.compose_face

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.interpretObjCPointerOrNull
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPreset640x480
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoStabilizationModeStandard
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.Foundation.NSString
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
internal fun createAVCaptureSession(
  queue: NSObject?,
  delegate: AVCaptureVideoDataOutputSampleBufferDelegateProtocol,
): AVCaptureSession {
  return AVCaptureSession().apply {
    val input = createAVCaptureDeviceInput()
    addInput(input)
    setSessionPreset(AVCaptureSessionPreset640x480)

    val output = createAVCaptureVideoDataOutput()
    addOutput(output)

    output.connectionWithMediaType(AVMediaTypeVideo)!!.apply {
      videoOrientation = AVCaptureVideoOrientationPortrait
      setPreferredVideoStabilizationMode(AVCaptureVideoStabilizationModeStandard)
      if (supportsVideoMirroring) videoMirrored = true
    }

    output.setSampleBufferDelegate(delegate, queue)
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun createAVCaptureDeviceInput(): AVCaptureDeviceInput {
  val device = AVCaptureDevice.defaultDeviceWithDeviceType(
    deviceType = AVCaptureDeviceTypeBuiltInWideAngleCamera,
    mediaType = AVMediaTypeVideo,
    position = AVCaptureDevicePositionFront,
  )!!
  return AVCaptureDeviceInput.deviceInputWithDevice(
    device = device,
    error = null,
  )!!
}

@OptIn(ExperimentalForeignApi::class)
private fun createAVCaptureVideoDataOutput(): AVCaptureVideoDataOutput {
  return AVCaptureVideoDataOutput().apply {
    alwaysDiscardsLateVideoFrames = true
    val key = interpretObjCPointerOrNull<NSString>(kCVPixelBufferPixelFormatTypeKey!!.rawValue)
    val value = kCVPixelFormatType_32BGRA
    setVideoSettings(mapOf(key to value))
  }
}
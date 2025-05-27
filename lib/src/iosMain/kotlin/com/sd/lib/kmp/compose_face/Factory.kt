package com.sd.lib.kmp.compose_face

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPreset640x480
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoStabilizationModeStandard
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
internal fun createAVCaptureSession(
  onCMSampleBufferRef: (CMSampleBufferRef) -> Unit,
): AVCaptureSession? {
  val input = createAVCaptureDeviceInput() ?: return null
  val output = createAVCaptureVideoDataOutput(onCMSampleBufferRef = onCMSampleBufferRef)
  return AVCaptureSession().apply {
    sessionPreset = AVCaptureSessionPreset640x480
    addInput(input)
    addOutput(output)
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun createAVCaptureDeviceInput(): AVCaptureDeviceInput? {
  val device = AVCaptureDevice.defaultDeviceWithDeviceType(
    deviceType = AVCaptureDeviceTypeBuiltInWideAngleCamera,
    mediaType = AVMediaTypeVideo,
    position = AVCaptureDevicePositionFront,
  ) ?: return null

  return AVCaptureDeviceInput.deviceInputWithDevice(
    device = device,
    error = null,
  )
}

@OptIn(ExperimentalForeignApi::class)
private fun createAVCaptureVideoDataOutput(
  onCMSampleBufferRef: (CMSampleBufferRef) -> Unit,
): AVCaptureVideoDataOutput {
  val delegate = object : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
    override fun captureOutput(output: AVCaptureOutput, didDropSampleBuffer: CMSampleBufferRef?, fromConnection: AVCaptureConnection) {
      if (didDropSampleBuffer != null) {
        onCMSampleBufferRef(didDropSampleBuffer)
      }
    }
  }
  return AVCaptureVideoDataOutput().apply {
    alwaysDiscardsLateVideoFrames = true
    videoSettings = mapOf(
      kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_32BGRA
    )

    connectionWithMediaType(AVMediaTypeVideo)!!.apply {
      videoOrientation = AVCaptureVideoOrientationPortrait
      setPreferredVideoStabilizationMode(AVCaptureVideoStabilizationModeStandard)
      if (supportsVideoMirroring) videoMirrored = true
    }

    setSampleBufferDelegate(delegate, dispatch_get_main_queue())
  }
}
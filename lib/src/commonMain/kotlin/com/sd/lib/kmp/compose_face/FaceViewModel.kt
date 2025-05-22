package com.sd.lib.kmp.compose_face

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class FaceViewModel(
  private val coroutineScope: CoroutineScope,
  private val listInteractionType: List<FaceInteractionType> = emptyList(),
  private val timeout: Long = 15_000,
  private val onSuccess: (FaceResult) -> Unit,
) {
  private val _stateFlow = MutableStateFlow<State>(State())
  val stateFlow: StateFlow<State> = _stateFlow.asStateFlow()

  private val _currentState get() = stateFlow.value

  /** 通过检测的脸部数据 */
  private var _checkedFaceData: FloatArray? = null
  /** 通过检测到脸部图片 */
  private var _checkedFaceImage: FaceImage? = null

  private var _timeoutJob: Job? = null
  private var _staticJob: Job? = null

  /** 无效类型 */
  val invalidTypeFlow: Flow<InvalidType?> = _stateFlow
    .map { it.invalidType() }
    .distinctUntilChanged()
    .mapLatest { tips ->
      if (tips != null) delay(800)
      tips
    }.distinctUntilChanged()

  /** 处理脸部信息 */
  fun process(faceInfo: FaceInfo) {
    val stage = _currentState.stage
    if (stage is Stage.Finished) {
      return
    }

    if (stage is Stage.None) {
      updateState { it.copy(stage = Stage.Preparing()) }
      startStaticJob()
    }

    when (faceInfo) {
      is ErrorGetFaceInfo -> {}
      is InvalidFaceCountFaceInfo -> {
        updateState { it.copy(faceCount = faceInfo.faceCount) }
      }
      is ValidFaceInfo -> {
        updateState { it.copy(faceCount = 1, faceState = faceInfo.faceState) }
        handleValidFaceInfo(faceInfo)
      }
    }
  }

  /** 结束 */
  fun finish() {
    finishWithType(FinishType.Normal)
  }

  /** 结束后，重新开始 */
  fun restart() {
    if (_currentState.stage is Stage.Finished) {
      updateState { State() }
    }
  }

  private fun finishWithType(type: FinishType) {
    _timeoutJob?.cancel()
    _staticJob?.cancel()
    if (_currentState.stage !is Stage.Finished) {
      updateState { State(stage = Stage.Finished(type = type)) }
    }
    _checkedFaceData = null
    _checkedFaceImage = null
  }

  private fun handleValidFaceInfo(faceInfo: ValidFaceInfo) {
    when (val stage = _currentState.stage) {
      is Stage.Preparing -> handleStagePreparing(faceInfo = faceInfo, stage = stage)
      is Stage.Interacting -> handleStageInteracting(faceInfo = faceInfo, stage = stage)
      else -> {}
    }
  }

  /** 准备阶段 */
  private fun handleStagePreparing(
    faceInfo: ValidFaceInfo,
    stage: Stage.Preparing,
  ) {
    if (!faceInfo.faceState.checkFaceState()) {
      // 只要有一次检测不通过，重新计数
      updatePreparingStage { it.copy(checkCount = 0) }
      return
    }

    val checkCount = stage.checkCount
    if (checkCount < 10) {
      // 如果小于检测次数，继续检测
      updatePreparingStage { it.copy(checkCount = it.checkCount + 1) }
      return
    }

    val checkedFaceData = faceInfo.faceData
    val checkedFaceImage = faceInfo.getFaceImage()

    if (listInteractionType.isEmpty()) {
      notifySuccess(data = checkedFaceData, image = checkedFaceImage)
      return
    }

    // 保存通过检测的数据
    _checkedFaceData = checkedFaceData
    _checkedFaceImage = checkedFaceImage

    val listType = listInteractionType.shuffled()
    val firstType = listType.first()
    updateState {
      it.copy(
        stage = Stage.Interacting(
          listInteractionType = listType - firstType,
          interactionType = firstType,
          interactionStage = FaceInteractionStage.Start,
          interactionCount = 0,
        ),
      )
    }
  }

  /** 互动阶段 */
  private fun handleStageInteracting(
    faceInfo: ValidFaceInfo,
    stage: Stage.Interacting,
  ) {
    val checkedFaceData = _checkedFaceData
    val checkedFaceImage = _checkedFaceImage
    if (checkedFaceData == null || checkedFaceImage == null) {
      finishWithType(FinishType.InternalError)
      return
    }

    val faceState = faceInfo.faceState

    when (stage.interactionStage) {
      FaceInteractionStage.Start -> {
        if (faceState.faceQuality < MIN_FACE_QUALITY_INTERACTING) {
          return
        }
        val hasTargetInteraction = when (stage.interactionType) {
          FaceInteractionType.Blink -> faceState.blink
          FaceInteractionType.Shake -> faceState.shake
          FaceInteractionType.MouthOpen -> faceState.mouthOpen
          FaceInteractionType.RaiseHead -> faceState.raiseHead
        }
        if (hasTargetInteraction) {
          val targetInteractionCount = 3
          updateInteractingStage {
            val newCount = it.interactionCount + 1
            if (newCount >= targetInteractionCount) {
              it.copy(interactionCount = newCount, interactionStage = FaceInteractionStage.Stop)
            } else {
              it.copy(interactionCount = newCount)
            }
          }
        }
      }
      FaceInteractionStage.Stop -> {
        if (!faceState.checkFaceState()) {
          return
        }
        val similarity = faceCompare(checkedFaceData, faceInfo.faceData)
        if (similarity >= 0.8f) {
          val listInteractionType = stage.listInteractionType
          if (listInteractionType.isEmpty()) {
            notifySuccess(data = checkedFaceData, image = checkedFaceImage)
          } else {
            moveToNextInteractionType(listInteractionType)
          }
        }
      }
    }
  }

  private fun moveToNextInteractionType(listInteractionType: List<FaceInteractionType>) {
    val nextType = listInteractionType.first()
    val newList = listInteractionType - nextType
    updateInteractingStage {
      Stage.Interacting(
        listInteractionType = newList,
        interactionType = nextType,
        interactionStage = FaceInteractionStage.Start,
        interactionCount = 0,
      )
    }
  }

  private fun notifySuccess(data: FloatArray, image: FaceImage) {
    finishWithType(FinishType.Success)
    onSuccess(FaceResult(data = data, image = image))
  }

  private fun restartTimeoutJob() {
    _timeoutJob?.cancel()
    _timeoutJob = coroutineScope.launch {
      delay(timeout)
      finishWithType(FinishType.Timeout)
    }
  }

  private fun startStaticJob() {
    if (_staticJob?.isActive == true) return
    _staticJob = coroutineScope.launch {
      launch {
        _stateFlow
          .map { it.stage is Stage.Preparing }
          .distinctUntilChanged()
          .collect {
            if (it) {
              restartTimeoutJob()
            }
          }
      }
      launch {
        _stateFlow
          .map {
            when (val stage = it.stage) {
              is Stage.Interacting -> stage.interactionStage
              else -> null
            }
          }
          .distinctUntilChanged()
          .collect {
            if (it != null) {
              restartTimeoutJob()
            }
          }
      }
    }
  }

  private fun updatePreparingStage(update: (Stage.Preparing) -> Stage.Preparing) {
    updateState {
      val oldStage = it.stage
      if (oldStage is Stage.Preparing) {
        it.copy(stage = update(oldStage))
      } else {
        it
      }
    }
  }

  private fun updateInteractingStage(update: (Stage.Interacting) -> Stage.Interacting) {
    updateState {
      val oldStage = it.stage
      if (oldStage is Stage.Interacting) {
        it.copy(stage = update(oldStage))
      } else {
        it
      }
    }
  }

  /** 更新状态 */
  private fun updateState(update: (State) -> State) {
    _stateFlow.update(update)
  }

  data class State(
    val stage: Stage = Stage.None,
    val faceCount: Int = 0,
    val faceState: FaceState? = null,
  ) {
    val isFinishedWithTimeout: Boolean get() = stage is Stage.Finished && stage.type == FinishType.Timeout
  }

  sealed interface Stage {
    data object None : Stage

    /** 准备阶段 */
    data class Preparing(
      val checkCount: Int = 0,
    ) : Stage

    /** 互动阶段 */
    data class Interacting(
      /** 需要互动的类型列表 */
      val listInteractionType: List<FaceInteractionType>,
      /** 当前互动的类型 */
      val interactionType: FaceInteractionType,
      /** 当前互动的阶段 */
      val interactionStage: FaceInteractionStage,
      /** 当前互动类型的次数 */
      val interactionCount: Int,
    ) : Stage

    /** 结束 */
    data class Finished(
      val type: FinishType,
    ) : Stage
  }

  enum class FinishType {
    Normal,
    Success,
    Timeout,
    InternalError,
  }

  enum class InvalidType {
    /** 未检测到人脸 */
    NoFace,

    /** 检测到多张人脸 */
    MultiFace,

    /** 人脸质量低 */
    LowFaceQuality,

    /** 脸部五官不自然 */
    FaceInteraction,
  }

  companion object {
    private const val MIN_FACE_QUALITY_PREPARING = 0.75f
    private const val MIN_FACE_QUALITY_INTERACTING = 0.65f

    /** 无效类型 */
    private fun State.invalidType(): InvalidType? {
      return preparingInvalidType() ?: interactingInvalidType()
    }

    /** 准备阶段无效类型 */
    private fun State.preparingInvalidType(): InvalidType? {
      if (stage is Stage.Preparing) {
        if (faceCount <= 0) return InvalidType.NoFace
        if (faceCount > 1) return InvalidType.MultiFace
        if (faceState == null) return null
        if (faceState.leftEyeOpen == false || faceState.rightEyeOpen == false) return InvalidType.LowFaceQuality
        if (faceState.faceQuality < MIN_FACE_QUALITY_PREPARING) return InvalidType.LowFaceQuality
        if (faceState.hasInteraction) return InvalidType.FaceInteraction
      }
      return null
    }

    /** 互动阶段无效类型 */
    private fun State.interactingInvalidType(): InvalidType? {
      if (stage is Stage.Interacting) {
        if (faceCount <= 0) return InvalidType.NoFace
        if (faceCount > 1) return InvalidType.MultiFace
        if (faceState == null) return null
        if (faceState.faceQuality < MIN_FACE_QUALITY_INTERACTING) return InvalidType.LowFaceQuality
      }
      return null
    }

    /** 检测脸部状态是否正常 */
    private fun FaceState.checkFaceState(): Boolean {
      return faceQuality >= MIN_FACE_QUALITY_PREPARING
        && leftEyeOpen == true && rightEyeOpen == true
        && !hasInteraction
    }
  }
}
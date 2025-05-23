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
  /** 要互动的类型列表 */
  private val listInteractionType: List<FaceInteractionType> = emptyList(),
  /** 最小人脸质量[0-1] */
  private val getMinFaceQuality: (Stage) -> Float = {
    when (it) {
      is Stage.Preparing -> 0.75f
      is Stage.Interacting -> when (it.interactionStage) {
        FaceInteractionStage.Interacting -> 0.65f
        FaceInteractionStage.Stop -> 0.75f
      }
      else -> 0f
    }
  },
  /** 人脸占图片的最小比例[0-1] */
  private val getMinFaceScale: (Stage) -> Float = { 0.6f },
  /** 超时(毫秒) */
  private val timeout: Long = 15_000,
  /** 最小验证人脸相似度[0-1] */
  private val minValidateSimilarity: Float = 0.8f,
  /** 成功回调 */
  private val onSuccess: (FaceResult) -> Unit,
) {
  private val _stateFlow = MutableStateFlow<State>(State())
  val stateFlow: StateFlow<State> = _stateFlow.asStateFlow()

  private val _currentState get() = stateFlow.value

  private val _faceInfoChecker = FaceInfoChecker()
  /** 通过检测的脸部数据 */
  private var _checkedFaceData: FloatArray? = null
  /** 通过检测到脸部图片 */
  private var _checkedFaceImage: FaceImage? = null

  private var _timeoutJob: Job? = null
  private var _staticJob: Job? = null

  init {
    require(timeout >= 5_000)
    require(minValidateSimilarity in 0f..1f)
  }

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
      updateState { it.copy(stage = Stage.Preparing) }
      startStaticJob()
    }

    when (faceInfo) {
      is ErrorGetFaceInfo -> {}
      is InvalidFaceCountFaceInfo -> {
        updateState { it.copy(faceCount = faceInfo.faceCount) }
      }
      is ValidFaceInfo -> {
        updateState {
          it.copy(
            faceCount = 1,
            faceState = faceInfo.faceState,
            faceBounds = faceInfo.faceBounds,
          )
        }
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
    _faceInfoChecker.reset()
    _checkedFaceData = null
    _checkedFaceImage = null
  }

  private fun handleValidFaceInfo(faceInfo: ValidFaceInfo) {
    when (val stage = _currentState.stage) {
      is Stage.Preparing -> handleStagePreparing(faceInfo = faceInfo)
      is Stage.Interacting -> handleStageInteracting(faceInfo = faceInfo, stage = stage)
      else -> {}
    }
  }

  /** 准备阶段 */
  private fun handleStagePreparing(faceInfo: ValidFaceInfo) {
    if (!_faceInfoChecker.check(Stage.Preparing, faceInfo)) return

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
          interactionStage = FaceInteractionStage.Interacting,
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

    when (stage.interactionStage) {
      FaceInteractionStage.Interacting -> {
        if (!_faceInfoChecker.check(stage, faceInfo, targetCount = 1)) return
        val hasTargetInteraction = with(faceInfo.faceState) {
          when (stage.interactionType) {
            FaceInteractionType.Blink -> blink
            FaceInteractionType.Shake -> shake
            FaceInteractionType.MouthOpen -> mouthOpen
            FaceInteractionType.RaiseHead -> raiseHead
          }
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
        if (!_faceInfoChecker.check(stage, faceInfo)) return
        val similarity = faceCompare(checkedFaceData, faceInfo.faceData)
        if (similarity >= minValidateSimilarity) {
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
        interactionStage = FaceInteractionStage.Interacting,
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
    val faceBounds: FaceBounds? = null,
  ) {
    val isFinishedWithTimeout: Boolean get() = stage is Stage.Finished && stage.type == FinishType.Timeout
  }

  sealed interface Stage {
    data object None : Stage

    /** 准备阶段 */
    data object Preparing : Stage

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

    /** 人脸区域太小 */
    SmallFace,

    /** 人脸质量低 */
    LowFaceQuality,

    /** 脸部五官不自然 */
    FaceInteraction,
  }

  private inner class FaceInfoChecker {
    private var _count = 0

    fun check(
      stage: Stage,
      faceInfo: ValidFaceInfo,
      targetCount: Int = 15,
    ): Boolean {
      if (checkFaceInfo(stage, faceInfo)) _count++ else _count = 0
      return (_count >= targetCount).also { if (it) reset() }
    }

    fun reset() {
      _count = 0
    }
  }

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
      if (faceState.faceQuality < getMinFaceQuality(stage)) return InvalidType.LowFaceQuality
      if (faceState.hasInteraction) return InvalidType.FaceInteraction

      if (faceBounds == null) return null
      if (faceBounds.faceWidthScale < 0.6f) return InvalidType.SmallFace
    }
    return null
  }

  /** 互动阶段无效类型 */
  private fun State.interactingInvalidType(): InvalidType? {
    if (stage is Stage.Interacting) {
      if (faceCount <= 0) return InvalidType.NoFace
      if (faceCount > 1) return InvalidType.MultiFace

      if (faceState == null) return null
      if (faceState.faceQuality < getMinFaceQuality(stage)) return InvalidType.LowFaceQuality

      if (faceBounds == null) return null
      if (faceBounds.faceWidthScale < 0.6f) return InvalidType.SmallFace
    }
    return null
  }

  /** 检查脸部信息 */
  private fun checkFaceInfo(
    stage: Stage,
    faceInfo: ValidFaceInfo,
  ): Boolean {
    return checkFaceState(stage, faceInfo.faceState) && checkFaceBounds(stage, faceInfo.faceBounds)
  }

  /** 检测脸部状态是否正常 */
  private fun checkFaceState(
    stage: Stage,
    faceState: FaceState,
  ): Boolean {
    return with(faceState) {
      faceQuality >= getMinFaceQuality(stage)
        && leftEyeOpen == true && rightEyeOpen == true
        && !hasInteraction
    }
  }

  /** 检测脸部区域是否正常 */
  private fun checkFaceBounds(
    stage: Stage,
    faceBounds: FaceBounds,
  ): Boolean {
    return with(faceBounds) {
      faceWidthScale >= getMinFaceScale(stage)
    }
  }
}
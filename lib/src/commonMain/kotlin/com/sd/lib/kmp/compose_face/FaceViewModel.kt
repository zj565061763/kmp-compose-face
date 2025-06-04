package com.sd.lib.kmp.compose_face

import com.sd.lib.kmp.face.ErrorFaceInfo
import com.sd.lib.kmp.face.FaceBounds
import com.sd.lib.kmp.face.FaceImage
import com.sd.lib.kmp.face.FaceInfo
import com.sd.lib.kmp.face.FaceState
import com.sd.lib.kmp.face.InvalidFaceCountFaceInfo
import com.sd.lib.kmp.face.ValidFaceInfo
import com.sd.lib.kmp.face.faceCompare
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
  private val getInteractionTypes: () -> List<FaceInteractionType> = { FaceInteractionType.entries },
  /** 最小人脸质量[0-1] */
  private val getMinFaceQuality: (Stage) -> Float = { minFaceQualityOfStage(it) },
  /** 人脸占图片的最小比例[0-1] */
  private val getMinFaceScale: (Stage) -> Float = { minFaceScaleOfStage(it) },
  /** 超时(毫秒) */
  private val timeout: Long = 15_000,
  /** 最小验证人脸相似度[0-1] */
  private val minValidateSimilarity: Float = 0.75f,
) {
  private val _stateFlow = MutableStateFlow<State>(State())
  val stateFlow: StateFlow<State> = _stateFlow.asStateFlow()

  private val _currentState get() = stateFlow.value

  private var _checkedFaceCount: Int = 0
  private var _checkedFaceQuality: Float = 0f
  private var _checkedFaceData: FloatArray? = null
  private var _checkedFaceImage: FaceImage? = null

  private var _timeoutJob: Job? = null
  private var _staticJob: Job? = null

  init {
    require(timeout >= 5_000)
    require(minValidateSimilarity in 0f..1f)
  }

  /** 无效类型 */
  val invalidTypeFlow: Flow<InvalidType?> = _stateFlow
    .map { it.checkInvalidType() }
    .distinctUntilChanged()
    .mapLatest { tips ->
      if (tips != null) delay(800)
      tips
    }.distinctUntilChanged()

  /** 处理脸部信息 */
  fun process(faceInfo: FaceInfo) {
    val stage = _currentState.stage
    if (stage is StageFinished) {
      return
    }

    if (stage is StageNone) {
      updateState { it.copy(stage = StagePreparing) }
      startStaticJob()
    }

    when (faceInfo) {
      is ErrorFaceInfo -> {
        finishStage(StageFinished.Error(faceInfo.error))
        return
      }
      is InvalidFaceCountFaceInfo -> updateState { it.copy(faceCount = faceInfo.faceCount) }
      is ValidFaceInfo -> {
        updateState {
          it.copy(
            faceCount = 1,
            faceState = faceInfo.faceState,
            faceBounds = faceInfo.faceBounds,
          )
        }
        try {
          val state = _currentState
          when (val stage = state.stage) {
            is StagePreparing -> handleStagePreparing(faceInfo = faceInfo, state = state)
            is StageInteracting -> handleStageInteracting(faceInfo = faceInfo, state = state, stage = stage)
            else -> error("Illegal stage:$stage")
          }
        } finally {
          faceInfo.close()
        }
      }
    }
  }

  /** 结束 */
  fun finish() {
    finishStage(StageFinished.Normal)
  }

  /** 结束后，重新开始 */
  fun restart() {
    if (_currentState.stage is StageFinished) {
      updateState { State() }
    }
  }

  private fun finishStage(stage: StageFinished) {
    _timeoutJob?.cancel()
    _staticJob?.cancel()
    if (_currentState.stage !is StageFinished) {
      updateState { State(stage = stage) }
    }
    _checkedFaceCount = 0
    _checkedFaceQuality = 0f
    _checkedFaceData = null
    _checkedFaceImage?.close()
    _checkedFaceImage = null
  }

  /** 准备阶段 */
  private fun handleStagePreparing(
    faceInfo: ValidFaceInfo,
    state: State,
  ) {
    if (state.checkInvalidType() != null) return

    val newFaceQuality = state.faceState.faceQuality
    if (newFaceQuality > _checkedFaceQuality) {
      _checkedFaceQuality = newFaceQuality
      _checkedFaceData = faceInfo.faceData
      _checkedFaceImage?.close()
      _checkedFaceImage = faceInfo.getFaceImage().also { it.init() }
    }

    _checkedFaceCount++
    if (_checkedFaceCount < 5) {
      return
    }

    val listInteractionType = getInteractionTypes()
    if (listInteractionType.isEmpty()) {
      notifySuccess()
      return
    }

    val listType = listInteractionType.shuffled()
    val firstType = listType.first()
    updateState {
      it.copy(
        stage = StageInteracting(
          listInteractionType = listType - firstType,
          interactionType = firstType,
          interactionStage = FaceInteractionStage.Interacting,
        ),
      )
    }
  }

  /** 互动阶段 */
  private fun handleStageInteracting(
    faceInfo: ValidFaceInfo,
    state: State,
    stage: StageInteracting,
  ) {
    if (state.checkInvalidType() != null) return

    val checkedFaceData = _checkedFaceData
    if (checkedFaceData == null) {
      finishStage(StageFinished.Error(Exception("_checkedFaceData is null when StageInteracting")))
      return
    }

    when (stage.interactionStage) {
      FaceInteractionStage.Interacting -> {
        with(state.faceState) {
          when (stage.interactionType) {
            FaceInteractionType.Blink -> blink
            FaceInteractionType.Shake -> shake
            FaceInteractionType.MouthOpen -> mouthOpen
            FaceInteractionType.RaiseHead -> raiseHead
          }
        }.also { hasTargetInteraction ->
          if (hasTargetInteraction) {
            val targetInteractionCount = targetInteractionCount(stage.interactionType)
            updateInteractingStage {
              val newCount = it.interactionCount + 1
              if (newCount >= targetInteractionCount) {
                it.copy(interactionStage = FaceInteractionStage.Stop)
              } else {
                it.copy(interactionCount = newCount)
              }
            }
          }
        }
      }
      FaceInteractionStage.Stop -> {
        val listInteractionType = stage.listInteractionType
        if (listInteractionType.isEmpty()) {
          val similarity = faceCompare(checkedFaceData, faceInfo.faceData)
          if (similarity >= minValidateSimilarity) {
            notifySuccess()
          }
        } else {
          moveToNextInteractionType(listInteractionType)
        }
      }
    }
  }

  private fun moveToNextInteractionType(listInteractionType: List<FaceInteractionType>) {
    val nextType = listInteractionType.first()
    val newList = listInteractionType - nextType
    updateInteractingStage {
      StageInteracting(
        listInteractionType = newList,
        interactionType = nextType,
        interactionStage = FaceInteractionStage.Interacting,
      )
    }
  }

  private fun notifySuccess() {
    val faceData = _checkedFaceData
    val faceImage = _checkedFaceImage
    if (faceData == null || faceImage == null) {
      finishStage(StageFinished.Error(Exception("_checkedFaceData or _checkedFaceImage is null when notifySuccess")))
    } else {
      val result = FaceResult(data = faceData, image = faceImage)
      finishStage(StageFinished.Success(result = result))
    }
  }

  private fun restartTimeoutJob() {
    _timeoutJob?.cancel()
    _timeoutJob = coroutineScope.launch {
      delay(timeout)
      finishStage(StageFinished.Timeout)
    }
  }

  private fun startStaticJob() {
    if (_staticJob?.isActive == true) return
    _staticJob = coroutineScope.launch {
      launch {
        _stateFlow
          .map { it.stage is StagePreparing }
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
              is StageInteracting -> stage.interactionStage
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

  private fun updateInteractingStage(update: (StageInteracting) -> StageInteracting) {
    updateState {
      val oldStage = it.stage
      if (oldStage is StageInteracting) {
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
    val stage: Stage = StageNone,
    val faceCount: Int = 0,
    val faceState: FaceState = FaceState.Empty,
    val faceBounds: FaceBounds = FaceBounds.Empty,
  )

  sealed interface Stage
  data object StageNone : Stage
  data object StagePreparing : Stage
  data class StageInteracting(
    /** 需要互动的类型列表 */
    val listInteractionType: List<FaceInteractionType>,
    /** 当前互动的类型 */
    val interactionType: FaceInteractionType,
    /** 当前互动的阶段 */
    val interactionStage: FaceInteractionStage,
    /** 当前互动类型的互动次数 */
    val interactionCount: Int = 0,
  ) : Stage

  sealed interface StageFinished : Stage {
    data object Normal : StageFinished
    data object Timeout : StageFinished

    data class Error(
      val error: Throwable,
    ) : StageFinished

    data class Success(
      val result: FaceResult,
    ) : StageFinished
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

  private fun State.checkInvalidType(): InvalidType? {
    return when (stage) {
      is StagePreparing -> checkInvalidTypeWithParams()
      is StageInteracting -> checkInvalidTypeWithParams(checkFaceInteraction = false)
      else -> null
    }
  }

  private fun State.checkInvalidTypeWithParams(
    checkFaceInteraction: Boolean = true,
  ): InvalidType? {
    // 检查人脸数量
    if (faceCount <= 0) return InvalidType.NoFace
    if (faceCount > 1) return InvalidType.MultiFace

    // 检查人脸质量是否太低
    if (faceState.faceQuality < getMinFaceQuality(stage)) return InvalidType.LowFaceQuality

    // 检查人脸互动
    if (checkFaceInteraction) {
      if (faceState.hasInteraction) return InvalidType.FaceInteraction
    }

    // 检查人脸区域是否太小
    if (faceBounds.faceWidthScale < getMinFaceScale(stage)) return InvalidType.SmallFace

    return null
  }

  companion object {
    private fun minFaceQualityOfStage(stage: Stage): Float {
      if (stage is StageInteracting
        && stage.interactionStage == FaceInteractionStage.Interacting
      ) return 0.5f
      return 0.7f
    }

    private fun minFaceScaleOfStage(stage: Stage): Float {
      if (stage is StageInteracting
        && stage.interactionStage == FaceInteractionStage.Interacting
        && stage.interactionType == FaceInteractionType.RaiseHead
      ) return 0.4f
      return 0.5f
    }

    private fun targetInteractionCount(type: FaceInteractionType): Int {
      return when (type) {
        FaceInteractionType.Blink -> 3
        else -> 1
      }
    }
  }
}
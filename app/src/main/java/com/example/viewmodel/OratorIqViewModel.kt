package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioAndDeliverablesManager
import com.example.audio.ChatAnalysisService
import com.example.audio.ChatMessage
import com.example.audio.PlaybackMode
import com.example.data.ContrastiveDatasetRepository
import com.example.dsp.ContrastiveSpeechAnalyticsEngine
import com.example.dsp.AdvancedSpeechMetricsEngine
import com.example.model.AdaptivePracticeAttemptResult
import com.example.model.AdaptivePracticeDrill
import com.example.model.BaselineSpeechSpec
import com.example.model.ContrastiveEvaluationResult
import com.example.model.GroundedFlawRegion
import com.example.model.PerturbationConfig
import com.example.model.SpectrumTier
import com.example.model.StressTestSuiteReport
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TelemetryChannel(val label: String, val unitLabel: String) {
  PITCH_SEMITONES("Pitch Contour", "Speaker-Normalized Semitones (st)"),
  RMS_ENERGY_Z("Vocal Energy", "Normalized Energy Z-Score (σ_E)"),
  SYLLABLE_CADENCE("Pacing Speed", "Syllables / sec (Hz)"),
  MFCC_CEPSTRAL("Vocal Clarity", "13-MFCC Cepstral Similarity")
}

data class OratorIqUiState(
  val showTutorial: Boolean = true,
  val speeches: List<BaselineSpeechSpec> = ContrastiveDatasetRepository.speeches,
  val selectedSpeech: BaselineSpeechSpec = ContrastiveDatasetRepository.speeches.first(),
  val selectedTier: SpectrumTier = SpectrumTier.TIER_2_MODERATE_MONOTONE,
  val customPerturbation: PerturbationConfig = PerturbationConfig(
    pitchVarianceScale = 0.35f,
    tempoMultiplier = 0.74f,
    pauseRetentionRatio = 0.25f,
    trailingEnergyDropDb = 6.5f,
    spectralSlurAmount = 0.28f,
    localizeToClimaxOnly = false,
    simulateDifferentBiologyPitchHz = 182f
  ),
  val evaluationResult: ContrastiveEvaluationResult? = null,
  val isEvaluating: Boolean = true,
  val selectedTelemetryChannel: TelemetryChannel = TelemetryChannel.PITCH_SEMITONES,
  val selectedWordIndex: Int? = null,
  val selectedFlawId: String? = null,
  val playbackMode: PlaybackMode = PlaybackMode.IDLE,
  val playheadMs: Int = 0,
  val isRecordingMic: Boolean = false,
  val liveMicRms: Float = 0f,
  val liveMicElapsedMs: Int = 0,
  val isRecordingDrill: Boolean = false,
  val drillMicRms: Float = 0f,
  val drillElapsedMs: Int = 0,
  val drillAttemptResult: AdaptivePracticeAttemptResult? = null,
  val stressReport: StressTestSuiteReport? = null,
  val isRunningStressSuite: Boolean = false,
  val chatMessages: List<ChatMessage> = listOf(
    ChatMessage(
      id = "welcome_1",
      isUser = false,
      text = "Welcome to Chat Analysis. Ask me anything about your speech delivery, exact timestamps, or vocal drills.\n\n💡 API Quota Saver Active: All FFT, 13-MFCC, F0 pitch, and DTW word alignments run 100% locally on-device (0 API tokens used). Tap 'Tips to save my API quota' above for more ways to save your API credits."
    )
  ),
  val isChatLoading: Boolean = false,
  val bannerToast: String? = null
)

class OratorIqViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(OratorIqUiState())
  val uiState: StateFlow<OratorIqUiState> = _uiState.asStateFlow()

  private val audioManager = AudioAndDeliverablesManager(viewModelScope)

  init {
    recomputeEvaluation()
    runFullStressSuite()
  }

  fun openTutorial() {
    _uiState.update { it.copy(showTutorial = true) }
  }

  fun dismissTutorial() {
    _uiState.update { it.copy(showTutorial = false) }
  }

  fun sendChatMessage(prompt: String) {
    val trimmed = prompt.trim()
    if (trimmed.isEmpty() || _uiState.value.isChatLoading) return

    val userMsg = ChatMessage(
      id = "user_${System.currentTimeMillis()}",
      isUser = true,
      text = trimmed
    )
    _uiState.update {
      it.copy(
        chatMessages = it.chatMessages + userMsg,
        isChatLoading = true
      )
    }

    viewModelScope.launch {
      val eval = _uiState.value.evaluationResult
      val replyText = ChatAnalysisService.askSpeechAnalyst(trimmed, eval)
      val botMsg = ChatMessage(
        id = "bot_${System.currentTimeMillis()}",
        isUser = false,
        text = replyText
      )
      _uiState.update {
        it.copy(
          chatMessages = it.chatMessages + botMsg,
          isChatLoading = false
        )
      }
    }
  }

  fun selectSpeech(speech: BaselineSpeechSpec) {
    audioManager.stopPlayback()
    _uiState.update {
      it.copy(
        selectedSpeech = speech,
        playbackMode = PlaybackMode.IDLE,
        playheadMs = 0,
        selectedWordIndex = null,
        selectedFlawId = null
      )
    }
    recomputeEvaluation()
  }

  fun selectTier(tier: SpectrumTier) {
    audioManager.stopPlayback()
    _uiState.update {
      it.copy(
        selectedTier = tier,
        playbackMode = PlaybackMode.IDLE,
        playheadMs = 0,
        selectedWordIndex = null,
        selectedFlawId = null
      )
    }
    recomputeEvaluation()
  }

  fun updateCustomPerturbation(newConfig: PerturbationConfig) {
    audioManager.stopPlayback()
    _uiState.update {
      it.copy(
        selectedTier = SpectrumTier.CUSTOM_SYNTH_OR_LIVE,
        customPerturbation = newConfig,
        playbackMode = PlaybackMode.IDLE
      )
    }
    recomputeEvaluation()
  }

  fun selectTelemetryChannel(channel: TelemetryChannel) {
    _uiState.update { it.copy(selectedTelemetryChannel = channel) }
  }

  fun selectWordIndex(index: Int) {
    val eval = _uiState.value.evaluationResult ?: return
    val word = eval.alignedWords.getOrNull(index) ?: return
    val matchingFlaw = eval.groundedFlaws.firstOrNull { index in it.wordStartIndex..it.wordEndIndex }
    _uiState.update {
      it.copy(
        selectedWordIndex = index,
        selectedFlawId = matchingFlaw?.id ?: it.selectedFlawId,
        playheadMs = word.participantStartMs
      )
    }
  }

  fun selectFlaw(flaw: GroundedFlawRegion) {
    _uiState.update {
      it.copy(
        selectedFlawId = flaw.id,
        selectedWordIndex = flaw.wordStartIndex,
        playheadMs = flaw.participantStartMs
      )
    }
  }

  fun scrubPlayhead(timestampMs: Int) {
    val eval = _uiState.value.evaluationResult ?: return
    val clamped = timestampMs.coerceIn(0, maxOf(eval.baselineDurationMs, eval.participantDurationMs))
    val wordAtCursor = eval.alignedWords.firstOrNull { clamped in it.participantStartMs..it.participantEndMs }
    val flawAtCursor = eval.groundedFlaws.firstOrNull { clamped in it.participantStartMs..it.participantEndMs }
    _uiState.update {
      it.copy(
        playheadMs = clamped,
        selectedWordIndex = wordAtCursor?.wordIndex ?: it.selectedWordIndex,
        selectedFlawId = flawAtCursor?.id ?: it.selectedFlawId
      )
    }
  }

  fun togglePlayBaseline() {
    val eval = _uiState.value.evaluationResult ?: return
    if (_uiState.value.playbackMode == PlaybackMode.PLAYING_BASELINE) {
      stopAudio()
      return
    }
    _uiState.update { it.copy(playbackMode = PlaybackMode.PLAYING_BASELINE, playheadMs = 0) }
    audioManager.playPcmSlice(
      pcm = eval.baselineAudioPcm,
      sampleRate = eval.sampleRate,
      startMs = 0,
      endMs = eval.baselineDurationMs,
      mode = PlaybackMode.PLAYING_BASELINE,
      onProgressMs = { ms, mode ->
        _uiState.update { it.copy(playheadMs = ms, playbackMode = mode) }
      },
      onComplete = {
        _uiState.update { it.copy(playbackMode = PlaybackMode.IDLE) }
      }
    )
  }

  fun togglePlayParticipant() {
    val eval = _uiState.value.evaluationResult ?: return
    if (_uiState.value.playbackMode == PlaybackMode.PLAYING_PARTICIPANT) {
      stopAudio()
      return
    }
    _uiState.update { it.copy(playbackMode = PlaybackMode.PLAYING_PARTICIPANT, playheadMs = 0) }
    audioManager.playPcmSlice(
      pcm = eval.participantAudioPcm,
      sampleRate = eval.sampleRate,
      startMs = 0,
      endMs = eval.participantDurationMs,
      mode = PlaybackMode.PLAYING_PARTICIPANT,
      onProgressMs = { ms, mode ->
        _uiState.update { it.copy(playheadMs = ms, playbackMode = mode) }
      },
      onComplete = {
        _uiState.update { it.copy(playbackMode = PlaybackMode.IDLE) }
      }
    )
  }

  fun playAbFlawComparison(flaw: GroundedFlawRegion) {
    val eval = _uiState.value.evaluationResult ?: return
    if (_uiState.value.playbackMode != PlaybackMode.IDLE) {
      stopAudio()
    }
    _uiState.update {
      it.copy(
        selectedFlawId = flaw.id,
        selectedWordIndex = flaw.wordStartIndex,
        playbackMode = PlaybackMode.PLAYING_AB_CONTRAST,
        bannerToast = "Playing Reference (${flaw.baselineStartMs}–${flaw.baselineEndMs}ms) then Your Take (${flaw.participantStartMs}–${flaw.participantEndMs}ms)"
      )
    }
    audioManager.playAbFlawContrast(
      eval = eval,
      flaw = flaw,
      onProgressMs = { ms, mode ->
        _uiState.update { it.copy(playheadMs = ms, playbackMode = mode) }
      },
      onComplete = {
        _uiState.update { it.copy(playbackMode = PlaybackMode.IDLE) }
      }
    )
  }

  fun stopAudio() {
    audioManager.stopPlayback {
      _uiState.update { it.copy(playbackMode = PlaybackMode.IDLE) }
    }
  }

  fun startLiveMicRecording() {
    stopAudio()
    _uiState.update {
      it.copy(
        isRecordingMic = true,
        liveMicRms = 0f,
        liveMicElapsedMs = 0,
        bannerToast = "Recording your voice... Read the transcript aloud."
      )
    }
    audioManager.startLiveMicrophoneRecording(
      sampleRate = 16000,
      maxDurationSeconds = 18,
      onLiveTelemetry = { rms, elapsedMs ->
        _uiState.update {
          it.copy(liveMicRms = rms, liveMicElapsedMs = elapsedMs)
        }
      },
      onFinished = { recordedPcm ->
        _uiState.update { it.copy(isRecordingMic = false) }
        if (recordedPcm.size > 4000) {
          evaluateLiveOrImportedPcm(recordedPcm, "Live Recording (${recordedPcm.size / 16000}s)")
        } else {
          _uiState.update {
            it.copy(bannerToast = "Recording was too brief. Please try again.")
          }
        }
      },
      onError = { msg ->
        _uiState.update {
          it.copy(isRecordingMic = false, bannerToast = msg)
        }
      }
    )
  }

  fun stopLiveMicRecording() {
    audioManager.stopLiveMicrophoneRecording()
  }

  fun startDrillRecording(drill: AdaptivePracticeDrill) {
    stopAudio()
    _uiState.update {
      it.copy(
        isRecordingDrill = true,
        drillMicRms = 0f,
        drillElapsedMs = 0,
        drillAttemptResult = null,
        bannerToast = "Recording practice drill... Read the tongue-twister aloud."
      )
    }
    audioManager.startLiveMicrophoneRecording(
      sampleRate = 16000,
      maxDurationSeconds = 12,
      onLiveTelemetry = { rms, elapsedMs ->
        _uiState.update {
          it.copy(drillMicRms = rms, drillElapsedMs = elapsedMs)
        }
      },
      onFinished = { recordedPcm ->
        _uiState.update { it.copy(isRecordingDrill = false) }
        if (recordedPcm.size > 3000) {
          val result = AdvancedSpeechMetricsEngine.evaluatePracticeDrillAttempt(drill, recordedPcm)
          _uiState.update {
            it.copy(
              drillAttemptResult = result,
              bannerToast = "Drill evaluated • Score: ${result.pronunciationScore.toInt()}% (+${String.format(Locale.US, "%.1f", result.improvementDeltaPercent)}%)"
            )
          }
        } else {
          _uiState.update {
            it.copy(bannerToast = "Drill take was too brief. Please try again.")
          }
        }
      },
      onError = { msg ->
        _uiState.update {
          it.copy(isRecordingDrill = false, bannerToast = msg)
        }
      }
    )
  }

  fun stopDrillRecording() {
    audioManager.stopLiveMicrophoneRecording()
  }

  fun resetDrillAttempt() {
    _uiState.update { it.copy(drillAttemptResult = null) }
  }

  fun regenerateAdaptiveDrill() {
    val eval = _uiState.value.evaluationResult ?: return
    val matrix = eval.rubric.fluencyMatrix ?: return
    val newDrill = AdvancedSpeechMetricsEngine.generateAdaptivePracticeDrill(
      speechSpec = eval.speechSpec,
      alignedWords = eval.alignedWords,
      flaws = eval.groundedFlaws,
      matrix = matrix
    )
    _uiState.update {
      it.copy(
        evaluationResult = eval.copy(adaptiveDrill = newDrill),
        drillAttemptResult = null,
        bannerToast = "Generated fresh adaptive drill: ${newDrill.shortfall.category}"
      )
    }
  }

  fun importExternalWavUri(context: Context, uri: Uri, onCompleteNavigateToWorkbench: () -> Unit) {
    viewModelScope.launch {
      _uiState.update { it.copy(isEvaluating = true, bannerToast = "Importing WAV audio...") }
      val decodedPcm = withContext(Dispatchers.IO) {
        audioManager.decodeWavUri(context, uri)
      }
      if (decodedPcm != null && decodedPcm.size > 3200) {
        evaluateLiveOrImportedPcm(decodedPcm, "Imported WAV (${decodedPcm.size / 16000}s)")
        onCompleteNavigateToWorkbench()
      } else {
        _uiState.update {
          it.copy(
            isEvaluating = false,
            bannerToast = "Could not read WAV file. Please select a standard .wav file."
          )
        }
      }
    }
  }

  private fun evaluateLiveOrImportedPcm(pcm: ShortArray, label: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isEvaluating = true, selectedTier = SpectrumTier.CUSTOM_SYNTH_OR_LIVE) }
      val speech = _uiState.value.selectedSpeech
      val result = withContext(Dispatchers.Default) {
        ContrastiveSpeechAnalyticsEngine.evaluateContrastivePair(
          speechSpec = speech,
          tier = SpectrumTier.CUSTOM_SYNTH_OR_LIVE,
          customConfig = _uiState.value.customPerturbation,
          overrideParticipantPcm = pcm
        )
      }
      _uiState.update {
        it.copy(
          evaluationResult = result,
          isEvaluating = false,
          selectedFlawId = result.groundedFlaws.firstOrNull()?.id,
          selectedWordIndex = result.groundedFlaws.firstOrNull()?.wordStartIndex ?: 0,
          bannerToast = "Analyzed $label • ${result.groundedFlaws.size} flaw region(s) found."
        )
      }
    }
  }

  private fun recomputeEvaluation() {
    viewModelScope.launch {
      _uiState.update { it.copy(isEvaluating = true) }
      val currentSpeech = _uiState.value.selectedSpeech
      val currentTier = _uiState.value.selectedTier
      val currentCustom = _uiState.value.customPerturbation

      val eval = withContext(Dispatchers.Default) {
        ContrastiveSpeechAnalyticsEngine.evaluateContrastivePair(
          speechSpec = currentSpeech,
          tier = currentTier,
          customConfig = currentCustom
        )
      }

      _uiState.update {
        it.copy(
          evaluationResult = eval,
          isEvaluating = false,
          selectedFlawId = eval.groundedFlaws.firstOrNull()?.id,
          selectedWordIndex = eval.groundedFlaws.firstOrNull()?.wordStartIndex ?: 0
        )
      }
    }
  }

  fun runFullStressSuite() {
    viewModelScope.launch {
      _uiState.update { it.copy(isRunningStressSuite = true) }
      val report = withContext(Dispatchers.Default) {
        ContrastiveSpeechAnalyticsEngine.runStressTestSuite()
      }
      _uiState.update {
        it.copy(
          stressReport = report,
          isRunningStressSuite = false
        )
      }
    }
  }

  fun exportCurrentDatasetBundle(context: Context) {
    val eval = _uiState.value.evaluationResult ?: return
    viewModelScope.launch {
      val msg = withContext(Dispatchers.IO) {
        audioManager.exportContrastiveDatasetBundle(context, eval)
      }
      _uiState.update { it.copy(bannerToast = msg) }
    }
  }

  fun exportTechnicalPaper(context: Context) {
    val eval = _uiState.value.evaluationResult ?: return
    val report = _uiState.value.stressReport ?: return
    audioManager.shareTechnicalDocumentation(context, eval, report)
  }

  fun getManifestJsonPreview(): String {
    val eval = _uiState.value.evaluationResult ?: return "{}"
    return audioManager.buildDatasetManifestJson(eval)
  }

  fun getTechnicalPaperPreview(): String {
    val eval = _uiState.value.evaluationResult ?: return ""
    val report = _uiState.value.stressReport ?: return ""
    return audioManager.buildTechnicalPaperMarkdown(eval, report)
  }

  fun dismissToast() {
    _uiState.update { it.copy(bannerToast = null) }
  }

  override fun onCleared() {
    super.onCleared()
    audioManager.stopPlayback()
    audioManager.stopLiveMicrophoneRecording()
  }
}

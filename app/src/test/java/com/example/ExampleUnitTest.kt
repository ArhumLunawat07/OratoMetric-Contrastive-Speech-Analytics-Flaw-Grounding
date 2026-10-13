package com.example

import com.example.data.ContrastiveDatasetRepository
import com.example.dsp.ContrastiveSpeechAnalyticsEngine
import com.example.model.PerturbationConfig
import com.example.model.SpectrumTier
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun contrastiveSpectrum_exhibitsMonotonicScoreGradientAndTemporalGrounding() {
    val jfk = ContrastiveDatasetRepository.speeches.first()

    val evalT0 = ContrastiveSpeechAnalyticsEngine.evaluateContrastivePair(jfk, SpectrumTier.TIER_0_IDEAL)
    val evalT1 = ContrastiveSpeechAnalyticsEngine.evaluateContrastivePair(jfk, SpectrumTier.TIER_1_NEAR_PERFECT)
    val evalT2 = ContrastiveSpeechAnalyticsEngine.evaluateContrastivePair(jfk, SpectrumTier.TIER_2_MODERATE_MONOTONE)
    val evalT3 = ContrastiveSpeechAnalyticsEngine.evaluateContrastivePair(jfk, SpectrumTier.TIER_3_SEVERE_CLUTTER)
    val evalT4 = ContrastiveSpeechAnalyticsEngine.evaluateContrastivePair(jfk, SpectrumTier.TIER_4_BOTCHED)

    assertTrue("Tier 0 should score highest", evalT0.rubric.overallScore > evalT1.rubric.overallScore)
    assertTrue("Tier 1 should score higher than Tier 2", evalT1.rubric.overallScore > evalT2.rubric.overallScore)
    assertTrue("Tier 2 should score higher than Tier 3", evalT2.rubric.overallScore > evalT3.rubric.overallScore)
    assertTrue("Tier 3 should score higher than Tier 4", evalT3.rubric.overallScore > evalT4.rubric.overallScore)

    assertTrue("Flawed takes must produce temporally grounded flaw regions", evalT2.groundedFlaws.isNotEmpty())
    assertTrue("Botched take must isolate multiple grounded flaws", evalT4.groundedFlaws.size >= 2)
  }

  @Test
  fun speakerNormalization_isInvariantToBiologicalPitchDifferences() {
    val mlk = ContrastiveDatasetRepository.speeches[1]
    val baseConfig = ContrastiveDatasetRepository.getPerturbationForTier(
      SpectrumTier.TIER_1_NEAR_PERFECT,
      PerturbationConfig()
    )

    val baritoneEval = ContrastiveSpeechAnalyticsEngine.evaluateContrastivePair(
      speechSpec = mlk,
      tier = SpectrumTier.CUSTOM_SYNTH_OR_LIVE,
      customConfig = baseConfig.copy(simulateDifferentBiologyPitchHz = 115f)
    )
    val sopranoEval = ContrastiveSpeechAnalyticsEngine.evaluateContrastivePair(
      speechSpec = mlk,
      tier = SpectrumTier.CUSTOM_SYNTH_OR_LIVE,
      customConfig = baseConfig.copy(simulateDifferentBiologyPitchHz = 225f)
    )

    val drift = abs(baritoneEval.rubric.overallScore - sopranoEval.rubric.overallScore)
    assertTrue("Speaker-agnostic normalization must keep score drift < 2.0 pts (was $drift)", drift < 2.0f)
  }

  @Test
  fun sideBySideWaveformAndSpectrogram_extractsValidDualVisualTracks() {
    val speech = ContrastiveDatasetRepository.speeches.first()
    val eval = ContrastiveSpeechAnalyticsEngine.evaluateContrastivePair(speech, SpectrumTier.TIER_2_MODERATE_MONOTONE)

    val baseTrack = eval.baselineSpectrogramTrack
    val userTrack = eval.participantSpectrogramTrack

    assertTrue("Baseline visual track must be populated", baseTrack != null)
    assertTrue("User visual track must be populated", userTrack != null)
    assertTrue("Baseline waveform envelopes must contain samples", baseTrack!!.waveformEnvelope.isNotEmpty())
    assertTrue("Baseline spectrogram slices must be present", baseTrack.slices.isNotEmpty())
    assertTrue("Frequency bin count must match 16 bands", baseTrack.slices.first().frequencyBins.size == 16)
    assertTrue("User waveform envelopes must contain samples", userTrack!!.waveformEnvelope.isNotEmpty())
    assertTrue("User spectrogram slices must be present", userTrack.slices.isNotEmpty())
  }

  @Test
  fun forcedAlignment_extractsPhonemeSegmentsWithHeatmapStatus() {
    val speech = ContrastiveDatasetRepository.speeches.first()
    val eval = ContrastiveSpeechAnalyticsEngine.evaluateContrastivePair(speech, SpectrumTier.TIER_3_SEVERE_CLUTTER)

    assertTrue("Aligned words must be extracted", eval.alignedWords.isNotEmpty())
    val wordsWithPhonemes = eval.alignedWords.filter { it.phonemes.isNotEmpty() }
    assertTrue("Aligned words should have phoneme segments breakdown", wordsWithPhonemes.isNotEmpty())

    val allPhonemes = eval.alignedWords.flatMap { it.phonemes }
    assertTrue("Multiple phonemes must be identified", allPhonemes.size >= 10)
    assertTrue("Phonemes must have start and end timestamps", allPhonemes.all { it.absoluteEndMs >= it.absoluteStartMs })
  }

  @Test
  fun autonomousAdaptivePracticeLoop_generatesDrillAndEvaluatesAttempt() {
    val speech = ContrastiveDatasetRepository.speeches.first()
    val eval = ContrastiveSpeechAnalyticsEngine.evaluateContrastivePair(speech, SpectrumTier.TIER_4_BOTCHED)

    val drill = eval.adaptiveDrill
    assertTrue("Adaptive drill must be synthesized for flawed speech", drill != null)
    assertTrue("Drill tongue-twister must not be blank", drill!!.tongueTwister.isNotBlank())
    assertTrue("Drill shortfall diagnosis category must be present", drill.shortfall.category.isNotBlank())
    assertTrue("Drill target phoneme must be set", drill.shortfall.targetPhonemes.isNotBlank())
    assertTrue("Drill cadence hint must be populated", drill.rhythmicCadenceHint.isNotBlank())

    // Simulate an oral practice attempt with synthetic audio (PCM buffer)
    val synthAttempt = ShortArray(16000 * 3) { (kotlin.math.sin(it * 0.05) * 8000).toInt().toShort() }
    val attemptResult = com.example.dsp.AdvancedSpeechMetricsEngine.evaluatePracticeDrillAttempt(drill, synthAttempt)

    assertTrue("Attempt score must be between 0 and 100", attemptResult.pronunciationScore in 0f..100f)
    assertTrue("Attempt feedback must not be blank", attemptResult.feedbackComment.isNotBlank())
  }

  @Test
  fun fluencyScoringMatrix_calculatesGranularSubmetricsAccurately() {
    val speech = ContrastiveDatasetRepository.speeches.first()
    val eval = ContrastiveSpeechAnalyticsEngine.evaluateContrastivePair(speech, SpectrumTier.TIER_1_NEAR_PERFECT)

    val matrix = eval.rubric.fluencyMatrix
    assertTrue("Fluency scoring matrix must be computed", matrix != null)
    assertTrue("Pitch variance score must be in 0..100 range", matrix!!.pitchVariance.scorePercent in 0f..100f)
    assertTrue("Pacing speed score must be in 0..100 range", matrix.pacingSpeedWpm.scorePercent in 0f..100f)
    assertTrue("Accent clarity score must be in 0..100 range", matrix.accentFormantClarity.scorePercent in 0f..100f)
    assertTrue("Composite score must be in 0..100 range", matrix.compositeFluencyScore in 0f..100f)
    assertTrue("Fluency grade must be assigned", matrix.overallConfidenceGrade.isNotBlank())
  }
}

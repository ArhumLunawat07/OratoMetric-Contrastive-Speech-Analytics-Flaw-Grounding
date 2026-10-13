package com.example.model

enum class OratoryCategory(val label: String, val badgeCode: String) {
  PERSUASIVE_ORATORY("Persuasive Oratory", "ORATORY"),
  DECLAMATION("Dramatic Declamation", "DECLAM"),
  EXTEMPORANEOUS_DEBATE("Championship Debate", "DEBATE"),
  INTERPRETIVE_READING("Interpretive Reading", "INTERP")
}

enum class SpectrumTier(
  val level: Int,
  val shortLabel: String,
  val fullTitle: String,
  val description: String,
  val expectedDegradation: Float
) {
  TIER_0_IDEAL(
    level = 0,
    shortLabel = "T0 • Ideal Baseline",
    fullTitle = "Tier 0: Master Oratory Baseline (Reference)",
    description = "Authentic prosodic F0 arc, deliberate rhetorical pause framing, crisp formant articulation, and sustained climax energy.",
    expectedDegradation = 0.02f
  ),
  TIER_1_NEAR_PERFECT(
    level = 1,
    shortLabel = "T1 • Near-Perfect",
    fullTitle = "Tier 1: Subtle Pre-Climax Rush (Minor Flaw)",
    description = "Strong overall delivery, but rushes the rhetorical pivot (+26% local WPM) and clips the dramatic suspension pause.",
    expectedDegradation = 0.16f
  ),
  TIER_2_MODERATE_MONOTONE(
    level = 2,
    shortLabel = "T2 • Monotone Drift",
    fullTitle = "Tier 2: Pitch Compression & Skipped Pause",
    description = "Flattened F0 inflection contour across emphatic stress tokens (σ_F0 compressed by 62%) and omitted clause pause.",
    expectedDegradation = 0.34f
  ),
  TIER_3_SEVERE_CLUTTER(
    level = 3,
    shortLabel = "T3 • Erratic Clutter",
    fullTitle = "Tier 3: Tempo Cluttering & Trailing Energy Drop",
    description = "Severe pacing acceleration on multi-syllabic phrases paired with a -7.8 dB subglottal pressure fade on clause endings.",
    expectedDegradation = 0.56f
  ),
  TIER_4_BOTCHED(
    level = 4,
    shortLabel = "T4 • Botched Collapse",
    fullTitle = "Tier 4: Complete Prosodic & Spectral Collapse",
    description = "Robotic flatline pitch, zero rhetorical pauses, slurred formant transitions (high MFCC distance), and erratic volume spikes.",
    expectedDegradation = 0.78f
  ),
  CUSTOM_SYNTH_OR_LIVE(
    level = 5,
    shortLabel = "Custom / Live Take",
    fullTitle = "Custom Stress-Test Perturbation / Live Recording",
    description = "User-recorded microphone audio, imported WAV, or custom parametric stress-test perturbation.",
    expectedDegradation = 0.40f
  )
}

enum class RhetoricalRole(val label: String) {
  ANAPHORA_ANCHOR("Anaphora Anchor"),
  SETUP_CLAUSE("Setup Clause"),
  CONTRAST_PIVOT("Contrast Pivot"),
  SUSPENSION_BRIDGE("Suspension Bridge"),
  CLIMAX_STRESS("Climax Stress"),
  CADENCE_RESOLUTION("Cadence Resolution")
}

data class TranscriptTokenSpec(
  val index: Int,
  val text: String,
  val syllableCount: Int,
  val rhetoricalRole: RhetoricalRole,
  val targetDurationMs: Int,
  val targetPauseAfterMs: Int,
  val targetSemitoneOffset: Float,
  val targetPitchSlope: Float,
  val targetPitchModulationDepth: Float,
  val targetEnergyDbOffset: Float,
  val formantF1Hz: Float,
  val formantF2Hz: Float,
  val sibilanceWeight: Float
)

data class BaselineSpeechSpec(
  val id: String,
  val title: String,
  val speaker: String,
  val eventAndYear: String,
  val category: OratoryCategory,
  val rhetoricalContext: String,
  val baselineSpeakerMedianF0Hz: Float,
  val participantSpeakerMedianF0Hz: Float,
  val tokens: List<TranscriptTokenSpec>
) {
  val fullTranscript: String
    get() = tokens.joinToString(" ") { it.text }
}

data class PerturbationConfig(
  val pitchVarianceScale: Float = 1.0f,
  val tempoMultiplier: Float = 1.0f,
  val pauseRetentionRatio: Float = 1.0f,
  val trailingEnergyDropDb: Float = 0.0f,
  val spectralSlurAmount: Float = 0.0f,
  val localizeToClimaxOnly: Boolean = false,
  val simulateDifferentBiologyPitchHz: Float = 185f
)

data class AcousticFrame(
  val frameIndex: Int,
  val timestampMs: Int,
  val rmsEnergyDb: Float,
  val normalizedEnergyZ: Float,
  val f0Hz: Float,
  val voicedConfidence: Float,
  val normalizedPitchSemitones: Float,
  val normalizedPitchZ: Float,
  val spectralCentroidHz: Float,
  val spectralFlatness: Float,
  val zeroCrossingRate: Float,
  val mfcc: FloatArray,
  val localSyllableRateHz: Float
)

enum class PhonemeStatus(val label: String) {
  ACCURATE("Accurate Pronunciation"),
  PITCH_DRIFT("Pitch / Intonation Drift"),
  TEMPO_RUSHED("Cadence Rushed"),
  SLURRED_OR_FLAWED("Formant Slur / Deformation")
}

data class PhonemeSegment(
  val symbol: String,
  val ipa: String,
  val phoneticType: String,
  val relativeWordStartMs: Int,
  val relativeWordEndMs: Int,
  val absoluteStartMs: Int,
  val absoluteEndMs: Int,
  val accuracyScore: Float,
  val status: PhonemeStatus,
  val pitchDeviationSemitones: Float,
  val formantClarityScore: Float,
  val articulatoryTip: String
)

data class SpectrogramSlice(
  val timestampMs: Int,
  val frequencyBins: FloatArray
)

data class SpectrogramVisualTrack(
  val durationMs: Int,
  val slices: List<SpectrogramSlice>,
  val waveformEnvelope: FloatArray
)

data class GranularMetricItem(
  val metricKey: String,
  val label: String,
  val userValueFormatted: String,
  val benchmarkValueFormatted: String,
  val deltaFormatted: String,
  val scorePercent: Float,
  val statusGrade: String,
  val isPassing: Boolean,
  val description: String,
  val actionableAdvice: String
)

data class FluencyScoringMatrix(
  val pitchVariance: GranularMetricItem,
  val pacingSpeedWpm: GranularMetricItem,
  val accentFormantClarity: GranularMetricItem,
  val vocalEnergyProjection: GranularMetricItem,
  val pauseFramingRhythm: GranularMetricItem,
  val compositeFluencyScore: Float,
  val overallConfidenceGrade: String
)

data class PhoneticShortfall(
  val targetPhonemes: String,
  val category: String,
  val detectedShortfallSummary: String,
  val acousticEvidence: String,
  val drillObjective: String
)

data class AdaptivePracticeDrill(
  val id: String,
  val shortfall: PhoneticShortfall,
  val tongueTwister: String,
  val rhythmicCadenceHint: String,
  val focusSyllables: List<String>,
  val targetWpm: Int,
  val targetMinPitchVarianceSemitones: Float,
  val coachingExplanation: String
)

data class AdaptivePracticeAttemptResult(
  val drillId: String,
  val recordedDurationMs: Int,
  val pronunciationScore: Float,
  val pacingWpm: Int,
  val pitchVarianceSemitones: Float,
  val improvementDeltaPercent: Float,
  val feedbackComment: String
)

data class AlignedWordInterval(
  val wordIndex: Int,
  val word: String,
  val syllableCount: Int,
  val rhetoricalRole: RhetoricalRole,
  val baselineStartMs: Int,
  val baselineEndMs: Int,
  val participantStartMs: Int,
  val participantEndMs: Int,
  val durationRatio: Float,
  val localWpmBaseline: Float,
  val localWpmParticipant: Float,
  val pauseAfterBaselineMs: Int,
  val pauseAfterParticipantMs: Int,
  val meanPitchSemitonesBaseline: Float,
  val meanPitchSemitonesParticipant: Float,
  val pitchStdDevBaseline: Float,
  val pitchStdDevParticipant: Float,
  val meanEnergyZBaseline: Float,
  val meanEnergyZParticipant: Float,
  val mfccCosineDistance: Float,
  val spectralFlatnessDelta: Float,
  val alignmentHealthScore: Float,
  val phonemes: List<PhonemeSegment> = emptyList()
) {
  val baselineDurationMs: Int
    get() = (baselineEndMs - baselineStartMs).coerceAtLeast(1)

  val participantDurationMs: Int
    get() = (participantEndMs - participantStartMs).coerceAtLeast(1)
}

enum class FlawCategory(
  val title: String,
  val shortTag: String,
  val metricUnit: String
) {
  MONOTONE_PITCH_COMPRESSION(
    title = "Monotone Pitch Compression",
    shortTag = "F0 FLATLINE",
    metricUnit = "Semitones (σ_F0)"
  ),
  RUSHED_CADENCE_SPIKE(
    title = "Rushed Cadence & Tempo Spike",
    shortTag = "PACE RUSH",
    metricUnit = "WPM / Duration Ratio"
  ),
  DRAGGING_TEMPO_LAG(
    title = "Dragging Tempo & Syllable Drawl",
    shortTag = "TEMPO DRAG",
    metricUnit = "Duration Ratio (r_d)"
  ),
  MISSING_RHETORICAL_PAUSE(
    title = "Omitted Rhetorical Pause Framing",
    shortTag = "PAUSE CLIP",
    metricUnit = "Pause Interval (ms)"
  ),
  DYNAMIC_ENERGY_DROPOUT(
    title = "Trailing Energy Fade / Dynamic Mismatch",
    shortTag = "ENERGY DROP",
    metricUnit = "Normalized RMS (Z_E)"
  ),
  ARTICULATION_SPECTRAL_SLUR(
    title = "Spectral Slurring & Formant Blur",
    shortTag = "MFCC SLUR",
    metricUnit = "Cepstral Cosine Dist"
  )
}

enum class FlawSeverity(val label: String, val weightPenalty: Float) {
  MINOR("Minor Deviation", 1.0f),
  MODERATE("Moderate Flaw", 2.2f),
  CRITICAL("Critical Delivery Failure", 3.8f)
}

data class GroundedFlawRegion(
  val id: String,
  val category: FlawCategory,
  val severity: FlawSeverity,
  val participantStartMs: Int,
  val participantEndMs: Int,
  val baselineStartMs: Int,
  val baselineEndMs: Int,
  val wordStartIndex: Int,
  val wordEndIndex: Int,
  val anchoredPhrase: String,
  val headline: String,
  val mathematicalRationale: String,
  val baselineMetricSummary: String,
  val participantMetricSummary: String,
  val rubricPointDeduction: Float,
  val causalRhetoricalImpact: String,
  val actionableCoachingDrill: String
)

data class SpeakerNormalizationStats(
  val baselineMedianF0Hz: Float,
  val participantMedianF0Hz: Float,
  val biologicalPitchOffsetSemitones: Float,
  val baselineMeanEnergyDb: Float,
  val participantMeanEnergyDb: Float,
  val gainCalibrationOffsetDb: Float,
  val invarianceVerified: Boolean
)

data class RubricBreakdown(
  val overallScore: Float,
  val pitchExpressivenessScore: Float,
  val cadencePacingScore: Float,
  val pauseFramingScore: Float,
  val dynamicEnergyScore: Float,
  val vocalClarityScore: Float,
  val normalizationStats: SpeakerNormalizationStats,
  val fluencyMatrix: FluencyScoringMatrix? = null
)

data class ContrastiveEvaluationResult(
  val speechSpec: BaselineSpeechSpec,
  val tier: SpectrumTier,
  val perturbationConfig: PerturbationConfig,
  val baselineFrames: List<AcousticFrame>,
  val participantFrames: List<AcousticFrame>,
  val dtwWarpPath: List<Pair<Int, Int>>,
  val dtwNormalizedCost: Float,
  val alignedWords: List<AlignedWordInterval>,
  val groundedFlaws: List<GroundedFlawRegion>,
  val rubric: RubricBreakdown,
  val baselineAudioPcm: ShortArray,
  val participantAudioPcm: ShortArray,
  val sampleRate: Int = 16000,
  val baselineDurationMs: Int,
  val participantDurationMs: Int,
  val baselineSpectrogramTrack: SpectrogramVisualTrack? = null,
  val participantSpectrogramTrack: SpectrogramVisualTrack? = null,
  val adaptiveDrill: AdaptivePracticeDrill? = null
)

data class StressTestPoint(
  val speechId: String,
  val speechTitle: String,
  val tier: SpectrumTier,
  val injectedFlawSeverity: Float,
  val evaluatedOverallScore: Float,
  val pitchScore: Float,
  val pacingScore: Float,
  val pauseScore: Float,
  val energyScore: Float,
  val clarityScore: Float,
  val groundedFlawCount: Int,
  val temporalGroundingIoU: Float
)

data class StressTestSuiteReport(
  val points: List<StressTestPoint>,
  val monotonicitySpearmanRho: Float,
  val meanTemporalGroundingIoU: Float,
  val speakerInvarianceMaxDriftPoints: Float,
  val totalPairsEvaluated: Int
)

package com.example.data

import com.example.model.BaselineSpeechSpec
import com.example.model.OratoryCategory
import com.example.model.PerturbationConfig
import com.example.model.RhetoricalRole
import com.example.model.SpectrumTier
import com.example.model.TranscriptTokenSpec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

object ContrastiveDatasetRepository {

  const val SAMPLE_RATE = 16000

  val speeches: List<BaselineSpeechSpec> = listOf(
    buildJfkMoonSpeech(),
    buildMlkDreamSpeech(),
    buildObamaCreedSpeech(),
    buildAngelouRiseSpeech()
  )

  fun getPerturbationForTier(tier: SpectrumTier, custom: PerturbationConfig): PerturbationConfig {
    return when (tier) {
      SpectrumTier.TIER_0_IDEAL -> PerturbationConfig(
        pitchVarianceScale = 1.0f,
        tempoMultiplier = 1.0f,
        pauseRetentionRatio = 1.0f,
        trailingEnergyDropDb = 0.0f,
        spectralSlurAmount = 0.0f,
        localizeToClimaxOnly = false,
        simulateDifferentBiologyPitchHz = 178f // Different biological voice pitch to prove speaker-invariance
      )
      SpectrumTier.TIER_1_NEAR_PERFECT -> PerturbationConfig(
        pitchVarianceScale = 0.94f,
        tempoMultiplier = 0.76f, // Rushes climax pivot by ~24%
        pauseRetentionRatio = 0.44f, // Slightly clips dramatic suspension pause
        trailingEnergyDropDb = 0.6f,
        spectralSlurAmount = 0.03f,
        localizeToClimaxOnly = true,
        simulateDifferentBiologyPitchHz = 182f
      )
      SpectrumTier.TIER_2_MODERATE_MONOTONE -> PerturbationConfig(
        pitchVarianceScale = 0.42f, // Moderate monotone compression across emphasis tokens
        tempoMultiplier = 0.84f,
        pauseRetentionRatio = 0.36f, // Skips key rhetorical pauses
        trailingEnergyDropDb = 1.8f,
        spectralSlurAmount = 0.10f,
        localizeToClimaxOnly = false,
        simulateDifferentBiologyPitchHz = 168f
      )
      SpectrumTier.TIER_3_SEVERE_CLUTTER -> PerturbationConfig(
        pitchVarianceScale = 0.45f,
        tempoMultiplier = 0.58f, // Severe cluttering rush (high WPM spike)
        pauseRetentionRatio = 0.18f,
        trailingEnergyDropDb = 8.4f, // Severe subglottal trailing fade on climax/resolution
        spectralSlurAmount = 0.35f,
        localizeToClimaxOnly = false,
        simulateDifferentBiologyPitchHz = 190f
      )
      SpectrumTier.TIER_4_BOTCHED -> PerturbationConfig(
        pitchVarianceScale = 0.10f, // Robotic flatline F0
        tempoMultiplier = 0.52f, // Extreme rush on setup + erratic drag
        pauseRetentionRatio = 0.05f, // Zero pause framing
        trailingEnergyDropDb = 11.2f, // Collapse of vocal support
        spectralSlurAmount = 0.68f, // Heavy formant slurring / mumble
        localizeToClimaxOnly = false,
        simulateDifferentBiologyPitchHz = 160f
      )
      SpectrumTier.CUSTOM_SYNTH_OR_LIVE -> custom
    }
  }

  data class SynthesizedTake(
    val pcmSamples: ShortArray,
    val groundTruthWordBoundsMs: List<Pair<Int, Int>>,
    val groundTruthPausesMs: List<Int>,
    val expectedFlawIntervalsMs: List<Pair<Int, Int>>
  )

  /**
   * Synthesizes a realistic 16kHz mono PCM speech signal using a multi-harmonic Rosenberg
   * glottal source filtered through time-varying 2-formant (F1/F2) resonators, sibilant
   * fricative bursts, and syllable amplitude envelopes.
   */
  fun synthesizeSpeechTake(
    speech: BaselineSpeechSpec,
    isBaseline: Boolean,
    tier: SpectrumTier,
    config: PerturbationConfig
  ): SynthesizedTake {
    val baseF0 = if (isBaseline) {
      speech.baselineSpeakerMedianF0Hz
    } else {
      config.simulateDifferentBiologyPitchHz
    }

    val wordBounds = mutableListOf<Pair<Int, Int>>()
    val pauseList = mutableListOf<Int>()
    val flawIntervals = mutableListOf<Pair<Int, Int>>()

    // First calculate exact durations so we can allocate PCM buffer cleanly
    data class TokenRenderPlan(
      val token: TranscriptTokenSpec,
      val durationMs: Int,
      val pauseMs: Int,
      val pitchSemitoneTarget: Float,
      val pitchSlope: Float,
      val modDepth: Float,
      val energyDbOffset: Float,
      val slurFactor: Float,
      val isPerturbedFlaw: Boolean
    )

    val plans = speech.tokens.map { token ->
      if (isBaseline) {
        TokenRenderPlan(
          token = token,
          durationMs = token.targetDurationMs,
          pauseMs = token.targetPauseAfterMs,
          pitchSemitoneTarget = token.targetSemitoneOffset,
          pitchSlope = token.targetPitchSlope,
          modDepth = token.targetPitchModulationDepth,
          energyDbOffset = token.targetEnergyDbOffset,
          slurFactor = 0f,
          isPerturbedFlaw = false
        )
      } else {
        val isTargetRegion = if (config.localizeToClimaxOnly) {
          token.rhetoricalRole == RhetoricalRole.CONTRAST_PIVOT ||
            token.rhetoricalRole == RhetoricalRole.SUSPENSION_BRIDGE ||
            token.rhetoricalRole == RhetoricalRole.CLIMAX_STRESS
        } else {
          token.rhetoricalRole != RhetoricalRole.SETUP_CLAUSE || tier.level >= 3
        }

        val localTempoMult = if (isTargetRegion) {
          if (tier == SpectrumTier.TIER_4_BOTCHED && token.rhetoricalRole == RhetoricalRole.CADENCE_RESOLUTION) {
            1.62f // Erratic drag at the end of botched take
          } else {
            config.tempoMultiplier
          }
        } else {
          1.0f
        }

        val localPitchScale = if (isTargetRegion) config.pitchVarianceScale else 0.9f
        val localPauseRetain = if (isTargetRegion || token.targetPauseAfterMs >= 220) {
          config.pauseRetentionRatio
        } else {
          0.9f
        }

        val isTrailingClause = token.rhetoricalRole == RhetoricalRole.CLIMAX_STRESS ||
          token.rhetoricalRole == RhetoricalRole.CADENCE_RESOLUTION
        val energyDrop = if (isTargetRegion && isTrailingClause) {
          -config.trailingEnergyDropDb
        } else {
          0f
        }

        val slur = if (isTargetRegion) config.spectralSlurAmount else config.spectralSlurAmount * 0.3f

        val durMs = (token.targetDurationMs * localTempoMult).toInt().coerceAtLeast(90)
        val pMs = (token.targetPauseAfterMs * localPauseRetain).toInt().coerceAtLeast(15)

        val hasSubstantialFlaw = isTargetRegion && (
          kotlin.math.abs(1f - localTempoMult) > 0.22f ||
            localPitchScale < 0.55f ||
            (token.targetPauseAfterMs >= 240 && pMs < 100) ||
            energyDrop < -4.0f ||
            slur > 0.25f
          )

        TokenRenderPlan(
          token = token,
          durationMs = durMs,
          pauseMs = pMs,
          pitchSemitoneTarget = token.targetSemitoneOffset * localPitchScale,
          pitchSlope = token.targetPitchSlope * localPitchScale,
          modDepth = token.targetPitchModulationDepth * localPitchScale,
          energyDbOffset = token.targetEnergyDbOffset + energyDrop,
          slurFactor = slur,
          isPerturbedFlaw = hasSubstantialFlaw
        )
      }
    }

    val totalDurationMs = plans.sumOf { it.durationMs + it.pauseMs } + 160
    val totalSamples = (totalDurationMs * SAMPLE_RATE) / 1000
    val pcm = ShortArray(totalSamples)

    var cursorMs = 80
    var phase = 0.0
    val rng = Random(speech.id.hashCode() + tier.level * 97 + (if (isBaseline) 0 else 1337))

    for (plan in plans) {
      val startMs = cursorMs
      val endMs = startMs + plan.durationMs
      wordBounds.add(startMs to endMs)
      pauseList.add(plan.pauseMs)

      if (plan.isPerturbedFlaw) {
        flawIntervals.add(startMs to (endMs + plan.pauseMs))
      }

      val startSample = (startMs * SAMPLE_RATE) / 1000
      val endSample = ((endMs * SAMPLE_RATE) / 1000).coerceAtMost(totalSamples)
      val wordSamples = (endSample - startSample).coerceAtLeast(1)

      val amplitudeLinear = 10.0.pow(plan.energyDbOffset / 20.0).toFloat().coerceIn(0.12f, 2.2f)
      val syllables = plan.token.syllableCount.coerceAtLeast(1)

      // Formant frequencies shifted toward neutral schwa (500Hz, 1500Hz) when slurred
      val targetF1 = plan.token.formantF1Hz * (1f - 0.55f * plan.slurFactor) + 500f * (0.55f * plan.slurFactor)
      val targetF2 = plan.token.formantF2Hz * (1f - 0.65f * plan.slurFactor) + 1480f * (0.65f * plan.slurFactor)

      for (i in 0 until wordSamples) {
        val progress = i.toFloat() / wordSamples.toFloat()

        // Syllable nucleus modulation envelope (creates realistic syllable rate peaks)
        val sylPhase = progress * syllables * PI
        val sylEnvelope = (0.38 + 0.62 * sin(sylPhase).pow(2.0)).toFloat()

        // Smooth attack/release at word boundaries
        val edgeEnv = when {
          progress < 0.08f -> progress / 0.08f
          progress > 0.90f -> (1f - progress) / 0.10f
          else -> 1f
        }

        // Instantaneous F0 contour in semitones -> Hz
        val stInstant = plan.pitchSemitoneTarget +
          plan.pitchSlope * (progress - 0.5f) +
          plan.modDepth * sin(2.0 * PI * progress * syllables.coerceAtLeast(1)).toFloat()

        val f0InstantHz = (baseF0 * 2.0.pow(stInstant / 12.0)).toFloat().coerceIn(75f, 420f)
        phase += (2.0 * PI * f0InstantHz) / SAMPLE_RATE

        // Multi-harmonic glottal pulse with formant resonance weighting
        var voicedSignal = 0.0
        val maxHarmonic = (3800f / f0InstantHz).toInt().coerceIn(4, 24)
        for (h in 1..maxHarmonic) {
          val freqH = h * f0InstantHz
          // Dual formant resonance transfer function + spectral tilt
          val d1 = (freqH - targetF1) / (95f + 160f * plan.slurFactor)
          val d2 = (freqH - targetF2) / (150f + 240f * plan.slurFactor)
          val resonance1 = 1.6 / (1.0 + d1 * d1)
          val resonance2 = 1.15 / (1.0 + d2 * d2)
          val spectralTilt = 1.0 / h.toDouble().pow(0.85 + 0.65 * plan.slurFactor)
          val harmGain = (0.35 + resonance1 + resonance2) * spectralTilt
          voicedSignal += harmGain * sin(h * phase)
        }

        // Add consonant sibilance at onset/offset or breath noise if slurred
        val isConsonantEdge = progress < 0.12f || progress > 0.86f
        val sibilanceAmp = if (isConsonantEdge) {
          plan.token.sibilanceWeight * (1f - 0.5f * plan.slurFactor)
        } else {
          0.02f + 0.22f * plan.slurFactor // Vocal breathiness / spectral flatness noise when slurred
        }
        val noise = (rng.nextFloat() * 2f - 1f) * sibilanceAmp

        val sampleVal = ((voicedSignal * 0.36f + noise) * sylEnvelope * edgeEnv * amplitudeLinear * 9200f)
          .toInt()
          .coerceIn(-31000, 31000)

        pcm[startSample + i] = sampleVal.toShort()
      }

      // Low-level room floor during pauses so log-energy and FFT stay numerically well-conditioned
      val pauseStartSample = endSample
      val pauseEndSample = (((endMs + plan.pauseMs) * SAMPLE_RATE) / 1000).coerceAtMost(totalSamples)
      for (s in pauseStartSample until pauseEndSample) {
        pcm[s] = ((rng.nextFloat() * 2f - 1f) * 28f).toInt().toShort()
      }

      cursorMs = endMs + plan.pauseMs
    }

    // Merge contiguous ground-truth flaw intervals
    val mergedFlaws = mergeIntervals(flawIntervals)
    return SynthesizedTake(
      pcmSamples = pcm,
      groundTruthWordBoundsMs = wordBounds,
      groundTruthPausesMs = pauseList,
      expectedFlawIntervalsMs = mergedFlaws
    )
  }

  private fun mergeIntervals(raw: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
    if (raw.isEmpty()) return emptyList()
    val sorted = raw.sortedBy { it.first }
    val result = mutableListOf<Pair<Int, Int>>()
    var current = sorted.first()
    for (i in 1 until sorted.size) {
      val next = sorted[i]
      if (next.first <= current.second + 180) {
        current = current.first to maxOf(current.second, next.second)
      } else {
        result.add(current)
        current = next
      }
    }
    result.add(current)
    return result
  }

  private fun buildJfkMoonSpeech(): BaselineSpeechSpec {
    val rawTokens = listOf(
      t(0, "We", 1, RhetoricalRole.ANAPHORA_ANCHOR, 240, 40, 1.2f, 0.8f, 0.6f, 1.5f, 320f, 2250f, 0.08f),
      t(1, "choose", 1, RhetoricalRole.ANAPHORA_ANCHOR, 380, 60, 3.4f, 1.5f, 1.1f, 3.2f, 360f, 1100f, 0.28f),
      t(2, "to", 1, RhetoricalRole.SETUP_CLAUSE, 150, 20, 0.5f, -0.3f, 0.3f, -0.5f, 420f, 1350f, 0.14f),
      t(3, "go", 1, RhetoricalRole.SETUP_CLAUSE, 290, 40, 1.8f, 0.6f, 0.7f, 1.4f, 460f, 1020f, 0.10f),
      t(4, "to", 1, RhetoricalRole.SETUP_CLAUSE, 140, 20, 0.2f, -0.2f, 0.3f, -0.8f, 420f, 1350f, 0.12f),
      t(5, "the", 1, RhetoricalRole.SETUP_CLAUSE, 150, 30, 0.0f, 0.0f, 0.2f, -1.0f, 500f, 1450f, 0.10f),
      t(6, "moon", 1, RhetoricalRole.CLIMAX_STRESS, 460, 340, 4.2f, -1.2f, 1.4f, 3.8f, 340f, 950f, 0.06f),
      t(7, "in", 1, RhetoricalRole.SETUP_CLAUSE, 170, 25, 0.4f, 0.2f, 0.4f, 0.2f, 400f, 1950f, 0.08f),
      t(8, "this", 1, RhetoricalRole.SETUP_CLAUSE, 240, 30, 1.4f, 0.5f, 0.5f, 1.0f, 410f, 1880f, 0.26f),
      t(9, "decade", 2, RhetoricalRole.SETUP_CLAUSE, 430, 290, 2.6f, -0.8f, 1.0f, 2.2f, 560f, 1780f, 0.16f),
      t(10, "and", 1, RhetoricalRole.SETUP_CLAUSE, 190, 30, -0.4f, 0.1f, 0.3f, -0.4f, 620f, 1620f, 0.08f),
      t(11, "do", 1, RhetoricalRole.SETUP_CLAUSE, 240, 30, 1.1f, 0.4f, 0.5f, 1.1f, 350f, 1080f, 0.10f),
      t(12, "the", 1, RhetoricalRole.SETUP_CLAUSE, 140, 20, 0.2f, 0.0f, 0.2f, -0.6f, 500f, 1480f, 0.09f),
      t(13, "other", 2, RhetoricalRole.SETUP_CLAUSE, 320, 35, 1.6f, 0.6f, 0.7f, 1.3f, 590f, 1380f, 0.12f),
      t(14, "things", 1, RhetoricalRole.SUSPENSION_BRIDGE, 420, 440, 2.2f, -1.0f, 0.9f, 2.0f, 410f, 1980f, 0.30f),
      t(15, "not", 1, RhetoricalRole.CONTRAST_PIVOT, 340, 70, 3.8f, 1.4f, 1.2f, 3.5f, 680f, 1180f, 0.18f),
      t(16, "because", 2, RhetoricalRole.CONTRAST_PIVOT, 390, 40, 2.1f, -0.4f, 0.8f, 2.0f, 540f, 1580f, 0.22f),
      t(17, "they", 1, RhetoricalRole.CONTRAST_PIVOT, 210, 30, 1.0f, 0.2f, 0.5f, 1.2f, 480f, 2050f, 0.10f),
      t(18, "are", 1, RhetoricalRole.CONTRAST_PIVOT, 200, 35, 0.6f, -0.3f, 0.4f, 0.8f, 690f, 1280f, 0.08f),
      t(19, "easy", 2, RhetoricalRole.SUSPENSION_BRIDGE, 450, 480, 3.2f, -1.8f, 1.3f, 2.6f, 310f, 2340f, 0.28f),
      t(20, "but", 1, RhetoricalRole.CONTRAST_PIVOT, 260, 60, 2.5f, 0.9f, 0.8f, 2.8f, 610f, 1340f, 0.16f),
      t(21, "because", 2, RhetoricalRole.CLIMAX_STRESS, 410, 50, 3.6f, 1.1f, 1.1f, 3.4f, 540f, 1620f, 0.22f),
      t(22, "they", 1, RhetoricalRole.CLIMAX_STRESS, 240, 40, 2.8f, 0.5f, 0.8f, 3.0f, 470f, 2080f, 0.10f),
      t(23, "are", 1, RhetoricalRole.CLIMAX_STRESS, 250, 65, 2.4f, 0.4f, 0.7f, 3.2f, 700f, 1290f, 0.08f),
      t(24, "hard", 1, RhetoricalRole.CADENCE_RESOLUTION, 580, 360, 4.8f, -2.6f, 1.7f, 4.6f, 740f, 1220f, 0.18f)
    )
    return BaselineSpeechSpec(
      id = "jfk_moon_1962",
      title = "We Choose to Go to the Moon",
      speaker = "John F. Kennedy",
      eventAndYear = "Rice University • 1962",
      category = OratoryCategory.PERSUASIVE_ORATORY,
      rhetoricalContext = "Antithetical contrast ('not because they are easy, but because they are hard') requiring a 440–480ms suspension pause and high F0 dynamic inflection on the climax.",
      baselineSpeakerMedianF0Hz = 132f,
      participantSpeakerMedianF0Hz = 178f,
      tokens = rawTokens
    )
  }

  private fun buildMlkDreamSpeech(): BaselineSpeechSpec {
    val rawTokens = listOf(
      t(0, "I", 1, RhetoricalRole.ANAPHORA_ANCHOR, 280, 40, 2.4f, 0.8f, 0.9f, 2.6f, 720f, 1240f, 0.06f),
      t(1, "have", 1, RhetoricalRole.ANAPHORA_ANCHOR, 260, 35, 3.1f, 0.6f, 0.8f, 2.8f, 660f, 1680f, 0.14f),
      t(2, "a", 1, RhetoricalRole.ANAPHORA_ANCHOR, 140, 25, 1.8f, 0.0f, 0.3f, 1.2f, 520f, 1450f, 0.06f),
      t(3, "dream", 1, RhetoricalRole.CLIMAX_STRESS, 540, 420, 4.9f, -1.6f, 1.6f, 4.4f, 310f, 2280f, 0.12f),
      t(4, "that", 1, RhetoricalRole.SETUP_CLAUSE, 180, 30, 0.6f, 0.0f, 0.4f, 0.4f, 580f, 1640f, 0.12f),
      t(5, "my", 1, RhetoricalRole.SETUP_CLAUSE, 220, 30, 1.2f, 0.3f, 0.5f, 1.0f, 700f, 1320f, 0.06f),
      t(6, "four", 1, RhetoricalRole.SETUP_CLAUSE, 310, 40, 2.2f, 0.5f, 0.8f, 2.0f, 450f, 920f, 0.18f),
      t(7, "little", 2, RhetoricalRole.SETUP_CLAUSE, 340, 35, 1.8f, -0.4f, 0.7f, 1.5f, 410f, 1860f, 0.14f),
      t(8, "children", 2, RhetoricalRole.SUSPENSION_BRIDGE, 460, 320, 2.9f, -1.1f, 1.1f, 2.5f, 430f, 1920f, 0.24f),
      t(9, "will", 1, RhetoricalRole.SETUP_CLAUSE, 200, 25, 0.8f, 0.2f, 0.4f, 0.8f, 400f, 1820f, 0.08f),
      t(10, "not", 1, RhetoricalRole.CONTRAST_PIVOT, 320, 55, 3.5f, 1.2f, 1.1f, 3.2f, 670f, 1190f, 0.16f),
      t(11, "be", 1, RhetoricalRole.CONTRAST_PIVOT, 210, 30, 2.0f, 0.2f, 0.5f, 1.8f, 300f, 2320f, 0.08f),
      t(12, "judged", 1, RhetoricalRole.CONTRAST_PIVOT, 420, 260, 3.4f, -1.2f, 1.2f, 3.0f, 560f, 1420f, 0.28f),
      t(13, "by", 1, RhetoricalRole.SETUP_CLAUSE, 190, 25, 0.5f, 0.1f, 0.4f, 0.6f, 690f, 1310f, 0.08f),
      t(14, "the", 1, RhetoricalRole.SETUP_CLAUSE, 140, 20, 0.1f, 0.0f, 0.2f, -0.4f, 500f, 1460f, 0.08f),
      t(15, "color", 2, RhetoricalRole.CONTRAST_PIVOT, 370, 35, 2.4f, 0.6f, 0.9f, 2.2f, 580f, 1260f, 0.14f),
      t(16, "of", 1, RhetoricalRole.SETUP_CLAUSE, 150, 20, 0.3f, -0.2f, 0.3f, 0.0f, 520f, 1380f, 0.12f),
      t(17, "their", 1, RhetoricalRole.SETUP_CLAUSE, 210, 30, 0.9f, 0.1f, 0.4f, 0.8f, 530f, 1720f, 0.10f),
      t(18, "skin", 1, RhetoricalRole.SUSPENSION_BRIDGE, 460, 460, 3.0f, -1.7f, 1.2f, 2.8f, 400f, 2040f, 0.32f),
      t(19, "but", 1, RhetoricalRole.CONTRAST_PIVOT, 270, 65, 2.8f, 1.0f, 0.9f, 3.0f, 600f, 1360f, 0.16f),
      t(20, "by", 1, RhetoricalRole.CLIMAX_STRESS, 220, 35, 3.1f, 0.7f, 0.8f, 3.2f, 690f, 1340f, 0.08f),
      t(21, "the", 1, RhetoricalRole.CLIMAX_STRESS, 150, 25, 2.4f, 0.3f, 0.4f, 2.2f, 500f, 1480f, 0.08f),
      t(22, "content", 2, RhetoricalRole.CLIMAX_STRESS, 450, 50, 4.5f, 1.3f, 1.5f, 4.2f, 640f, 1560f, 0.22f),
      t(23, "of", 1, RhetoricalRole.CLIMAX_STRESS, 160, 25, 2.6f, -0.2f, 0.4f, 2.4f, 520f, 1380f, 0.12f),
      t(24, "their", 1, RhetoricalRole.CLIMAX_STRESS, 230, 40, 3.0f, 0.4f, 0.7f, 2.9f, 530f, 1740f, 0.10f),
      t(25, "character", 3, RhetoricalRole.CADENCE_RESOLUTION, 620, 380, 5.1f, -2.9f, 1.8f, 4.8f, 620f, 1690f, 0.24f)
    )
    return BaselineSpeechSpec(
      id = "mlk_dream_1963",
      title = "I Have a Dream (Character Creed)",
      speaker = "Dr. Martin Luther King Jr.",
      eventAndYear = "Lincoln Memorial • 1963",
      category = OratoryCategory.DECLAMATION,
      rhetoricalContext = "Sustained lyrical declamation with anaphoric opening, a 460ms dramatic pause after 'skin', and a soaring 5-semitone cadence arch on 'content of their character'.",
      baselineSpeakerMedianF0Hz = 142f,
      participantSpeakerMedianF0Hz = 188f,
      tokens = rawTokens
    )
  }

  private fun buildObamaCreedSpeech(): BaselineSpeechSpec {
    val rawTokens = listOf(
      t(0, "Where", 1, RhetoricalRole.SETUP_CLAUSE, 230, 30, 1.4f, 0.5f, 0.6f, 1.4f, 510f, 1780f, 0.10f),
      t(1, "we", 1, RhetoricalRole.SETUP_CLAUSE, 180, 25, 1.0f, 0.2f, 0.4f, 1.0f, 320f, 2240f, 0.06f),
      t(2, "are", 1, RhetoricalRole.SETUP_CLAUSE, 190, 25, 0.8f, 0.0f, 0.4f, 0.8f, 680f, 1280f, 0.08f),
      t(3, "met", 1, RhetoricalRole.SETUP_CLAUSE, 260, 35, 1.8f, 0.6f, 0.7f, 1.8f, 550f, 1820f, 0.16f),
      t(4, "with", 1, RhetoricalRole.SETUP_CLAUSE, 170, 25, 0.5f, -0.2f, 0.3f, 0.4f, 410f, 1880f, 0.18f),
      t(5, "cynicism", 4, RhetoricalRole.CONTRAST_PIVOT, 520, 80, 3.2f, -0.8f, 1.2f, 2.8f, 420f, 1960f, 0.34f),
      t(6, "and", 1, RhetoricalRole.SETUP_CLAUSE, 170, 25, 0.4f, 0.1f, 0.3f, 0.2f, 600f, 1600f, 0.08f),
      t(7, "doubt", 1, RhetoricalRole.SUSPENSION_BRIDGE, 420, 380, 2.8f, -1.4f, 1.1f, 2.6f, 660f, 1220f, 0.18f),
      t(8, "we", 1, RhetoricalRole.CONTRAST_PIVOT, 230, 35, 2.4f, 0.8f, 0.7f, 2.5f, 320f, 2260f, 0.08f),
      t(9, "will", 1, RhetoricalRole.CONTRAST_PIVOT, 240, 35, 2.8f, 0.6f, 0.8f, 2.8f, 400f, 1840f, 0.10f),
      t(10, "respond", 2, RhetoricalRole.CLIMAX_STRESS, 450, 70, 3.9f, 1.1f, 1.3f, 3.6f, 620f, 1180f, 0.24f),
      t(11, "with", 1, RhetoricalRole.SETUP_CLAUSE, 170, 25, 1.2f, 0.0f, 0.4f, 1.2f, 410f, 1880f, 0.16f),
      t(12, "that", 1, RhetoricalRole.SETUP_CLAUSE, 200, 30, 1.6f, 0.3f, 0.5f, 1.5f, 580f, 1660f, 0.14f),
      t(13, "timeless", 2, RhetoricalRole.CLIMAX_STRESS, 430, 50, 3.6f, 0.9f, 1.2f, 3.3f, 690f, 1480f, 0.28f),
      t(14, "creed", 1, RhetoricalRole.SUSPENSION_BRIDGE, 460, 490, 4.1f, -1.5f, 1.4f, 3.7f, 300f, 2340f, 0.16f),
      t(15, "yes", 1, RhetoricalRole.ANAPHORA_ANCHOR, 380, 140, 4.6f, 1.2f, 1.4f, 4.4f, 530f, 1940f, 0.32f),
      t(16, "we", 1, RhetoricalRole.CLIMAX_STRESS, 310, 90, 4.2f, 0.8f, 1.1f, 4.2f, 310f, 2290f, 0.08f),
      t(17, "can", 1, RhetoricalRole.CADENCE_RESOLUTION, 560, 360, 5.0f, -2.5f, 1.7f, 4.7f, 640f, 1720f, 0.14f)
    )
    return BaselineSpeechSpec(
      id = "obama_creed_2008",
      title = "That Timeless Creed: Yes We Can",
      speaker = "Barack Obama",
      eventAndYear = "New Hampshire • 2008",
      category = OratoryCategory.EXTEMPORANEOUS_DEBATE,
      rhetoricalContext = "Crisp syllabic articulation on 'cynicism', a 490ms rhetorical caesura after 'creed', and deliberate triadic stress separation on 'Yes — we — can'.",
      baselineSpeakerMedianF0Hz = 124f,
      participantSpeakerMedianF0Hz = 172f,
      tokens = rawTokens
    )
  }

  private fun buildAngelouRiseSpeech(): BaselineSpeechSpec {
    val rawTokens = listOf(
      t(0, "You", 1, RhetoricalRole.ANAPHORA_ANCHOR, 240, 30, 1.6f, 0.5f, 0.6f, 1.8f, 330f, 1060f, 0.08f),
      t(1, "may", 1, RhetoricalRole.SETUP_CLAUSE, 230, 30, 2.0f, 0.4f, 0.6f, 2.0f, 460f, 2040f, 0.08f),
      t(2, "trod", 1, RhetoricalRole.CONTRAST_PIVOT, 360, 45, 3.1f, 0.9f, 1.0f, 3.1f, 640f, 1140f, 0.22f),
      t(3, "me", 1, RhetoricalRole.SETUP_CLAUSE, 220, 30, 1.8f, -0.2f, 0.5f, 1.8f, 300f, 2310f, 0.06f),
      t(4, "in", 1, RhetoricalRole.SETUP_CLAUSE, 160, 25, 0.8f, 0.0f, 0.3f, 0.6f, 400f, 1920f, 0.08f),
      t(5, "the", 1, RhetoricalRole.SETUP_CLAUSE, 150, 25, 0.4f, 0.0f, 0.3f, 0.2f, 500f, 1480f, 0.08f),
      t(6, "very", 2, RhetoricalRole.CONTRAST_PIVOT, 350, 40, 2.6f, 0.7f, 0.9f, 2.5f, 520f, 1880f, 0.16f),
      t(7, "dirt", 1, RhetoricalRole.SUSPENSION_BRIDGE, 460, 450, 2.2f, -1.8f, 1.2f, 2.8f, 510f, 1460f, 0.24f),
      t(8, "but", 1, RhetoricalRole.CONTRAST_PIVOT, 260, 60, 2.8f, 1.1f, 0.8f, 3.0f, 600f, 1340f, 0.16f),
      t(9, "still", 1, RhetoricalRole.CLIMAX_STRESS, 390, 140, 4.0f, 1.3f, 1.3f, 3.8f, 390f, 1960f, 0.34f),
      t(10, "like", 1, RhetoricalRole.SETUP_CLAUSE, 260, 40, 2.9f, 0.4f, 0.8f, 2.8f, 680f, 1380f, 0.16f),
      t(11, "dust", 1, RhetoricalRole.SUSPENSION_BRIDGE, 430, 360, 3.5f, -0.9f, 1.1f, 3.4f, 610f, 1390f, 0.32f),
      t(12, "I'll", 1, RhetoricalRole.CLIMAX_STRESS, 310, 60, 4.3f, 1.4f, 1.2f, 4.1f, 700f, 1320f, 0.10f),
      t(13, "rise", 1, RhetoricalRole.CADENCE_RESOLUTION, 610, 380, 5.4f, 2.4f, 1.9f, 4.9f, 710f, 1480f, 0.30f)
    )
    return BaselineSpeechSpec(
      id = "angelou_rise_1978",
      title = "Still I Rise (Poetic Meter)",
      speaker = "Maya Angelou",
      eventAndYear = "And Still I Rise • 1978",
      category = OratoryCategory.INTERPRETIVE_READING,
      rhetoricalContext = "Interpretive trochaic/iambic meter requiring downward pitch resolution on 'dirt', a 450ms caesura, and an ascending +2.4 st/word inflection arc on 'I'll rise'.",
      baselineSpeakerMedianF0Hz = 165f,
      participantSpeakerMedianF0Hz = 128f,
      tokens = rawTokens
    )
  }

  private fun t(
    index: Int,
    text: String,
    syllables: Int,
    role: RhetoricalRole,
    durMs: Int,
    pauseMs: Int,
    stOffset: Float,
    slope: Float,
    modDepth: Float,
    energyDb: Float,
    f1: Float,
    f2: Float,
    sibilance: Float
  ) = TranscriptTokenSpec(
    index = index,
    text = text,
    syllableCount = syllables,
    rhetoricalRole = role,
    targetDurationMs = durMs,
    targetPauseAfterMs = pauseMs,
    targetSemitoneOffset = stOffset,
    targetPitchSlope = slope,
    targetPitchModulationDepth = modDepth,
    targetEnergyDbOffset = energyDb,
    formantF1Hz = f1,
    formantF2Hz = f2,
    sibilanceWeight = sibilance
  )
}

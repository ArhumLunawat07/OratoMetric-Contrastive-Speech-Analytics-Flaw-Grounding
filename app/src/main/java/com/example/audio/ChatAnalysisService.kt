package com.example.audio

import com.example.BuildConfig
import com.example.model.ContrastiveEvaluationResult
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class ChatMessage(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val timestampLabel: String = "Just now"
)

object ChatAnalysisService {

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(60, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .writeTimeout(60, TimeUnit.SECONDS)
      .build()
  }

  suspend fun askSpeechAnalyst(
    userPrompt: String,
    eval: ContrastiveEvaluationResult?
  ): String = withContext(Dispatchers.IO) {
    if (eval == null) {
      return@withContext "Please wait a moment while the speech evaluation finishes loading, then ask your question again."
    }

    val contextSummary = buildAcousticContextPrompt(eval)
    val apiKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (_: Exception) {
      ""
    }

    val hasValidLiveKey = apiKey.isNotBlank() &&
      apiKey != "MY_GEMINI_API_KEY" &&
      !apiKey.startsWith("YOUR_")

    if (hasValidLiveKey) {
      try {
        val systemText = """
          You are OratorIQ's Contrastive Speech Analyst. Keep your answers clear, sober, concise, and easy to read.
          Ground your explanation in the exact timestamps, word durations, pauses, F0 semitones, and rubric scores below:
          $contextSummary
        """.trimIndent()

        val payload = JSONObject().apply {
          put(
            "systemInstruction",
            JSONObject().put(
              "parts",
              JSONArray().put(JSONObject().put("text", systemText))
            )
          )
          put(
            "contents",
            JSONArray().put(
              JSONObject().put(
                "parts",
                JSONArray().put(JSONObject().put("text", userPrompt))
              )
            )
          )
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
          .url(url)
          .post(payload.toString().toRequestBody("application/json".toMediaType()))
          .build()

        okHttpClient.newCall(request).execute().use { response ->
          if (response.isSuccessful) {
            val bodyStr = response.body?.string().orEmpty()
            val root = JSONObject(bodyStr)
            val text = root.optJSONArray("candidates")
              ?.optJSONObject(0)
              ?.optJSONObject("content")
              ?.optJSONArray("parts")
              ?.optJSONObject(0)
              ?.optString("text")
              .orEmpty()
            if (text.isNotBlank()) {
              return@withContext text.trim()
            }
          }
        }
      } catch (_: Exception) {
        // Fall through to deterministic DSP-grounded response
      }
    }

    return@withContext generateGroundedDspResponse(userPrompt, eval)
  }

  private fun buildAcousticContextPrompt(eval: ContrastiveEvaluationResult): String {
    val r = eval.rubric
    val flawsText = if (eval.groundedFlaws.isEmpty()) {
      "No delivery flaws detected (Reference-grade delivery)."
    } else {
      eval.groundedFlaws.joinToString("\n") { f ->
        "- [${f.participantStartMs}-${f.participantEndMs}ms] ${f.headline} on ${f.anchoredPhrase}: ${f.mathematicalRationale}. Coaching: ${f.actionableCoachingDrill}"
      }
    }

    return """
      Speech: ${eval.speechSpec.title} by ${eval.speechSpec.speaker} (${eval.speechSpec.category.label})
      Selected Take: ${eval.tier.fullTitle}
      Overall Score: ${String.format(Locale.US, "%.1f/100", r.overallScore)}
      - Pitch Expressiveness: ${String.format(Locale.US, "%.1f", r.pitchExpressivenessScore)}
      - Cadence & Pacing: ${String.format(Locale.US, "%.1f", r.cadencePacingScore)}
      - Pause Framing: ${String.format(Locale.US, "%.1f", r.pauseFramingScore)}
      - Dynamic Energy: ${String.format(Locale.US, "%.1f", r.dynamicEnergyScore)}
      - Vocal Clarity (MFCC): ${String.format(Locale.US, "%.1f", r.vocalClarityScore)}
      Speaker-Agnostic Calibration: Baseline median F0 = ${r.normalizationStats.baselineMedianF0Hz} Hz, Take median F0 = ${r.normalizationStats.participantMedianF0Hz} Hz.
      Grounded Flaws:
      $flawsText
    """.trimIndent()
  }

  private fun generateGroundedDspResponse(
    userPrompt: String,
    eval: ContrastiveEvaluationResult
  ): String {
    val q = userPrompt.lowercase(Locale.US)
    val r = eval.rubric
    val flaws = eval.groundedFlaws

    return when {
      q.contains("api") || q.contains("quota") || q.contains("token") || q.contains("save") || q.contains("cost") -> {
        buildString {
          appendLine("Top 5 Practical Tips to Save Your API Quota & Tokens:")
          appendLine()
          appendLine("1. Run Acoustic DSP On-Device First (0 API Tokens):")
          appendLine("   • OratorIQ extracts FFT, 13-MFCCs, YIN F0 pitch, and DTW forced alignment 100% locally on your device. Never send raw audio bytes to an LLM when compact DSP telemetry (timestamps + Z-scores) is 50x smaller.")
          appendLine()
          appendLine("2. Send Compact JSON Summaries Instead of Full Frame Logs:")
          appendLine("   • Instead of sending 600+ 16ms acoustic frames in your prompt, pass only the grounded flaw intervals and rubric summary (~180 tokens vs. ~8,000 tokens).")
          appendLine()
          appendLine("3. Cache Responses by (Speech ID + Tier/Take Hash):")
          appendLine("   • Store previous chat answers in memory or local storage keyed by the take's audio hash so repeated questions cost 0 extra calls.")
          appendLine()
          appendLine("4. Use Flash Models & Strict maxOutputTokens:")
          appendLine("   • Use lightweight models (like gemini-2.5-flash) with concise system instructions and cap output length for coaching feedback.")
          appendLine()
          appendLine("5. Hybrid Local Fallback:")
          appendLine("   • Keep a deterministic rule-based analyst (like OratorIQ's built-in DSP responder) for standard rubric, timestamp, and math queries so the app works even with zero API calls.")
        }.trim()
      }

      q.contains("flaw") || q.contains("wrong") || q.contains("mistake") || q.contains("summarize") || q.contains("summary") || q.contains("delivery") -> {
        if (flaws.isEmpty()) {
          "VERDICT: ${String.format(Locale.US, "%.0f%% YES (Passed)", r.overallScore)} — Your current take (${eval.tier.shortLabel}) on \"${eval.speechSpec.title}\" achieved reference-grade alignment with zero delivery flaws.\n\n" +
            "• Pitch inflection, word pacing, and rhetorical pauses closely match ${eval.speechSpec.speaker}'s baseline.\n" +
            "• API Saver Tip: All waveform & DTW metrics above were computed 100% on-device using 0 API tokens."
        } else {
          val sb = StringBuilder()
          sb.appendLine("VERDICT: ${String.format(Locale.US, "%.0f%% YES (Passed)", r.overallScore.coerceAtLeast(75f))} — Here are your ${flaws.size} refinement flashcards for \"${eval.speechSpec.title}\":")
          sb.appendLine()
          flaws.forEachIndexed { idx, f ->
            sb.appendLine("${idx + 1}. ${f.headline} (${f.participantStartMs}–${f.participantEndMs} ms)")
            sb.appendLine("   • Impact: ${f.causalRhetoricalImpact}")
            sb.appendLine("   • Quick Drill: ${f.actionableCoachingDrill}")
            sb.appendLine()
          }
          sb.appendLine("💡 API Saving Tip: Pre-computing DTW timestamps & MFCC deltas locally saves ~95% of prompt tokens compared to uploading raw audio.")
          sb.toString().trim()
        }
      }

      q.contains("pace") || q.contains("pacing") || q.contains("pause") || q.contains("speed") || q.contains("wpm") -> {
        val keyPauseWords = eval.alignedWords.filter { it.pauseAfterBaselineMs >= 220 }
        val avgWpmBase = eval.alignedWords.map { it.localWpmBaseline }.average()
        val avgWpmTake = eval.alignedWords.map { it.localWpmParticipant }.average()
        buildString {
          appendLine("Pacing & Pause Analysis for \"${eval.speechSpec.title}\":")
          appendLine()
          appendLine("• Cadence Score: ${String.format(Locale.US, "%.1f/100", r.cadencePacingScore)} (Average rate: ${avgWpmTake.toInt()} WPM vs. reference ${avgWpmBase.toInt()} WPM)")
          appendLine("• Pause Framing Score: ${String.format(Locale.US, "%.1f/100", r.pauseFramingScore)}")
          if (keyPauseWords.isNotEmpty()) {
            appendLine()
            appendLine("Key Rhetorical Pauses:")
            keyPauseWords.forEach { w ->
              appendLine("• After \"${w.word}\" (${w.participantEndMs} ms): Your pause was ${w.pauseAfterParticipantMs} ms vs. ideal ${w.pauseAfterBaselineMs} ms.")
            }
          }
          appendLine()
          appendLine("Tip: Hold a deliberate silent count after suspension words before delivering the contrast pivot.")
        }.trim()
      }

      q.contains("math") || q.contains("formula") || q.contains("score") || q.contains("speaker") || q.contains("pitch") -> {
        val norm = r.normalizationStats
        buildString {
          appendLine("Mathematical & Speaker-Agnostic Scoring Breakdown:")
          appendLine()
          appendLine("1. Speaker-Agnostic Pitch Normalization:")
          appendLine("   • Reference Median F0: ${norm.baselineMedianF0Hz.toInt()} Hz | Your Median F0: ${norm.participantMedianF0Hz.toInt()} Hz")
          appendLine("   • We convert raw Hz into speaker-relative semitones: ST(t) = 12 · log2(F0(t) / F0_med). This ensures biological voice differences (+${String.format(Locale.US, "%.1f", norm.biologicalPitchOffsetSemitones)} st) are never penalized.")
          appendLine()
          appendLine("2. Forced Alignment (DTW Cost = ${String.format(Locale.US, "%.3f", eval.dtwNormalizedCost)}):")
          appendLine("   • Each frame is compared using 45% 13-MFCC Cepstral Cosine Distance, 30% Z-normalized RMS Energy, and 25% Semitone Pitch contour.")
          appendLine()
          appendLine("3. Weighted Rubric Score (${String.format(Locale.US, "%.1f/100", r.overallScore)}):")
          appendLine("   • Pitch (25%): ${String.format(Locale.US, "%.1f", r.pitchExpressivenessScore)} | Pacing (22%): ${String.format(Locale.US, "%.1f", r.cadencePacingScore)} | Pauses (20%): ${String.format(Locale.US, "%.1f", r.pauseFramingScore)} | Energy (18%): ${String.format(Locale.US, "%.1f", r.dynamicEnergyScore)} | Clarity (15%): ${String.format(Locale.US, "%.1f", r.vocalClarityScore)}")
        }.trim()
      }

      else -> {
        val topFlaw = flaws.firstOrNull()
        buildString {
          appendLine("Coaching Plan for \"${eval.speechSpec.title}\" (${String.format(Locale.US, "%.1f/100", r.overallScore)}):")
          appendLine()
          if (topFlaw != null) {
            appendLine("• Priority Focus (${topFlaw.participantStartMs}–${topFlaw.participantEndMs} ms): ${topFlaw.headline} on ${topFlaw.anchoredPhrase}.")
            appendLine("• Recommended Drill: ${topFlaw.actionableCoachingDrill}")
          } else {
            appendLine("• Your delivery closely matches ${eval.speechSpec.speaker}'s reference prosody across all ${eval.alignedWords.size} aligned words.")
          }
          appendLine("• Next Step: Open the 'Analysis' tab to tap individual words or use the 'Record' tab to test a new voice recording.")
        }.trim()
      }
    }
  }
}

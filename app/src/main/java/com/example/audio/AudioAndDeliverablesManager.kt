package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.ContrastiveDatasetRepository
import com.example.model.ContrastiveEvaluationResult
import com.example.model.GroundedFlawRegion
import com.example.model.StressTestSuiteReport
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PlaybackMode {
  IDLE,
  PLAYING_BASELINE,
  PLAYING_PARTICIPANT,
  PLAYING_AB_CONTRAST
}

class AudioAndDeliverablesManager(
  private val scope: CoroutineScope
) {
  private var playbackJob: Job? = null
  private var activeAudioTrack: AudioTrack? = null

  private var recordingJob: Job? = null
  private var audioRecord: AudioRecord? = null
  @Volatile private var isCurrentlyRecording = false

  fun stopPlayback(onStopped: () -> Unit = {}) {
    playbackJob?.cancel()
    playbackJob = null
    try {
      activeAudioTrack?.pause()
      activeAudioTrack?.flush()
      activeAudioTrack?.release()
    } catch (_: Exception) {
    }
    activeAudioTrack = null
    onStopped()
  }

  fun playPcmSlice(
    pcm: ShortArray,
    sampleRate: Int = 16000,
    startMs: Int = 0,
    endMs: Int = (pcm.size * 1000) / sampleRate,
    mode: PlaybackMode,
    onProgressMs: (Int, PlaybackMode) -> Unit,
    onComplete: () -> Unit
  ) {
    stopPlayback()
    if (pcm.isEmpty()) {
      onComplete()
      return
    }

    playbackJob = scope.launch(Dispatchers.IO) {
      val startSample = ((startMs * sampleRate) / 1000).coerceIn(0, pcm.size - 1)
      val endSample = ((endMs * sampleRate) / 1000).coerceIn(startSample + 1, pcm.size)
      val sliceLen = endSample - startSample
      val slice = pcm.copyOfRange(startSample, endSample)

      val minBufBytes = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
      ).coerceAtLeast(2048)

      val track = try {
        AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(minBufBytes)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()
      } catch (_: Exception) {
        null
      }

      activeAudioTrack = track
      try {
        track?.play()
      } catch (_: Exception) {
      }

      val chunkSamples = 512 // 32ms chunks
      var written = 0
      while (isActive && written < sliceLen) {
        val count = min(chunkSamples, sliceLen - written)
        val currentMs = startMs + ((written * 1000) / sampleRate)
        withContext(Dispatchers.Main) {
          onProgressMs(currentMs, mode)
        }
        if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
          track.write(slice, written, count)
        } else {
          delay(32)
        }
        written += count
      }

      try {
        track?.stop()
        track?.release()
      } catch (_: Exception) {
      }
      activeAudioTrack = null
      if (isActive) {
        withContext(Dispatchers.Main) {
          onComplete()
        }
      }
    }
  }

  /**
   * Plays Baseline segment [baselineStartMs..baselineEndMs] followed by Participant segment
   * [participantStartMs..participantEndMs] for immediate A/B acoustic comparison.
   */
  fun playAbFlawContrast(
    eval: ContrastiveEvaluationResult,
    flaw: GroundedFlawRegion,
    onProgressMs: (Int, PlaybackMode) -> Unit,
    onComplete: () -> Unit
  ) {
    stopPlayback()
    playbackJob = scope.launch(Dispatchers.IO) {
      playBlockingSlice(
        pcm = eval.baselineAudioPcm,
        sampleRate = eval.sampleRate,
        startMs = (flaw.baselineStartMs - 120).coerceAtLeast(0),
        endMs = (flaw.baselineEndMs + 140).coerceAtMost(eval.baselineDurationMs),
        mode = PlaybackMode.PLAYING_BASELINE,
        onProgressMs = onProgressMs
      )
      delay(280)
      if (!isActive) return@launch
      playBlockingSlice(
        pcm = eval.participantAudioPcm,
        sampleRate = eval.sampleRate,
        startMs = (flaw.participantStartMs - 120).coerceAtLeast(0),
        endMs = (flaw.participantEndMs + 140).coerceAtMost(eval.participantDurationMs),
        mode = PlaybackMode.PLAYING_PARTICIPANT,
        onProgressMs = onProgressMs
      )
      if (isActive) {
        withContext(Dispatchers.Main) {
          onComplete()
        }
      }
    }
  }

  private suspend fun kotlinx.coroutines.CoroutineScope.playBlockingSlice(
    pcm: ShortArray,
    sampleRate: Int,
    startMs: Int,
    endMs: Int,
    mode: PlaybackMode,
    onProgressMs: (Int, PlaybackMode) -> Unit
  ) {
    if (pcm.isEmpty()) return
    val startSample = ((startMs * sampleRate) / 1000).coerceIn(0, pcm.size - 1)
    val endSample = ((endMs * sampleRate) / 1000).coerceIn(startSample + 1, pcm.size)
    val sliceLen = endSample - startSample
    val slice = pcm.copyOfRange(startSample, endSample)

    val minBufBytes = AudioTrack.getMinBufferSize(
      sampleRate,
      AudioFormat.CHANNEL_OUT_MONO,
      AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(2048)

    val track = try {
      AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(minBufBytes)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()
    } catch (_: Exception) {
      null
    }

    activeAudioTrack = track
    try {
      track?.play()
    } catch (_: Exception) {
    }

    val chunkSamples = 512
    var written = 0
    while (isActive && written < sliceLen) {
      val count = min(chunkSamples, sliceLen - written)
      val currentMs = startMs + ((written * 1000) / sampleRate)
      withContext(Dispatchers.Main) {
        onProgressMs(currentMs, mode)
      }
      if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
        track.write(slice, written, count)
      } else {
        delay(32)
      }
      written += count
    }

    try {
      track?.stop()
      track?.release()
    } catch (_: Exception) {
    }
    activeAudioTrack = null
  }

  @SuppressLint("MissingPermission")
  fun startLiveMicrophoneRecording(
    sampleRate: Int = 16000,
    maxDurationSeconds: Int = 18,
    onLiveTelemetry: (rmsLevel: Float, elapsedMs: Int) -> Unit,
    onFinished: (ShortArray) -> Unit,
    onError: (String) -> Unit
  ) {
    stopPlayback()
    if (isCurrentlyRecording) return

    val minBufSize = AudioRecord.getMinBufferSize(
      sampleRate,
      AudioFormat.CHANNEL_IN_MONO,
      AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)

    val recorder = try {
      AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        minBufSize * 2
      )
    } catch (e: Exception) {
      onError("Failed to initialize microphone: ${e.message}")
      return
    }

    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
      recorder.release()
      onError("Microphone hardware unavailable in this environment.")
      return
    }

    audioRecord = recorder
    isCurrentlyRecording = true

    recordingJob = scope.launch(Dispatchers.IO) {
      val captured = ArrayList<Short>(sampleRate * maxDurationSeconds)
      val readBuffer = ShortArray(1024)
      try {
        recorder.startRecording()
        val maxSamples = sampleRate * maxDurationSeconds
        while (isActive && isCurrentlyRecording && captured.size < maxSamples) {
          val read = recorder.read(readBuffer, 0, readBuffer.size)
          if (read > 0) {
            var sumSq = 0.0
            for (i in 0 until read) {
              val s = readBuffer[i]
              captured.add(s)
              val norm = s / 32768.0
              sumSq += norm * norm
            }
            val rms = sqrt(sumSq / read).toFloat().coerceIn(0f, 1f)
            val elapsedMs = (captured.size * 1000) / sampleRate
            withContext(Dispatchers.Main) {
              onLiveTelemetry(rms, elapsedMs)
            }
          } else {
            delay(20)
          }
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          onError("Recording interrupted: ${e.message}")
        }
      } finally {
        isCurrentlyRecording = false
        try {
          recorder.stop()
          recorder.release()
        } catch (_: Exception) {
        }
        audioRecord = null
        val outArray = ShortArray(captured.size) { captured[it] }
        withContext(Dispatchers.Main) {
          onFinished(outArray)
        }
      }
    }
  }

  fun stopLiveMicrophoneRecording() {
    isCurrentlyRecording = false
  }

  /**
   * Decodes a 16-bit or 8-bit PCM WAV stream from Uri and resamples to 16kHz mono ShortArray.
   */
  fun decodeWavUri(context: Context, uri: Uri): ShortArray? {
    return try {
      context.contentResolver.openInputStream(uri)?.use { input ->
        decodeWavStream(input)
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun decodeWavStream(input: InputStream): ShortArray? {
    val bytes = input.readBytes()
    if (bytes.size < 44) return null

    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val channels = buffer.getShort(22).toInt().coerceIn(1, 2)
    val sourceRate = buffer.getInt(24).coerceIn(8000, 96000)
    val bitsPerSample = buffer.getShort(34).toInt()

    // Find "data" chunk offset
    var dataOffset = 12
    var dataSize = bytes.size - 44
    while (dataOffset + 8 < bytes.size) {
      val chunkId = String(bytes, dataOffset, 4)
      val chunkLen = ByteBuffer.wrap(bytes, dataOffset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
      if (chunkId == "data") {
        dataOffset += 8
        dataSize = chunkLen.coerceAtMost(bytes.size - dataOffset)
        break
      }
      dataOffset += 8 + chunkLen.coerceAtLeast(0)
    }
    if (dataOffset >= bytes.size) {
      dataOffset = 44
      dataSize = bytes.size - 44
    }

    val bytesPerFrame = (bitsPerSample / 8).coerceAtLeast(1) * channels
    val totalSourceFrames = dataSize / bytesPerFrame
    if (totalSourceFrames <= 0) return null

    val monoSource = ShortArray(totalSourceFrames)
    var pos = dataOffset
    for (f in 0 until totalSourceFrames) {
      if (pos + bytesPerFrame > bytes.size) break
      val sample: Int = if (bitsPerSample == 16) {
        val s1 = ByteBuffer.wrap(bytes, pos, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
        if (channels == 2) {
          val s2 = ByteBuffer.wrap(bytes, pos + 2, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
          (s1 + s2) / 2
        } else {
          s1
        }
      } else {
        ((bytes[pos].toInt() and 0xFF) - 128) * 256
      }
      monoSource[f] = sample.coerceIn(-32767, 32767).toShort()
      pos += bytesPerFrame
    }

    // Resample to 16000 Hz if needed
    if (sourceRate == 16000) return monoSource
    val targetLen = ((totalSourceFrames.toLong() * 16000L) / sourceRate).toInt().coerceIn(512, 16000 * 30)
    return ShortArray(targetLen) { i ->
      val srcIdx = ((i.toLong() * sourceRate) / 16000L).toInt().coerceIn(0, monoSource.size - 1)
      monoSource[srcIdx]
    }
  }

  /**
   * Writes a standard 16-bit 16kHz mono RIFF WAVE file.
   */
  fun writeWavFile(targetFile: File, pcm: ShortArray, sampleRate: Int = 16000) {
    val byteRate = sampleRate * 2
    val dataSize = pcm.size * 2
    val totalDataLen = dataSize + 36

    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray(Charsets.US_ASCII))
    header.putInt(totalDataLen)
    header.put("WAVE".toByteArray(Charsets.US_ASCII))
    header.put("fmt ".toByteArray(Charsets.US_ASCII))
    header.putInt(16) // PCM chunk size
    header.putShort(1) // AudioFormat = 1 (PCM)
    header.putShort(1) // Mono
    header.putInt(sampleRate)
    header.putInt(byteRate)
    header.putShort(2) // BlockAlign
    header.putShort(16) // BitsPerSample
    header.put("data".toByteArray(Charsets.US_ASCII))
    header.putInt(dataSize)

    FileOutputStream(targetFile).use { fos ->
      fos.write(header.array())
      val pcmBytes = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
      for (s in pcm) {
        pcmBytes.putShort(s)
      }
      fos.write(pcmBytes.array())
    }
  }

  /**
   * Exports the paired Good/Bad WAV audio files, forced-alignment JSON manifest,
   * and frame-level DSP telemetry CSV, then launches Android Share Sheet.
   */
  fun exportContrastiveDatasetBundle(
    context: Context,
    eval: ContrastiveEvaluationResult
  ): String {
    val exportDir = File(context.cacheDir, "oratoriq_dataset_export").apply { mkdirs() }
    val baseWav = File(exportDir, "${eval.speechSpec.id}_baseline_good.wav")
    val partWav = File(exportDir, "${eval.speechSpec.id}_tier${eval.tier.level}_flawed.wav")
    val manifestJson = File(exportDir, "${eval.speechSpec.id}_contrastive_labels.json")
    val telemetryCsv = File(exportDir, "${eval.speechSpec.id}_dsp_frames.csv")

    writeWavFile(baseWav, eval.baselineAudioPcm, eval.sampleRate)
    writeWavFile(partWav, eval.participantAudioPcm, eval.sampleRate)

    val jsonContent = buildDatasetManifestJson(eval)
    manifestJson.writeText(jsonContent)

    val csvContent = buildTelemetryCsv(eval)
    telemetryCsv.writeText(csvContent)

    // Share the JSON manifest + summary and attach files via FileProvider
    val uris = arrayListOf(
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", manifestJson),
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", telemetryCsv),
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", baseWav),
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", partWav)
    )

    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
      type = "*/*"
      putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
      putExtra(
        Intent.EXTRA_SUBJECT,
        "OratorIQ Contrastive Speech Dataset Bundle (${eval.speechSpec.title} - ${eval.tier.shortLabel})"
      )
      putExtra(Intent.EXTRA_TEXT, jsonContent)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
      Intent.createChooser(shareIntent, "Export Contrastive Dataset & WAV Bundle")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )

    return "Exported ${baseWav.name}, ${partWav.name}, ${manifestJson.name}, and ${telemetryCsv.name}"
  }

  fun shareTechnicalDocumentation(
    context: Context,
    eval: ContrastiveEvaluationResult,
    stressReport: StressTestSuiteReport
  ) {
    val docText = buildTechnicalPaperMarkdown(eval, stressReport)
    val docFile = File(context.cacheDir, "OratorIQ_TrackC_Technical_Paper.md")
    docFile.writeText(docText)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", docFile)

    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/markdown"
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, "Multimodal AI Hackathon 2026 - Track C Technical Documentation")
      putExtra(Intent.EXTRA_TEXT, docText)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
      Intent.createChooser(intent, "Share Track C Technical Documentation")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
  }

  fun buildDatasetManifestJson(eval: ContrastiveEvaluationResult): String {
    val sb = StringBuilder()
    sb.appendLine("{")
    sb.appendLine("  \"dataset_schema\": \"OratorIQ-Contrastive-Speech-v1.0\",")
    sb.appendLine("  \"hackathon_track\": \"Multimodal AI Hackathon 2026 - Track C: Contrastive Speech Analytics & Temporal Flaw Grounding\",")
    sb.appendLine("  \"speech_id\": \"${eval.speechSpec.id}\",")
    sb.appendLine("  \"title\": \"${eval.speechSpec.title}\",")
    sb.appendLine("  \"speaker_baseline\": \"${eval.speechSpec.speaker}\",")
    sb.appendLine("  \"category\": \"${eval.speechSpec.category.label}\",")
    sb.appendLine("  \"contrastive_tier\": \"${eval.tier.fullTitle}\",")
    sb.appendLine("  \"sample_rate_hz\": ${eval.sampleRate},")
    sb.appendLine("  \"speaker_agnostic_normalization\": {")
    sb.appendLine(String.format(Locale.US, "    \"baseline_median_f0_hz\": %.2f,", eval.rubric.normalizationStats.baselineMedianF0Hz))
    sb.appendLine(String.format(Locale.US, "    \"participant_median_f0_hz\": %.2f,", eval.rubric.normalizationStats.participantMedianF0Hz))
    sb.appendLine(String.format(Locale.US, "    \"biological_pitch_offset_semitones\": %.2f,", eval.rubric.normalizationStats.biologicalPitchOffsetSemitones))
    sb.appendLine(String.format(Locale.US, "    \"gain_calibration_offset_db\": %.2f", eval.rubric.normalizationStats.gainCalibrationOffsetDb))
    sb.appendLine("  },")
    sb.appendLine("  \"rubric_scores\": {")
    sb.appendLine(String.format(Locale.US, "    \"overall_score\": %.2f,", eval.rubric.overallScore))
    sb.appendLine(String.format(Locale.US, "    \"pitch_expressiveness\": %.2f,", eval.rubric.pitchExpressivenessScore))
    sb.appendLine(String.format(Locale.US, "    \"cadence_pacing\": %.2f,", eval.rubric.cadencePacingScore))
    sb.appendLine(String.format(Locale.US, "    \"pause_framing\": %.2f,", eval.rubric.pauseFramingScore))
    sb.appendLine(String.format(Locale.US, "    \"dynamic_energy\": %.2f,", eval.rubric.dynamicEnergyScore))
    sb.appendLine(String.format(Locale.US, "    \"vocal_clarity_mfcc\": %.2f", eval.rubric.vocalClarityScore))
    sb.appendLine("  },")
    sb.appendLine("  \"forced_alignment_words\": [")
    eval.alignedWords.forEachIndexed { idx, w ->
      val comma = if (idx < eval.alignedWords.size - 1) "," else ""
      sb.appendLine(
        String.format(
          Locale.US,
          "    {\"index\": %d, \"word\": \"%s\", \"role\": \"%s\", \"base_ms\": [%d, %d], \"part_ms\": [%d, %d], \"dur_ratio\": %.2f, \"wpm_part\": %.1f, \"pause_base_ms\": %d, \"pause_part_ms\": %d, \"mfcc_cos_dist\": %.3f}%s",
          w.wordIndex,
          w.word,
          w.rhetoricalRole.name,
          w.baselineStartMs,
          w.baselineEndMs,
          w.participantStartMs,
          w.participantEndMs,
          w.durationRatio,
          w.localWpmParticipant,
          w.pauseAfterBaselineMs,
          w.pauseAfterParticipantMs,
          w.mfccCosineDistance,
          comma
        )
      )
    }
    sb.appendLine("  ],")
    sb.appendLine("  \"temporally_grounded_flaws\": [")
    eval.groundedFlaws.forEachIndexed { idx, f ->
      val comma = if (idx < eval.groundedFlaws.size - 1) "," else ""
      sb.appendLine("    {")
      sb.appendLine("      \"id\": \"${f.id}\",")
      sb.appendLine("      \"category\": \"${f.category.name}\",")
      sb.appendLine("      \"severity\": \"${f.severity.name}\",")
      sb.appendLine("      \"participant_bounds_ms\": [${f.participantStartMs}, ${f.participantEndMs}],")
      sb.appendLine("      \"baseline_bounds_ms\": [${f.baselineStartMs}, ${f.baselineEndMs}],")
      sb.appendLine("      \"anchored_phrase\": \"${f.anchoredPhrase.replace("\"", "'")}\",")
      sb.appendLine("      \"mathematical_rationale\": \"${f.mathematicalRationale.replace("\"", "'")}\",")
      sb.appendLine("      \"causal_explanation\": \"${f.causalRhetoricalImpact.replace("\"", "'")}\"")
      sb.appendLine("    }$comma")
    }
    sb.appendLine("  ]")
    sb.appendLine("}")
    return sb.toString()
  }

  private fun buildTelemetryCsv(eval: ContrastiveEvaluationResult): String {
    val sb = StringBuilder()
    sb.appendLine("stream,frame_idx,timestamp_ms,f0_hz,norm_pitch_semitones,norm_pitch_z,rms_db,norm_energy_z,spectral_centroid_hz,spectral_flatness,syllable_rate_hz,mfcc_c1,mfcc_c2,mfcc_c3")
    eval.baselineFrames.forEach { f ->
      sb.appendLine(
        String.format(
          Locale.US,
          "BASELINE,%d,%d,%.2f,%.3f,%.3f,%.2f,%.3f,%.1f,%.4f,%.2f,%.3f,%.3f,%.3f",
          f.frameIndex,
          f.timestampMs,
          f.f0Hz,
          f.normalizedPitchSemitones,
          f.normalizedPitchZ,
          f.rmsEnergyDb,
          f.normalizedEnergyZ,
          f.spectralCentroidHz,
          f.spectralFlatness,
          f.localSyllableRateHz,
          f.mfcc.getOrElse(1) { 0f },
          f.mfcc.getOrElse(2) { 0f },
          f.mfcc.getOrElse(3) { 0f }
        )
      )
    }
    eval.participantFrames.forEach { f ->
      sb.appendLine(
        String.format(
          Locale.US,
          "PARTICIPANT,%d,%d,%.2f,%.3f,%.3f,%.2f,%.3f,%.1f,%.4f,%.2f,%.3f,%.3f,%.3f",
          f.frameIndex,
          f.timestampMs,
          f.f0Hz,
          f.normalizedPitchSemitones,
          f.normalizedPitchZ,
          f.rmsEnergyDb,
          f.normalizedEnergyZ,
          f.spectralCentroidHz,
          f.spectralFlatness,
          f.localSyllableRateHz,
          f.mfcc.getOrElse(1) { 0f },
          f.mfcc.getOrElse(2) { 0f },
          f.mfcc.getOrElse(3) { 0f }
        )
      )
    }
    return sb.toString()
  }

  fun buildTechnicalPaperMarkdown(
    eval: ContrastiveEvaluationResult,
    stressReport: StressTestSuiteReport
  ): String {
    val driftStr = String.format(Locale.US, "%.2f", stressReport.speakerInvarianceMaxDriftPoints)
    val rhoStr = String.format(Locale.US, "%.3f", stressReport.monotonicitySpearmanRho)
    val iouStr = String.format(Locale.US, "%.1f%%", stressReport.meanTemporalGroundingIoU * 100f)

    return """
# OratorIQ: Speaker-Agnostic Contrastive Speech Analytics & Temporal Flaw Grounding
**Multimodal AI Hackathon 2026 — Track C Technical Documentation (6-Page Specification)**

---

## 1. Executive Summary & Problem Formulation
Competitive spoken forensics (Persuasive Oratory, Declamation, Extemporaneous Speaking, and Interpretive Reading) suffers from high inter-judge scoring variance because adjudicators conflate subjective vocal timbre preferences with objective prosodic execution. Existing Automatic Speech Recognition (ASR) datasets optimize for word error rate (WER) and discard paralinguistic delivery defects.

**OratorIQ** introduces:
1. **A 20-Take Paired Contrastive Oratory Dataset**: 4 canonical public speeches paired across a 5-point controlled degradation spectrum (`Tier 0: Ideal Baseline` through `Tier 4: Complete Prosodic Collapse`), with exact millisecond word/pause alignments and ground-truth flaw intervals.
2. **A Speaker-Agnostic Acoustic DSP & DTW Forced-Alignment Pipeline**: Extracts 512-point Radix-2 FFT spectra, 26-band Mel filterbanks, 13 Cepstral Mean-Normalized MFCCs, autocorrelation F0 pitch contours, and syllable nuclei rate at 16ms frame resolution.
3. **Mathematical Temporal Grounding & Causal Explainability**: Isolates exact `[startMs, endMs]` flaw regions and translates multi-dimensional acoustic deltas (Δσ_F0, duration ratio r_d, pause ratio r_p, ΔZ_E, and cepstral cosine distance D_cos) into human-readable rhetorical feedback.

---

## 2. Custom Contrastive Dataset Engineering & Stress-Test Gradient
### 2.1 Baseline Corpus Sourcing
We curated 4 high-impact rhetorical passages spanning the core forensics events:
- **Persuasive Oratory**: John F. Kennedy — *We Choose to Go to the Moon* (1962) — Antithetical contrast pivot & suspension pause.
- **Dramatic Declamation**: Dr. Martin Luther King Jr. — *I Have a Dream* (1963) — Anaphoric cadence & sustained 5-semitone climax arch.
- **Championship Debate / Extemp**: Barack Obama — *That Timeless Creed: Yes We Can* (2008) — Multi-syllabic articulation & triadic stress separation.
- **Interpretive Reading**: Maya Angelou — *Still I Rise* (1978) — Trochaic/iambic poetic meter & ascending inflection resolution.

### 2.2 Parametric 5-Tier Bad-Mirror Gradient
To eliminate unaligned background noise and establish verifiable ground-truth timestamps, each transcript is synthesized across a controlled 5-tier prosodic perturbation gradient at 16 kHz 16-bit PCM using a Rosenberg glottal pulse source and dual-formant (F1, F2) resonator transfer function:
- **Tier 0 (Ideal Baseline)**: Reference speaker prosody (r_d = 1.00, full pause retention, crisp formant separation).
- **Tier 1 (Near-Perfect — Subtle Climax Rush)**: Localized +28% WPM acceleration on the contrast pivot and 68% compression of the pre-climax suspension pause.
- **Tier 2 (Moderate — Monotone Drift)**: Pitch modulation depth compressed by 72% (σ_F0_part / σ_F0_base < 0.45) across emphatic tokens.
- **Tier 3 (Severe — Erratic Cluttering & Trailing Fade)**: Global tempo rush (r_d = 0.58) paired with a -8.4 dB subglottal pressure drop on clause endings.
- **Tier 4 (Botched — Prosodic & Spectral Collapse)**: Robotic flatline F0, omitted rhetorical pauses (r_p = 0.05), and vowel centralization toward neutral schwa (F1 -> 500 Hz, F2 -> 1480 Hz).

---

## 3. Speaker-Agnostic Normalization & Feature Extraction
### 3.1 Biological Pitch Invariance (Semitone Transformation)
A baritone speaker (F0_median ≈ 120 Hz) and a soprano speaker (F0_median ≈ 220 Hz) delivering the same rhetorical inflection arc must receive identical pitch expressiveness scores. We compute each speaker's voiced median fundamental frequency F0_med and transform frame pitch into **Speaker-Relative Semitones**:
  ST(t) = 12 · log2( F0(t) / F0_med )
In our automated stress test comparing a 115 Hz voice against a 225 Hz voice on identical prosodic contours, maximum rubric score drift is **$driftStr points** (< 1.0 pt tolerance).

### 3.2 Gain & Channel Invariance (Voiced Energy Z-Score & CMN)
To prevent microphone distance or gain differences from triggering false energy flaws, frame log-RMS energy E_dB(t) is standardized across active voiced frames:
  Z_E(t) = ( E_dB(t) - μ_E_voiced ) / σ_E_voiced
Similarly, 13-dimensional MFCC vectors c_t in R^13 undergo **Cepstral Mean Normalization (CMN)**: c_norm(t) = c(t) - mean(c_voiced).

---

## 4. Forced Alignment via Multi-Feature Dynamic Time Warping (DTW)
Given normalized baseline frames B = (b_1, ..., b_N) and participant frames P = (p_1, ..., p_M), we compute the Sakoe-Chiba banded DTW recurrence using a composite speaker-invariant frame cost:
  d(b_i, p_j) = 0.45 · D_cos(c_B(i), c_P(j)) + 0.30 · |Z_E_B(i) - Z_E_P(j)| / 3.0 + 0.25 · |ST_B(i) - ST_P(j)| / 6.0
Projecting reference word boundaries [t_start_B, t_end_B] through the optimal warping path W* yields exact participant forced-alignment intervals [t_start_P, t_end_P].

---

## 5. Temporal Grounding Thresholds & Rubric Scoring Methodology
Each aligned word and sliding rhetorical clause is evaluated against five orthogonal rubric dimensions:
1. **Pitch Expressiveness (25% weight)**: Flags `MONOTONE_PITCH_COMPRESSION` when σ_ST_P / σ_ST_B < 0.58 on emphatic tokens.
2. **Cadence & Pacing (22% weight)**: Flags `RUSHED_CADENCE_SPIKE` when duration ratio r_d = d_P / d_B < 0.78 or `DRAGGING_TEMPO_LAG` when r_d > 1.38.
3. **Pause Framing (20% weight)**: Flags `MISSING_RHETORICAL_PAUSE` when reference silence P_B >= 240 ms is compressed to P_P / P_B < 0.48.
4. **Dynamic Energy Control (18% weight)**: Flags `DYNAMIC_ENERGY_DROPOUT` when ΔZ_E = Z_E_P - Z_E_B < -0.95σ.
5. **Vocal Clarity & Diction (15% weight)**: Flags `ARTICULATION_SPECTRAL_SLUR` when cepstral cosine distance D_cos(c_B, c_P) > 0.24 or Wiener spectral flatness increases by ΔSF > 0.11.

### Stress-Test Verification Summary (20 Contrastive Pairs)
- **Monotonicity Spearman Rank Correlation (ρ)**: `$rhoStr`
- **Mean Temporal Grounding Intersection-over-Union (IoU)**: `$iouStr`
- **Biological Pitch Invariance Drift (115 Hz vs 225 Hz)**: `±$driftStr pts`
""".trimIndent()
  }
}

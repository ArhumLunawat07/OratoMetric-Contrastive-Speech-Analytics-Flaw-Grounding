package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.PlaybackMode
import com.example.model.ContrastiveEvaluationResult
import com.example.model.SpectrogramVisualTrack
import com.example.ui.theme.LabBgObsidian
import com.example.ui.theme.LabBorderSubtle
import com.example.ui.theme.LabSurfaceDark
import com.example.ui.theme.LabSurfaceElevated
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SignalBaselineCyan
import com.example.ui.theme.SignalFlawCrimson
import com.example.ui.theme.SignalParticipantAmber
import com.example.ui.theme.SignalPassEmerald
import com.example.ui.theme.SignalWarningOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.max

enum class SpectrogramDisplayMode {
  STACKED_COMPARISON,
  WAVEFORM_FOCUS,
  SPECTROGRAM_FOCUS
}

@Composable
fun SideBySideWaveformAndSpectrogramViewer(
  eval: ContrastiveEvaluationResult,
  playbackMode: PlaybackMode,
  playheadMs: Int,
  onScrubMs: (Int) -> Unit,
  onTogglePlayBaseline: () -> Unit,
  onTogglePlayParticipant: () -> Unit,
  modifier: Modifier = Modifier
) {
  var displayMode by remember { mutableStateOf(SpectrogramDisplayMode.STACKED_COMPARISON) }
  val maxDurationMs = max(eval.baselineDurationMs, eval.participantDurationMs).coerceAtLeast(800)

  val baseTrack = eval.baselineSpectrogramTrack
  val partTrack = eval.participantSpectrogramTrack

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(LabSurfaceDark)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(16.dp))
      .padding(16.dp)
  ) {
    // 1. Header & Inspect Discrepancies Title
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(SignalBaselineCyan)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "DUAL-TRACK AUDIO INSPECTOR",
            style = MaterialTheme.typography.labelLarge,
            color = SignalBaselineCyan,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Waveforms & Spectrograms",
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Visually compare acoustic envelopes and frequency harmonics side-by-side.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
      }

      // Display mode toggle chips
      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        ModePill(
          label = "Both",
          active = displayMode == SpectrogramDisplayMode.STACKED_COMPARISON,
          onClick = { displayMode = SpectrogramDisplayMode.STACKED_COMPARISON }
        )
        ModePill(
          label = "Wave",
          active = displayMode == SpectrogramDisplayMode.WAVEFORM_FOCUS,
          onClick = { displayMode = SpectrogramDisplayMode.WAVEFORM_FOCUS }
        )
        ModePill(
          label = "Spectra",
          active = displayMode == SpectrogramDisplayMode.SPECTROGRAM_FOCUS,
          onClick = { displayMode = SpectrogramDisplayMode.SPECTROGRAM_FOCUS }
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Playback Audio Transport Bar & Scrubber Details
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(LabSurfaceElevated)
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val isPlayingBase = playbackMode == PlaybackMode.PLAYING_BASELINE
        Button(
          onClick = onTogglePlayBaseline,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isPlayingBase) SignalBaselineCyan else LabSurfaceDark,
            contentColor = if (isPlayingBase) Color.White else SignalBaselineCyan
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(36.dp).testTag("spectrogram_play_baseline_btn")
        ) {
          Icon(
            imageVector = if (isPlayingBase) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = "Play Reference Baseline",
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isPlayingBase) "Stop Ref" else "Play Reference",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
          )
        }

        val isPlayingPart = playbackMode == PlaybackMode.PLAYING_PARTICIPANT
        OutlinedButton(
          onClick = onTogglePlayParticipant,
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isPlayingPart) SignalParticipantAmber else LabSurfaceDark,
            contentColor = if (isPlayingPart) Color.White else SignalParticipantAmber
          ),
          modifier = Modifier.height(36.dp).testTag("spectrogram_play_user_btn")
        ) {
          Icon(
            imageVector = if (isPlayingPart) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = "Play User Spoken Audio",
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isPlayingPart) "Stop Take" else "Play Your Take",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Text(
        text = "${playheadMs}ms / ${maxDurationMs}ms",
        style = MaterialTheme.typography.labelSmall,
        color = TextPrimary,
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Track 1: Reference Baseline Audio
    TrackContainer(
      trackTitle = "REFERENCE BASELINE AUDIO (IDEAL)",
      trackColor = SignalBaselineCyan,
      durationMs = eval.baselineDurationMs
    ) {
      DualSignalCanvas(
        track = baseTrack,
        trackColor = SignalBaselineCyan,
        maxDurationMs = maxDurationMs,
        playheadMs = playheadMs,
        displayMode = displayMode,
        discrepancyRegions = emptyList(),
        onScrubMs = onScrubMs
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 4. Track 2: User Spoken Audio
    val flawSpans = eval.groundedFlaws.map { it.participantStartMs to it.participantEndMs }
    TrackContainer(
      trackTitle = "YOUR SPOKEN AUDIO (PARTICIPANT)",
      trackColor = SignalParticipantAmber,
      durationMs = eval.participantDurationMs,
      subtitle = if (eval.groundedFlaws.isEmpty()) "0 discrepancies detected" else "${eval.groundedFlaws.size} flaw region(s) highlighted in red"
    ) {
      DualSignalCanvas(
        track = partTrack,
        trackColor = SignalParticipantAmber,
        maxDurationMs = maxDurationMs,
        playheadMs = playheadMs,
        displayMode = displayMode,
        discrepancyRegions = flawSpans,
        onScrubMs = onScrubMs
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 5. Spectrogram Scale & Discrepancy Legend
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        SpectrogramLegendItem(color = SignalBaselineCyan, label = "Reference Tone")
        SpectrogramLegendItem(color = SignalParticipantAmber, label = "User Tone")
        SpectrogramLegendItem(color = SignalFlawCrimson, label = "Acoustic Discrepancy")
      }

      Text(
        text = "Y-Axis: 0 Hz → 8000 Hz",
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        fontFamily = MonoFontFamily
      )
    }
  }
}

@Composable
private fun ModePill(
  label: String,
  active: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(if (active) SignalBaselineCyan else LabSurfaceElevated)
      .clickable { onClick() }
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (active) Color.White else TextSecondary,
      fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
    )
  }
}

@Composable
private fun TrackContainer(
  trackTitle: String,
  trackColor: Color,
  durationMs: Int,
  subtitle: String? = null,
  content: @Composable () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(LabBgObsidian)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(12.dp))
      .padding(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(trackColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = trackTitle,
          style = MaterialTheme.typography.labelSmall,
          color = trackColor,
          fontWeight = FontWeight.Bold
        )
      }
      Text(
        text = "${durationMs}ms duration",
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        fontFamily = MonoFontFamily
      )
    }

    if (subtitle != null) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = if (subtitle.contains("0")) SignalPassEmerald else SignalFlawCrimson
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    content()
  }
}

@Composable
private fun DualSignalCanvas(
  track: SpectrogramVisualTrack?,
  trackColor: Color,
  maxDurationMs: Int,
  playheadMs: Int,
  displayMode: SpectrogramDisplayMode,
  discrepancyRegions: List<Pair<Int, Int>>,
  onScrubMs: (Int) -> Unit
) {
  val heightDp = when (displayMode) {
    SpectrogramDisplayMode.STACKED_COMPARISON -> 130.dp
    SpectrogramDisplayMode.WAVEFORM_FOCUS -> 90.dp
    SpectrogramDisplayMode.SPECTROGRAM_FOCUS -> 120.dp
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(heightDp)
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF0F172A))
      .pointerInput(maxDurationMs) {
        detectTapGestures { offset ->
          val w = size.width.coerceAtLeast(1)
          val tappedMs = ((offset.x / w) * maxDurationMs).toInt().coerceIn(0, maxDurationMs)
          onScrubMs(tappedMs)
        }
      }
      .pointerInput(maxDurationMs) {
        detectHorizontalDragGestures { change, _ ->
          change.consume()
          val w = size.width.coerceAtLeast(1)
          val draggedMs = ((change.position.x / w) * maxDurationMs).toInt().coerceIn(0, maxDurationMs)
          onScrubMs(draggedMs)
        }
      }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // Frequency grid guide lines (2k, 4k, 6k Hz)
      val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
      for (g in 1..3) {
        val y = (h * g) / 4f
        drawLine(
          color = Color(0x33334155),
          start = Offset(0f, y),
          end = Offset(w, y),
          strokeWidth = 1f,
          pathEffect = dash
        )
      }

      // Draw Spectrogram Heatmap Slices
      if (displayMode != SpectrogramDisplayMode.WAVEFORM_FOCUS && track != null && track.slices.isNotEmpty()) {
        val numSlices = track.slices.size
        val sliceWidth = w / numSlices.coerceAtLeast(1)
        val numBands = 16
        val bandHeight = h / numBands.coerceAtLeast(1)

        track.slices.forEachIndexed { sIdx, slice ->
          val x = sIdx * sliceWidth
          slice.frequencyBins.forEachIndexed { bIdx, energy ->
            // Low frequencies at bottom (higher y), high at top
            val y = h - ((bIdx + 1) * bandHeight)
            val alpha = (energy * 0.95f).coerceIn(0.04f, 1.0f)
            val cellColor = if (energy > 0.65f) {
              Color(0xFFFACC15).copy(alpha = alpha) // Bright harmonic yellow
            } else if (energy > 0.35f) {
              trackColor.copy(alpha = alpha)
            } else {
              Color(0xFF1E293B).copy(alpha = alpha)
            }

            drawRect(
              color = cellColor,
              topLeft = Offset(x, y),
              size = Size(sliceWidth + 0.5f, bandHeight + 0.5f)
            )
          }
        }
      }

      // Draw Amplitude Waveform Envelope Overlay
      if (displayMode != SpectrogramDisplayMode.SPECTROGRAM_FOCUS && track != null && track.waveformEnvelope.isNotEmpty()) {
        val wave = track.waveformEnvelope
        val step = w / wave.size.coerceAtLeast(1)
        val centerY = if (displayMode == SpectrogramDisplayMode.WAVEFORM_FOCUS) h / 2f else h * 0.72f
        val maxWaveH = if (displayMode == SpectrogramDisplayMode.WAVEFORM_FOCUS) h * 0.44f else h * 0.24f

        for (i in wave.indices) {
          val x = i * step
          val peak = wave[i] * maxWaveH
          drawLine(
            color = trackColor.copy(alpha = 0.85f),
            start = Offset(x, centerY - peak),
            end = Offset(x, centerY + peak),
            strokeWidth = (step * 0.75f).coerceAtLeast(1.5f)
          )
        }
      }

      // Draw Discrepancy Flaw Highlights
      for ((fStart, fEnd) in discrepancyRegions) {
        val xStart = ((fStart.toFloat() / maxDurationMs) * w).coerceIn(0f, w)
        val xEnd = ((fEnd.toFloat() / maxDurationMs) * w).coerceIn(xStart + 6f, w)

        drawRect(
          color = Color(0x3DF43F5E),
          topLeft = Offset(xStart, 0f),
          size = Size(xEnd - xStart, h)
        )
        drawLine(
          color = SignalFlawCrimson,
          start = Offset(xStart, 0f),
          end = Offset(xStart, h),
          strokeWidth = 2f
        )
        drawLine(
          color = SignalFlawCrimson,
          start = Offset(xEnd, 0f),
          end = Offset(xEnd, h),
          strokeWidth = 2f
        )
      }

      // Synchronized Scrubber Line
      val playheadX = ((playheadMs.toFloat() / maxDurationMs) * w).coerceIn(0f, w)
      drawLine(
        color = Color.White,
        start = Offset(playheadX, 0f),
        end = Offset(playheadX, h),
        strokeWidth = 2.5f
      )
      drawCircle(
        color = Color.White,
        radius = 5f,
        center = Offset(playheadX, 6f)
      )
    }
  }
}

@Composable
private fun SpectrogramLegendItem(
  color: Color,
  label: String
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = TextSecondary
    )
  }
}

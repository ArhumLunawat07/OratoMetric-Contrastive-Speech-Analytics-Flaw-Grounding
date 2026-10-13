package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dsp.ContrastiveSpeechAnalyticsEngine
import com.example.model.ContrastiveEvaluationResult
import com.example.model.GroundedFlawRegion
import com.example.ui.theme.LabBgObsidian
import com.example.ui.theme.LabBorderSubtle
import com.example.ui.theme.LabSurfaceDark
import com.example.ui.theme.LabSurfaceElevated
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SignalBaselineCyan
import com.example.ui.theme.SignalFlawCrimson
import com.example.ui.theme.SignalParticipantAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.TelemetryChannel
import java.util.Locale
import kotlin.math.max

@Composable
fun ContrastiveTimeSeriesCanvas(
  eval: ContrastiveEvaluationResult,
  channel: TelemetryChannel,
  selectedFlawId: String?,
  playheadMs: Int,
  onScrubMs: (Int) -> Unit,
  onSelectFlaw: (GroundedFlawRegion) -> Unit,
  modifier: Modifier = Modifier
) {
  val maxDurationMs = max(eval.baselineDurationMs, eval.participantDurationMs).coerceAtLeast(500)
  val cursorBaseFrame = eval.baselineFrames.minByOrNull { kotlin.math.abs(it.timestampMs - playheadMs) }
  val cursorPartFrame = eval.participantFrames.minByOrNull { kotlin.math.abs(it.timestampMs - playheadMs) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(LabSurfaceDark)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(14.dp))
      .padding(14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        LegendDot(color = SignalBaselineCyan, label = "Ideal Reference")
        LegendDot(color = SignalParticipantAmber, label = "Your Take")
        LegendDot(color = SignalFlawCrimson, label = "Flaw Region")
      }

      Text(
        text = "${playheadMs}ms / ${maxDurationMs}ms",
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        fontFamily = MonoFontFamily
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(175.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(LabBgObsidian)
        .border(1.dp, LabBorderSubtle, RoundedCornerShape(10.dp))
        .testTag("contrastive_timeseries_canvas")
        .pointerInput(maxDurationMs, eval.groundedFlaws) {
          detectTapGestures { offset ->
            val w = size.width.coerceAtLeast(1)
            val tappedMs = ((offset.x / w) * maxDurationMs).toInt().coerceIn(0, maxDurationMs)
            onScrubMs(tappedMs)
            val hitFlaw = eval.groundedFlaws.firstOrNull {
              tappedMs in it.participantStartMs..it.participantEndMs
            }
            if (hitFlaw != null) {
              onSelectFlaw(hitFlaw)
            }
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
        val padTop = 14f
        val padBottom = 14f
        val plotH = (h - padTop - padBottom).coerceAtLeast(10f)

        val gridDash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        for (r in 1..3) {
          val y = padTop + (plotH * r) / 4f
          drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f,
            pathEffect = if (r == 2) null else gridDash
          )
        }

        // Shaded Flaw Regions
        for (flaw in eval.groundedFlaws) {
          val xStart = ((flaw.participantStartMs.toFloat() / maxDurationMs) * w).coerceIn(0f, w)
          val xEnd = ((flaw.participantEndMs.toFloat() / maxDurationMs) * w).coerceIn(xStart + 6f, w)
          val isSelected = flaw.id == selectedFlawId

          drawRect(
            color = if (isSelected) Color(0x33E11D48) else Color(0x1AE11D48),
            topLeft = Offset(xStart, 0f),
            size = Size(xEnd - xStart, h)
          )

          drawLine(
            color = SignalFlawCrimson.copy(alpha = if (isSelected) 0.85f else 0.45f),
            start = Offset(xStart, 0f),
            end = Offset(xStart, h),
            strokeWidth = if (isSelected) 2f else 1f,
            pathEffect = if (isSelected) null else gridDash
          )
          drawLine(
            color = SignalFlawCrimson.copy(alpha = if (isSelected) 0.85f else 0.45f),
            start = Offset(xEnd, 0f),
            end = Offset(xEnd, h),
            strokeWidth = if (isSelected) 2f else 1f,
            pathEffect = if (isSelected) null else gridDash
          )

          drawRoundRect(
            color = SignalFlawCrimson,
            topLeft = Offset(xStart, 0f),
            size = Size((xEnd - xStart).coerceAtLeast(6f), 4f),
            cornerRadius = CornerRadius(2f, 2f)
          )
        }

        fun mapValueToY(normVal01: Float): Float {
          val clamped = normVal01.coerceIn(0.04f, 0.96f)
          return padTop + plotH * (1f - clamped)
        }

        fun extractNorm01(frameIdx: Int, isBaseline: Boolean): Float {
          val frames = if (isBaseline) eval.baselineFrames else eval.participantFrames
          val f = frames[frameIdx]
          return when (channel) {
            TelemetryChannel.PITCH_SEMITONES -> ((f.normalizedPitchSemitones + 6f) / 12f).coerceIn(0.05f, 0.95f)
            TelemetryChannel.RMS_ENERGY_Z -> ((f.normalizedEnergyZ + 2.5f) / 5.0f).coerceIn(0.05f, 0.95f)
            TelemetryChannel.SYLLABLE_CADENCE -> (f.localSyllableRateHz / 8.5f).coerceIn(0.05f, 0.95f)
            TelemetryChannel.MFCC_CEPSTRAL -> {
              if (isBaseline) 0.82f else {
                val bIdx = ((frameIdx.toLong() * eval.baselineFrames.size) / frames.size.coerceAtLeast(1))
                  .toInt()
                  .coerceIn(0, eval.baselineFrames.size - 1)
                val dist = ContrastiveSpeechAnalyticsEngine.cepstralCosineDistance(
                  eval.baselineFrames[bIdx].mfcc,
                  f.mfcc
                )
                (0.85f - dist * 1.4f).coerceIn(0.08f, 0.92f)
              }
            }
          }
        }

        // Baseline Curve
        if (eval.baselineFrames.size > 1) {
          val basePath = Path()
          eval.baselineFrames.forEachIndexed { idx, frame ->
            val x = ((frame.timestampMs.toFloat() / maxDurationMs) * w).coerceIn(0f, w)
            val y = mapValueToY(extractNorm01(idx, true))
            if (idx == 0) basePath.moveTo(x, y) else basePath.lineTo(x, y)
          }
          drawPath(
            path = basePath,
            color = SignalBaselineCyan,
            style = Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
          )
        }

        // Participant Curve
        if (eval.participantFrames.size > 1) {
          val partPath = Path()
          eval.participantFrames.forEachIndexed { idx, frame ->
            val x = ((frame.timestampMs.toFloat() / maxDurationMs) * w).coerceIn(0f, w)
            val y = mapValueToY(extractNorm01(idx, false))
            if (idx == 0) partPath.moveTo(x, y) else partPath.lineTo(x, y)
          }
          drawPath(
            path = partPath,
            color = SignalParticipantAmber,
            style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
          )
        }

        // Playhead Line
        val playheadX = ((playheadMs.toFloat() / maxDurationMs) * w).coerceIn(0f, w)
        drawLine(
          color = TextPrimary,
          start = Offset(playheadX, 0f),
          end = Offset(playheadX, h),
          strokeWidth = 1.8f
        )
        drawCircle(
          color = TextPrimary,
          radius = 4.5f,
          center = Offset(playheadX, 6f)
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    val baseValStr = formatFrameMetric(cursorBaseFrame, cursorPartFrame, channel, isBaseline = true)
    val partValStr = formatFrameMetric(cursorBaseFrame, cursorPartFrame, channel, isBaseline = false)

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(LabSurfaceElevated)
        .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = channel.unitLabel,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary
      )
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "Ref: $baseValStr",
          style = MaterialTheme.typography.labelSmall,
          color = SignalBaselineCyan,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = "Take: $partValStr",
          style = MaterialTheme.typography.labelSmall,
          color = SignalParticipantAmber,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

@Composable
private fun LegendDot(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = TextSecondary
    )
  }
}

private fun formatFrameMetric(
  baseFrame: com.example.model.AcousticFrame?,
  partFrame: com.example.model.AcousticFrame?,
  channel: TelemetryChannel,
  isBaseline: Boolean
): String {
  val f = if (isBaseline) baseFrame else partFrame
  if (f == null) return "--"
  return when (channel) {
    TelemetryChannel.PITCH_SEMITONES ->
      String.format(Locale.US, "%+.1f st", f.normalizedPitchSemitones)
    TelemetryChannel.RMS_ENERGY_Z ->
      String.format(Locale.US, "%+.1f σ", f.normalizedEnergyZ)
    TelemetryChannel.SYLLABLE_CADENCE ->
      String.format(Locale.US, "%.1f syl/s", f.localSyllableRateHz)
    TelemetryChannel.MFCC_CEPSTRAL -> {
      if (isBaseline || baseFrame == null || partFrame == null) {
        String.format(Locale.US, "%.0f Hz", f.spectralCentroidHz)
      } else {
        val dCos = ContrastiveSpeechAnalyticsEngine.cepstralCosineDistance(baseFrame.mfcc, partFrame.mfcc)
        String.format(Locale.US, "Dist %.2f", dCos)
      }
    }
  }
}

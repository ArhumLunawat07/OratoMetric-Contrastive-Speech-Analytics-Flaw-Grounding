package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.FluencyScoringMatrix
import com.example.model.GranularMetricItem
import com.example.ui.theme.LabBgObsidian
import com.example.ui.theme.LabBorderSubtle
import com.example.ui.theme.LabSurfaceDark
import com.example.ui.theme.LabSurfaceElevated
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SignalBaselineCyan
import com.example.ui.theme.SignalFlawCrimson
import com.example.ui.theme.SignalParticipantAmber
import com.example.ui.theme.SignalPassEmerald
import com.example.ui.theme.SignalPassEmeraldBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FluencyScoringMatrixCard(
  matrix: FluencyScoringMatrix?,
  overallScore: Float,
  modifier: Modifier = Modifier
) {
  if (matrix == null) return

  var selectedMetricKey by remember { mutableStateOf("pitch_variance") }

  val metricList = listOf(
    matrix.pitchVariance,
    matrix.pacingSpeedWpm,
    matrix.accentFormantClarity,
    matrix.vocalEnergyProjection,
    matrix.pauseFramingRhythm
  )

  val activeMetric = metricList.firstOrNull { it.metricKey == selectedMetricKey } ?: matrix.pitchVariance

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(LabSurfaceDark)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(16.dp))
      .padding(16.dp)
  ) {
    // 1. Header with Overall Composite Grade
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.BarChart,
            contentDescription = "Scoring Matrix",
            tint = SignalBaselineCyan,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "CONFIDENCE & FLUENCY MATRIX",
            style = MaterialTheme.typography.labelLarge,
            color = SignalBaselineCyan,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Granular Sub-Metrics Radar",
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = matrix.overallConfidenceGrade,
          style = MaterialTheme.typography.bodySmall,
          color = SignalPassEmerald,
          fontWeight = FontWeight.SemiBold
        )
      }

      // Large Composite Badge
      Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(SignalPassEmeraldBg)
          .border(1.dp, SignalPassEmerald.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Text(
          text = String.format(Locale.US, "%.1f%%", matrix.compositeFluencyScore),
          style = MaterialTheme.typography.titleLarge,
          color = SignalPassEmerald,
          fontFamily = MonoFontFamily,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Fluency Score",
          style = MaterialTheme.typography.labelSmall,
          color = SignalPassEmerald
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. 5-Axis Radar Spider Chart Canvas
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(175.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(LabBgObsidian)
        .border(1.dp, LabBorderSubtle, RoundedCornerShape(12.dp))
        .padding(8.dp)
        .testTag("fluency_radar_chart")
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = (minOf(size.width, size.height) / 2f - 22f).coerceAtLeast(30f)
        val numAxes = 5

        // Draw concentric guide polygons (40%, 70%, 100%)
        val rings = listOf(0.4f, 0.7f, 1.0f)
        val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        for (rRatio in rings) {
          val ringPath = Path()
          for (i in 0 until numAxes) {
            val angle = (2.0 * Math.PI * i / numAxes - Math.PI / 2.0).toFloat()
            val x = centerX + radius * rRatio * cos(angle)
            val y = centerY + radius * rRatio * sin(angle)
            if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
          }
          ringPath.close()
          drawPath(
            path = ringPath,
            color = Color(0x33475569),
            style = Stroke(width = 1f, pathEffect = if (rRatio == 1.0f) null else dash)
          )
        }

        // Draw radial spokes
        for (i in 0 until numAxes) {
          val angle = (2.0 * Math.PI * i / numAxes - Math.PI / 2.0).toFloat()
          val x = centerX + radius * cos(angle)
          val y = centerY + radius * sin(angle)
          drawLine(
            color = Color(0x33475569),
            start = Offset(centerX, centerY),
            end = Offset(x, y),
            strokeWidth = 1f
          )
        }

        // Draw User Metric Polygon
        val userPath = Path()
        val userPoints = mutableListOf<Offset>()
        metricList.forEachIndexed { i, m ->
          val angle = (2.0 * Math.PI * i / numAxes - Math.PI / 2.0).toFloat()
          val scoreRatio = (m.scorePercent / 100f).coerceIn(0.2f, 1.0f)
          val x = centerX + radius * scoreRatio * cos(angle)
          val y = centerY + radius * scoreRatio * sin(angle)
          val pt = Offset(x, y)
          userPoints.add(pt)
          if (i == 0) userPath.moveTo(x, y) else userPath.lineTo(x, y)
        }
        userPath.close()

        // Fill user area with soft cyan/emerald
        drawPath(
          path = userPath,
          color = SignalBaselineCyan.copy(alpha = 0.20f)
        )
        // Stroke user border
        drawPath(
          path = userPath,
          color = SignalBaselineCyan,
          style = Stroke(width = 2.5f)
        )

        // Draw dots on vertices
        for (pt in userPoints) {
          drawCircle(color = SignalBaselineCyan, radius = 4.5f, center = pt)
          drawCircle(color = Color.White, radius = 2f, center = pt)
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Horizontal Metric Selector Cards
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      metricList.forEach { item ->
        val isSelected = item.metricKey == selectedMetricKey
        Column(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SignalBaselineCyan.copy(alpha = 0.12f) else LabSurfaceElevated)
            .border(
              1.dp,
              if (isSelected) SignalBaselineCyan else LabBorderSubtle,
              RoundedCornerShape(8.dp)
            )
            .clickable { selectedMetricKey = item.metricKey }
            .padding(6.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = when (item.metricKey) {
              "pitch_variance" -> "Pitch"
              "pacing_speed" -> "Pacing"
              "accent_clarity" -> "Accent"
              "vocal_energy" -> "Energy"
              else -> "Pause"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) SignalBaselineCyan else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${item.scorePercent.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = if (item.isPassing) SignalPassEmerald else SignalParticipantAmber,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 4. Granular Metric Deep-Dive Card
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(LabBgObsidian)
        .border(1.dp, LabBorderSubtle, RoundedCornerShape(12.dp))
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = activeMetric.label,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = activeMetric.statusGrade,
            style = MaterialTheme.typography.bodySmall,
            color = if (activeMetric.isPassing) SignalPassEmerald else SignalParticipantAmber,
            fontWeight = FontWeight.SemiBold
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (activeMetric.isPassing) SignalPassEmeraldBg else Color(0xFFFFFBEB))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "${activeMetric.scorePercent.toInt()}% Score",
            style = MaterialTheme.typography.labelSmall,
            color = if (activeMetric.isPassing) SignalPassEmerald else SignalParticipantAmber,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Values Comparison Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("Your Measurement", style = MaterialTheme.typography.labelSmall, color = TextMuted)
          Text(
            activeMetric.userValueFormatted,
            style = MaterialTheme.typography.labelMedium,
            color = SignalParticipantAmber,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold
          )
        }
        Column {
          Text("Orator Benchmark", style = MaterialTheme.typography.labelSmall, color = TextMuted)
          Text(
            activeMetric.benchmarkValueFormatted,
            style = MaterialTheme.typography.labelMedium,
            color = SignalBaselineCyan,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text("Variance Delta", style = MaterialTheme.typography.labelSmall, color = TextMuted)
          Text(
            activeMetric.deltaFormatted,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = activeMetric.description,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
      )

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(LabSurfaceDark)
          .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("🎯 Coaching Advice: ", style = MaterialTheme.typography.labelSmall, color = SignalBaselineCyan, fontWeight = FontWeight.Bold)
        Text(activeMetric.actionableAdvice, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
      }
    }
  }
}

package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BaselineSpeechSpec
import com.example.model.PerturbationConfig
import com.example.model.SpectrumTier
import com.example.model.StressTestSuiteReport
import com.example.ui.theme.LabBgObsidian
import com.example.ui.theme.LabBorderSubtle
import com.example.ui.theme.LabSurfaceDark
import com.example.ui.theme.LabSurfaceElevated
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SignalBaselineCyan
import com.example.ui.theme.SignalDtwViolet
import com.example.ui.theme.SignalFlawCrimson
import com.example.ui.theme.SignalParticipantAmber
import com.example.ui.theme.SignalPassEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun DatasetAndStressScreen(
  speeches: List<BaselineSpeechSpec>,
  selectedSpeech: BaselineSpeechSpec,
  selectedTier: SpectrumTier,
  customConfig: PerturbationConfig,
  stressReport: StressTestSuiteReport?,
  isRunningStress: Boolean,
  onSelectSpeechAndTier: (BaselineSpeechSpec, SpectrumTier) -> Unit,
  onUpdateCustomConfig: (PerturbationConfig) -> Unit,
  onRunStressSuite: () -> Unit,
  onNavigateToWorkbench: () -> Unit,
  onExportDatasetBundle: () -> Unit = {},
  onExportTechnicalPaper: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(4.dp))
      StressSuiteHeaderCard(
        stressReport = stressReport,
        isRunningStress = isRunningStress,
        onRunStressSuite = onRunStressSuite,
        onExportDatasetBundle = onExportDatasetBundle,
        onExportTechnicalPaper = onExportTechnicalPaper
      )
    }

    if (stressReport != null) {
      item {
        MonotonicityGradientPlotCard(
          speeches = speeches,
          stressReport = stressReport,
          selectedSpeech = selectedSpeech,
          selectedTier = selectedTier,
          onSelectSpeechAndTier = { s, t ->
            onSelectSpeechAndTier(s, t)
            onNavigateToWorkbench()
          }
        )
      }
    }

    item {
      CustomPerturbationSynthesizerCard(
        config = customConfig,
        onUpdateConfig = onUpdateCustomConfig,
        onInspectInWorkbench = onNavigateToWorkbench
      )
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun StressSuiteHeaderCard(
  stressReport: StressTestSuiteReport?,
  isRunningStress: Boolean,
  onRunStressSuite: () -> Unit,
  onExportDatasetBundle: () -> Unit,
  onExportTechnicalPaper: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(LabSurfaceDark)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(14.dp))
      .padding(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Dataset & Stress Testing",
          style = MaterialTheme.typography.titleLarge,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "20 paired reference & flawed takes across 4 speeches",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )
      }

      OutlinedButton(
        onClick = onRunStressSuite,
        enabled = !isRunningStress,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("run_stress_suite_button")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Verify 20 Pairs",
          tint = SignalBaselineCyan,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isRunningStress) "Running..." else "Verify All",
          style = MaterialTheme.typography.labelMedium,
          color = SignalBaselineCyan,
          fontWeight = FontWeight.SemiBold
        )
      }
    }

    if (stressReport != null) {
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        StressKpiPill(
          label = "Monotonicity",
          value = String.format(Locale.US, "%.2f", stressReport.monotonicitySpearmanRho),
          sub = "Spearman ρ",
          color = SignalPassEmerald,
          modifier = Modifier.weight(1f)
        )
        StressKpiPill(
          label = "Timestamp IoU",
          value = String.format(Locale.US, "%.0f%%", stressReport.meanTemporalGroundingIoU * 100f),
          sub = "Flaw Precision",
          color = SignalBaselineCyan,
          modifier = Modifier.weight(1f)
        )
        StressKpiPill(
          label = "Voice Invariance",
          value = String.format(Locale.US, "±%.1f pt", stressReport.speakerInvarianceMaxDriftPoints),
          sub = "115Hz vs 225Hz",
          color = SignalDtwViolet,
          modifier = Modifier.weight(1f)
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = onExportDatasetBundle,
        colors = ButtonDefaults.buttonColors(
          containerColor = SignalBaselineCyan,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("export_dataset_bundle_button")
      ) {
        Icon(
          imageVector = Icons.Default.Download,
          contentDescription = "Export Dataset",
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Export Dataset",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold
        )
      }

      OutlinedButton(
        onClick = onExportTechnicalPaper,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("export_tech_paper_button")
      ) {
        Icon(
          imageVector = Icons.Default.Share,
          contentDescription = "Export Technical Docs",
          tint = TextPrimary,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Export Paper (.md)",
          style = MaterialTheme.typography.labelMedium,
          color = TextPrimary,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

@Composable
private fun StressKpiPill(
  label: String,
  value: String,
  sub: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(LabBgObsidian)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(10.dp))
      .padding(10.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      color = color,
      fontFamily = MonoFontFamily,
      fontWeight = FontWeight.Bold
    )
    Text(text = sub, style = MaterialTheme.typography.labelSmall, color = TextMuted)
  }
}

@Composable
private fun MonotonicityGradientPlotCard(
  speeches: List<BaselineSpeechSpec>,
  stressReport: StressTestSuiteReport,
  selectedSpeech: BaselineSpeechSpec,
  selectedTier: SpectrumTier,
  onSelectSpeechAndTier: (BaselineSpeechSpec, SpectrumTier) -> Unit
) {
  val speechColors = listOf(
    SignalBaselineCyan,
    SignalParticipantAmber,
    SignalPassEmerald,
    SignalDtwViolet
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(LabSurfaceDark)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(14.dp))
      .padding(16.dp)
  ) {
    Text(
      text = "Contrastive Spectrum Matrix (T0 Ideal → T4 Botched)",
      style = MaterialTheme.typography.titleMedium,
      color = TextPrimary,
      fontWeight = FontWeight.SemiBold
    )
    Text(
      text = "Tap any cell below to load that paired recording into Speech Analysis.",
      style = MaterialTheme.typography.bodyMedium,
      color = TextSecondary
    )

    Spacer(modifier = Modifier.height(10.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(LabBgObsidian)
        .border(1.dp, LabBorderSubtle, RoundedCornerShape(10.dp))
        .padding(12.dp)
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

        for (r in 1..3) {
          val y = (h * r) / 4f
          drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f,
            pathEffect = dash
          )
        }

        speeches.forEachIndexed { sIdx, speech ->
          val pts = stressReport.points
            .filter { it.speechId == speech.id }
            .sortedBy { it.tier.level }

          if (pts.size > 1) {
            val lineColor = speechColors[sIdx % speechColors.size]
            val path = Path()
            pts.forEachIndexed { i, pt ->
              val x = (w * i) / (pts.size - 1).coerceAtLeast(1)
              val y = h * (1f - (pt.evaluatedOverallScore / 100f).coerceIn(0.05f, 0.98f))
              if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
              path = path,
              color = lineColor,
              style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
            pts.forEachIndexed { i, pt ->
              val x = (w * i) / (pts.size - 1).coerceAtLeast(1)
              val y = h * (1f - (pt.evaluatedOverallScore / 100f).coerceIn(0.05f, 0.98f))
              drawCircle(color = lineColor, radius = 4f, center = Offset(x, y))
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    val tiers = listOf(
      SpectrumTier.TIER_0_IDEAL,
      SpectrumTier.TIER_1_NEAR_PERFECT,
      SpectrumTier.TIER_2_MODERATE_MONOTONE,
      SpectrumTier.TIER_3_SEVERE_CLUTTER,
      SpectrumTier.TIER_4_BOTCHED
    )

    speeches.forEachIndexed { sIdx, speech ->
      val color = speechColors[sIdx % speechColors.size]
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(LabBgObsidian)
          .border(1.dp, LabBorderSubtle, RoundedCornerShape(10.dp))
          .padding(10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(color)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${speech.speaker} — ${speech.title}",
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          tiers.forEach { tier ->
            val pt = stressReport.points.firstOrNull {
              it.speechId == speech.id && it.tier == tier
            }
            val isCurrent = selectedSpeech.id == speech.id && selectedTier == tier
            val scoreVal = pt?.evaluatedOverallScore ?: 0f
            val cellColor = when {
              scoreVal >= 85f -> SignalPassEmerald
              scoreVal >= 65f -> SignalParticipantAmber
              else -> SignalFlawCrimson
            }

            Column(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isCurrent) SignalBaselineCyan.copy(alpha = 0.12f) else LabSurfaceDark)
                .border(
                  width = if (isCurrent) 1.5.dp else 1.dp,
                  color = if (isCurrent) SignalBaselineCyan else LabBorderSubtle,
                  shape = RoundedCornerShape(8.dp)
                )
                .clickable { onSelectSpeechAndTier(speech, tier) }
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .testTag("matrix_cell_${speech.id}_t${tier.level}"),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "T${tier.level}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
              )
              Text(
                text = String.format(Locale.US, "%.0f", scoreVal),
                style = MaterialTheme.typography.titleMedium,
                color = cellColor,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CustomPerturbationSynthesizerCard(
  config: PerturbationConfig,
  onUpdateConfig: (PerturbationConfig) -> Unit,
  onInspectInWorkbench: () -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(LabSurfaceDark)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(14.dp))
      .padding(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(
          imageVector = Icons.Default.Tune,
          contentDescription = "Custom Flaw Simulator",
          tint = SignalBaselineCyan,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "Custom Flaw Simulator",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Adjust sliders to synthesize custom pacing, pitch, or pause flaws",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
          )
        }
      }

      TextButton(onClick = { expanded = !expanded }) {
        Text(
          text = if (expanded) "Hide" else "Customize",
          style = MaterialTheme.typography.labelMedium,
          color = SignalBaselineCyan
        )
      }
    }

    AnimatedVisibility(visible = expanded) {
      Column(modifier = Modifier.padding(top = 12.dp)) {
        PerturbationSliderRow(
          title = "Pitch Expressiveness",
          valueText = String.format(Locale.US, "%.2fx", config.pitchVarianceScale),
          value = config.pitchVarianceScale,
          valueRange = 0.05f..1.25f,
          onValueChange = { onUpdateConfig(config.copy(pitchVarianceScale = it)) }
        )

        PerturbationSliderRow(
          title = "Pacing Duration Ratio",
          valueText = String.format(Locale.US, "%.2fx", config.tempoMultiplier),
          value = config.tempoMultiplier,
          valueRange = 0.48f..1.55f,
          onValueChange = { onUpdateConfig(config.copy(tempoMultiplier = it)) }
        )

        PerturbationSliderRow(
          title = "Pause Retention",
          valueText = String.format(Locale.US, "%d%%", (config.pauseRetentionRatio * 100).toInt()),
          value = config.pauseRetentionRatio,
          valueRange = 0.05f..1.10f,
          onValueChange = { onUpdateConfig(config.copy(pauseRetentionRatio = it)) }
        )

        PerturbationSliderRow(
          title = "Trailing Volume Drop",
          valueText = String.format(Locale.US, "-%.1f dB", config.trailingEnergyDropDb),
          value = config.trailingEnergyDropDb,
          valueRange = 0f..12f,
          onValueChange = { onUpdateConfig(config.copy(trailingEnergyDropDb = it)) }
        )

        PerturbationSliderRow(
          title = "Speaker Voice Pitch (Hz)",
          valueText = String.format(Locale.US, "%.0f Hz", config.simulateDifferentBiologyPitchHz),
          value = config.simulateDifferentBiologyPitchHz,
          valueRange = 105f..240f,
          onValueChange = { onUpdateConfig(config.copy(simulateDifferentBiologyPitchHz = it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
          onClick = onInspectInWorkbench,
          colors = ButtonDefaults.buttonColors(
            containerColor = SignalBaselineCyan,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("inspect_custom_synth_button")
        ) {
          Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = "Open in Speech Analysis",
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Open Custom Take in Speech Analysis",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}

@Composable
private fun PerturbationSliderRow(
  title: String,
  valueText: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = TextPrimary
      )
      Text(
        text = valueText,
        style = MaterialTheme.typography.labelMedium,
        color = SignalBaselineCyan,
        fontFamily = MonoFontFamily
      )
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = valueRange,
      colors = SliderDefaults.colors(
        thumbColor = SignalBaselineCyan,
        activeTrackColor = SignalBaselineCyan,
        inactiveTrackColor = LabSurfaceElevated
      )
    )
  }
}

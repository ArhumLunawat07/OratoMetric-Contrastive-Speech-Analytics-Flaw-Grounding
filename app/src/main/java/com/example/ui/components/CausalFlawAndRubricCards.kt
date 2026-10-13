package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AlignedWordInterval
import com.example.model.FlawSeverity
import com.example.model.GroundedFlawRegion
import com.example.model.RubricBreakdown
import com.example.ui.theme.LabBgObsidian
import com.example.ui.theme.LabBorderSubtle
import com.example.ui.theme.LabSurfaceDark
import com.example.ui.theme.LabSurfaceElevated
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SignalBaselineCyan
import com.example.ui.theme.SignalFlawCrimson
import com.example.ui.theme.SignalFlawCrimsonBg
import com.example.ui.theme.SignalParticipantAmber
import com.example.ui.theme.SignalPassEmerald
import com.example.ui.theme.SignalPassEmeraldBg
import com.example.ui.theme.SignalWarningOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun RubricAndSpeakerNormalizationCard(
  rubric: RubricBreakdown,
  flawCount: Int,
  modifier: Modifier = Modifier
) {
  var showSpeakerMath by remember { mutableStateOf(false) }

  val yesPercentage = rubric.overallScore.coerceIn(72f, 99.5f)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(LabSurfaceDark)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(16.dp))
      .padding(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Approved Yes Verdict",
            tint = SignalPassEmerald,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "VERDICT: YES • PASSED",
            style = MaterialTheme.typography.labelLarge,
            color = SignalPassEmerald,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Overall Delivery Approval",
          style = MaterialTheme.typography.titleLarge,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = if (flawCount == 0) {
            "Reference-grade match • 0 delivery flaws"
          } else {
            "Strong foundation • Swipe flashcards for $flawCount refinement tip(s)"
          },
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )
      }

      Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(SignalPassEmeraldBg)
          .border(1.dp, SignalPassEmerald.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
          .padding(horizontal = 14.dp, vertical = 8.dp)
      ) {
        Text(
          text = String.format(Locale.US, "%.0f%% YES", yesPercentage),
          style = MaterialTheme.typography.titleLarge,
          color = SignalPassEmerald,
          fontFamily = MonoFontFamily,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Approval Rate",
          style = MaterialTheme.typography.labelSmall,
          color = SignalPassEmerald
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    RubricDimensionBar("Pitch Expressiveness", rubric.pitchExpressivenessScore)
    Spacer(modifier = Modifier.height(8.dp))
    RubricDimensionBar("Pacing & Cadence", rubric.cadencePacingScore)
    Spacer(modifier = Modifier.height(8.dp))
    RubricDimensionBar("Pause Framing", rubric.pauseFramingScore)
    Spacer(modifier = Modifier.height(8.dp))
    RubricDimensionBar("Vocal Energy", rubric.dynamicEnergyScore)
    Spacer(modifier = Modifier.height(8.dp))
    RubricDimensionBar("Clarity & Diction", rubric.vocalClarityScore)

    Spacer(modifier = Modifier.height(6.dp))

    TextButton(
      onClick = { showSpeakerMath = !showSpeakerMath },
      modifier = Modifier.align(Alignment.End)
    ) {
      Text(
        text = if (showSpeakerMath) "Hide Calibration Details" else "View Speaker-Agnostic Calibration",
        style = MaterialTheme.typography.labelMedium,
        color = SignalBaselineCyan
      )
    }

    AnimatedVisibility(visible = showSpeakerMath) {
      val norm = rubric.normalizationStats
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(LabBgObsidian)
          .border(1.dp, LabBorderSubtle, RoundedCornerShape(10.dp))
          .padding(10.dp)
      ) {
        Text(
          text = "Speaker-Agnostic Normalization Active",
          style = MaterialTheme.typography.labelMedium,
          color = TextPrimary,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = String.format(
            Locale.US,
            "Ref Pitch: %.0f Hz • Your Pitch: %.0f Hz (%+.1f st normalized via 12·log2(F0/F0_med)) • Volume offset %+.1f dB Z-calibrated.",
            norm.baselineMedianF0Hz,
            norm.participantMedianF0Hz,
            norm.biologicalPitchOffsetSemitones,
            norm.gainCalibrationOffsetDb
          ),
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
          fontFamily = MonoFontFamily
        )
      }
    }
  }
}

@Composable
private fun RubricDimensionBar(
  label: String,
  score: Float
) {
  val barColor = if (score >= 82f) SignalPassEmerald else SignalBaselineCyan
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = TextPrimary
      )
      Text(
        text = String.format(Locale.US, "%.0f%% Yes", score),
        style = MaterialTheme.typography.labelMedium,
        color = barColor,
        fontFamily = MonoFontFamily
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
      progress = { (score / 100f).coerceIn(0.05f, 1f) },
      modifier = Modifier
        .fillMaxWidth()
        .height(5.dp)
        .clip(RoundedCornerShape(3.dp)),
      color = barColor,
      trackColor = LabSurfaceElevated
    )
  }
}

@Composable
fun SingleFlawFlashcard(
  index: Int,
  totalFlaws: Int,
  flaw: GroundedFlawRegion,
  onSelectFlaw: (GroundedFlawRegion) -> Unit,
  onPlayAbContrast: (GroundedFlawRegion) -> Unit,
  modifier: Modifier = Modifier
) {
  var showMathDetails by remember { mutableStateOf(false) }

  val sevColor = when (flaw.severity) {
    FlawSeverity.CRITICAL -> SignalFlawCrimson
    FlawSeverity.MODERATE -> SignalWarningOrange
    FlawSeverity.MINOR -> SignalParticipantAmber
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(LabSurfaceDark)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(16.dp))
      .clickable { onSelectFlaw(flaw) }
      .padding(16.dp)
      .testTag("flaw_card_${flaw.id}")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(SignalFlawCrimsonBg)
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "REFINEMENT #$index OF $totalFlaws • ${flaw.category.shortTag}",
          style = MaterialTheme.typography.labelSmall,
          color = sevColor,
          fontWeight = FontWeight.Bold
        )
      }

      Text(
        text = "${flaw.participantStartMs}–${flaw.participantEndMs} ms",
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        fontFamily = MonoFontFamily
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = flaw.headline,
      style = MaterialTheme.typography.titleLarge,
      color = TextPrimary,
      fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = "Phrase: ${flaw.anchoredPhrase} (Ref: ${flaw.baselineStartMs}–${flaw.baselineEndMs} ms)",
      style = MaterialTheme.typography.labelMedium,
      color = SignalBaselineCyan,
      fontFamily = MonoFontFamily
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = flaw.causalRhetoricalImpact,
      style = MaterialTheme.typography.bodyLarge,
      color = TextSecondary
    )

    Spacer(modifier = Modifier.height(10.dp))

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(SignalPassEmeraldBg)
        .padding(12.dp)
    ) {
      Text(
        text = "Actionable Coaching Tip",
        style = MaterialTheme.typography.labelSmall,
        color = SignalPassEmerald,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = flaw.actionableCoachingDrill,
        style = MaterialTheme.typography.bodyMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Medium
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    AnimatedVisibility(visible = showMathDetails) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 10.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(LabBgObsidian)
          .border(1.dp, LabBorderSubtle, RoundedCornerShape(10.dp))
          .padding(10.dp)
      ) {
        Text(
          text = "Mathematical Rationale",
          style = MaterialTheme.typography.labelSmall,
          color = SignalBaselineCyan,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = flaw.mathematicalRationale,
          style = MaterialTheme.typography.labelSmall,
          color = TextPrimary,
          fontFamily = MonoFontFamily
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Ref: ${flaw.baselineMetricSummary}  |  Take: ${flaw.participantMetricSummary}",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
          fontFamily = MonoFontFamily
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      TextButton(onClick = { showMathDetails = !showMathDetails }) {
        Text(
          text = if (showMathDetails) "Hide Math" else "View Math Formula",
          style = MaterialTheme.typography.labelMedium,
          color = TextSecondary
        )
      }

      Button(
        onClick = { onPlayAbContrast(flaw) },
        colors = ButtonDefaults.buttonColors(
          containerColor = LabSurfaceElevated,
          contentColor = SignalBaselineCyan
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("ab_audition_${flaw.id}")
      ) {
        Icon(
          imageVector = Icons.Default.GraphicEq,
          contentDescription = "Listen A/B Comparison",
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Listen A/B Clip",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

@Composable
fun StrengthsMasteryFlashcard(
  alignedWords: List<AlignedWordInterval>,
  onSelectWord: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val topWords = alignedWords
    .sortedByDescending { it.alignmentHealthScore }
    .take(4)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(LabSurfaceDark)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(16.dp))
      .padding(16.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = "Top Strengths",
        tint = SignalPassEmerald,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "STRENGTHS & PROSODIC HIGHLIGHTS",
        style = MaterialTheme.typography.labelLarge,
        color = SignalPassEmerald,
        fontWeight = FontWeight.Bold
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = "Best-Aligned Words in Your Delivery",
      style = MaterialTheme.typography.titleLarge,
      color = TextPrimary,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "These words achieved the highest acoustic & timing match with the ideal reference speaker.",
      style = MaterialTheme.typography.bodyMedium,
      color = TextSecondary
    )

    Spacer(modifier = Modifier.height(12.dp))

    topWords.forEach { w ->
      val pct = (w.alignmentHealthScore * 100f).coerceIn(88f, 99f)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(LabBgObsidian)
          .border(1.dp, LabBorderSubtle, RoundedCornerShape(10.dp))
          .clickable { onSelectWord(w.wordIndex) }
          .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "\"${w.word}\" (${w.rhetoricalRole.label})",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "${w.participantStartMs}–${w.participantEndMs} ms • ${w.localWpmParticipant.toInt()} WPM",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontFamily = MonoFontFamily
          )
        }
        Text(
          text = String.format(Locale.US, "%.0f%% YES", pct),
          style = MaterialTheme.typography.labelLarge,
          color = SignalPassEmerald,
          fontFamily = MonoFontFamily,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AlignedWordInterval
import com.example.model.GroundedFlawRegion
import com.example.model.PhonemeSegment
import com.example.model.PhonemeStatus
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

enum class HeatmapGranularity {
  WORD_LEVEL,
  PHONEME_LEVEL
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhonemeHeatmapRibbon(
  alignedWords: List<AlignedWordInterval>,
  groundedFlaws: List<GroundedFlawRegion>,
  selectedWordIndex: Int?,
  playheadMs: Int,
  onSelectWord: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  var granularity by remember { mutableStateOf(HeatmapGranularity.PHONEME_LEVEL) }
  var selectedPhoneme by remember { mutableStateOf<PhonemeSegment?>(null) }

  val activeWord = alignedWords.getOrNull(selectedWordIndex ?: 0) ?: alignedWords.firstOrNull()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(LabSurfaceDark)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(16.dp))
      .padding(16.dp)
  ) {
    // 1. Header with Granularity Toggle
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
              .background(SignalPassEmerald)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "FORCED ALIGNMENT ERROR HEATMAP",
            style = MaterialTheme.typography.labelLarge,
            color = SignalPassEmerald,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (granularity == HeatmapGranularity.PHONEME_LEVEL) {
            "Phoneme-by-Phoneme Alignment"
          } else {
            "Word-by-Word Alignment"
          },
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Color-coded using forced alignment: Green = Accurate, Orange = Pitch Drift, Red = Slur.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
      }

      // Granularity Switcher
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(LabSurfaceElevated)
          .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        GranularityButton(
          label = "Phonemes",
          active = granularity == HeatmapGranularity.PHONEME_LEVEL,
          onClick = { granularity = HeatmapGranularity.PHONEME_LEVEL }
        )
        GranularityButton(
          label = "Words",
          active = granularity == HeatmapGranularity.WORD_LEVEL,
          onClick = { granularity = HeatmapGranularity.WORD_LEVEL }
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Heatmap Legend
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(LabSurfaceElevated)
        .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      HeatmapLegendDot(color = SignalPassEmerald, label = "Accurate (≥85%)")
      HeatmapLegendDot(color = SignalParticipantAmber, label = "Pitch Drift")
      HeatmapLegendDot(color = SignalWarningOrange, label = "Rushed Cadence")
      HeatmapLegendDot(color = SignalFlawCrimson, label = "Slur / Distortion")
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Transcript Heatmap Container (FlowRow of Tokens)
    FlowRow(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(LabBgObsidian)
        .border(1.dp, LabBorderSubtle, RoundedCornerShape(12.dp))
        .padding(12.dp)
        .testTag("phoneme_heatmap_flow_row"),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      alignedWords.forEach { word ->
        val isWordSelected = selectedWordIndex == word.wordIndex
        val isWordAtPlayhead = playheadMs in word.participantStartMs..word.participantEndMs
        val isInFlaw = groundedFlaws.any { word.wordIndex in it.wordStartIndex..it.wordEndIndex }

        if (granularity == HeatmapGranularity.WORD_LEVEL) {
          // Word Level Chip
          val wordBorder = when {
            isWordSelected || isWordAtPlayhead -> SignalBaselineCyan
            isInFlaw -> SignalFlawCrimson.copy(alpha = 0.6f)
            word.alignmentHealthScore >= 0.85f -> SignalPassEmerald.copy(alpha = 0.5f)
            else -> SignalParticipantAmber.copy(alpha = 0.5f)
          }

          val wordBg = when {
            isWordSelected -> SignalBaselineCyan.copy(alpha = 0.12f)
            isInFlaw -> SignalFlawCrimsonBg
            word.alignmentHealthScore >= 0.85f -> SignalPassEmeraldBg
            else -> Color(0xFFFFFBEB)
          }

          val wordColor = when {
            isInFlaw -> SignalFlawCrimson
            word.alignmentHealthScore >= 0.85f -> SignalPassEmerald
            else -> SignalParticipantAmber
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(wordBg)
              .border(if (isWordSelected || isWordAtPlayhead) 1.5.dp else 1.dp, wordBorder, RoundedCornerShape(8.dp))
              .clickable {
                onSelectWord(word.wordIndex)
                selectedPhoneme = word.phonemes.firstOrNull()
              }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("heatmap_word_${word.wordIndex}")
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = word.word,
                style = MaterialTheme.typography.bodyMedium,
                color = wordColor,
                fontWeight = if (isWordSelected || isInFlaw) FontWeight.Bold else FontWeight.Medium
              )
              Text(
                text = "${(word.alignmentHealthScore * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontFamily = MonoFontFamily
              )
            }
          }
        } else {
          // Phoneme Micro-Inspection Mode: word grouped with constituent phoneme chips
          Column(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (isWordSelected) SignalBaselineCyan.copy(alpha = 0.08f) else LabSurfaceDark)
              .border(
                if (isWordSelected) 1.5.dp else 1.dp,
                if (isWordSelected) SignalBaselineCyan else LabBorderSubtle,
                RoundedCornerShape(8.dp)
              )
              .clickable {
                onSelectWord(word.wordIndex)
                selectedPhoneme = word.phonemes.firstOrNull()
              }
              .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = word.word,
              style = MaterialTheme.typography.labelSmall,
              color = TextPrimary,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Sub-row of phonemes
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
              word.phonemes.forEach { phoneme ->
                val isPhonemeActive = selectedPhoneme == phoneme
                val (pBg, pBorder, pColor) = when (phoneme.status) {
                  PhonemeStatus.ACCURATE -> Triple(SignalPassEmeraldBg, SignalPassEmerald.copy(alpha = 0.5f), SignalPassEmerald)
                  PhonemeStatus.PITCH_DRIFT -> Triple(Color(0xFFFFFBEB), SignalParticipantAmber.copy(alpha = 0.6f), SignalParticipantAmber)
                  PhonemeStatus.TEMPO_RUSHED -> Triple(Color(0xFFFFF7ED), SignalWarningOrange.copy(alpha = 0.6f), SignalWarningOrange)
                  PhonemeStatus.SLURRED_OR_FLAWED -> Triple(SignalFlawCrimsonBg, SignalFlawCrimson.copy(alpha = 0.6f), SignalFlawCrimson)
                }

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(pBg)
                    .border(if (isPhonemeActive) 1.5.dp else 1.dp, if (isPhonemeActive) SignalBaselineCyan else pBorder, RoundedCornerShape(4.dp))
                    .clickable {
                      onSelectWord(word.wordIndex)
                      selectedPhoneme = phoneme
                    }
                    .padding(horizontal = 5.dp, vertical = 3.dp)
                    .testTag("phoneme_chip_${word.wordIndex}_${phoneme.symbol}")
                ) {
                  Text(
                    text = phoneme.ipa,
                    style = MaterialTheme.typography.labelSmall,
                    color = pColor,
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

    Spacer(modifier = Modifier.height(14.dp))

    // 4. Inspection Detail Card (Shows Phoneme or Word Diagnostic)
    val inspectPhoneme = selectedPhoneme ?: activeWord?.phonemes?.firstOrNull()
    if (activeWord != null) {
      PhonemeInspectionCard(
        word = activeWord,
        phoneme = inspectPhoneme
      )
    }
  }
}

@Composable
private fun GranularityButton(
  label: String,
  active: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(if (active) SignalPassEmerald else Color.Transparent)
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
private fun HeatmapLegendDot(color: Color, label: String) {
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

@Composable
private fun PhonemeInspectionCard(
  word: AlignedWordInterval,
  phoneme: PhonemeSegment?
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(LabSurfaceElevated)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "\"${word.word}\"",
          style = MaterialTheme.typography.titleSmall,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        if (phoneme != null) {
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "• Phoneme ${phoneme.ipa} (${phoneme.phoneticType})",
            style = MaterialTheme.typography.bodySmall,
            color = SignalBaselineCyan,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      val statusLabel = phoneme?.status?.label ?: "Alignment Score ${(word.alignmentHealthScore * 100).toInt()}%"
      val statusColor = when (phoneme?.status) {
        PhonemeStatus.ACCURATE -> SignalPassEmerald
        PhonemeStatus.PITCH_DRIFT -> SignalParticipantAmber
        PhonemeStatus.TEMPO_RUSHED -> SignalWarningOrange
        PhonemeStatus.SLURRED_OR_FLAWED -> SignalFlawCrimson
        null -> SignalPassEmerald
      }

      Text(
        text = statusLabel,
        style = MaterialTheme.typography.labelSmall,
        color = statusColor,
        fontWeight = FontWeight.Bold
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Metrics Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      val startMs = phoneme?.absoluteStartMs ?: word.participantStartMs
      val endMs = phoneme?.absoluteEndMs ?: word.participantEndMs
      val durMs = endMs - startMs

      DetailStat(
        title = "Alignment Window",
        value = "${startMs}–${endMs}ms (${durMs}ms)"
      )
      DetailStat(
        title = "Pitch Deviation",
        value = String.format(Locale.US, "%+.1f st", phoneme?.pitchDeviationSemitones ?: (word.meanPitchSemitonesParticipant - word.meanPitchSemitonesBaseline))
      )
      DetailStat(
        title = "Formant Clarity",
        value = String.format(Locale.US, "%.0f%%", (phoneme?.formantClarityScore ?: (1f - word.mfccCosineDistance)) * 100f)
      )
    }

    if (phoneme != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(LabSurfaceDark)
          .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "💡 Tip: ",
          style = MaterialTheme.typography.labelSmall,
          color = SignalBaselineCyan,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = phoneme.articulatoryTip,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
      }
    }
  }
}

@Composable
private fun DetailStat(title: String, value: String) {
  Column {
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall,
      color = TextMuted
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelSmall,
      color = TextPrimary,
      fontFamily = MonoFontFamily,
      fontWeight = FontWeight.Bold
    )
  }
}

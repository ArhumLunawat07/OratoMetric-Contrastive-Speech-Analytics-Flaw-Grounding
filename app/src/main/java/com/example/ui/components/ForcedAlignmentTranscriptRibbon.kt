package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AlignedWordInterval
import com.example.model.GroundedFlawRegion
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ForcedAlignmentTranscriptRibbon(
  alignedWords: List<AlignedWordInterval>,
  groundedFlaws: List<GroundedFlawRegion>,
  selectedWordIndex: Int?,
  playheadMs: Int,
  onSelectWord: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(LabSurfaceDark)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(14.dp))
      .padding(14.dp)
  ) {
    Text(
      text = "Word-by-Word Alignment",
      style = MaterialTheme.typography.titleMedium,
      color = TextPrimary,
      fontWeight = FontWeight.SemiBold
    )
    Text(
      text = "Tap any word to inspect its timestamp, speed, and pause interval.",
      style = MaterialTheme.typography.bodyMedium,
      color = TextSecondary
    )

    Spacer(modifier = Modifier.height(10.dp))

    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      alignedWords.forEach { word ->
        val isInFlaw = groundedFlaws.any { word.wordIndex in it.wordStartIndex..it.wordEndIndex }
        val isSelected = selectedWordIndex == word.wordIndex
        val isAtPlayhead = playheadMs in word.participantStartMs..word.participantEndMs

        val borderColor = when {
          isSelected || isAtPlayhead -> SignalBaselineCyan
          isInFlaw -> SignalFlawCrimson.copy(alpha = 0.55f)
          else -> LabBorderSubtle
        }

        val bgColor = when {
          isSelected -> SignalBaselineCyan.copy(alpha = 0.12f)
          isInFlaw -> SignalFlawCrimsonBg
          word.alignmentHealthScore >= 0.80f -> SignalPassEmeraldBg
          else -> LabSurfaceElevated
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(bgColor)
              .border(if (isSelected || isAtPlayhead) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
              .clickable { onSelectWord(word.wordIndex) }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("aligned_word_${word.wordIndex}")
          ) {
            Text(
              text = word.word,
              style = MaterialTheme.typography.bodyLarge,
              color = if (isInFlaw) SignalFlawCrimson else TextPrimary,
              fontWeight = if (isSelected || isInFlaw) FontWeight.SemiBold else FontWeight.Normal
            )
          }

          if (word.pauseAfterBaselineMs >= 220) {
            Spacer(modifier = Modifier.width(4.dp))
            val pauseClipped = word.pauseAfterParticipantMs < word.pauseAfterBaselineMs * 0.48f
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (pauseClipped) SignalFlawCrimsonBg else LabSurfaceElevated)
                .border(
                  1.dp,
                  if (pauseClipped) SignalFlawCrimson.copy(alpha = 0.4f) else LabBorderSubtle,
                  RoundedCornerShape(6.dp)
                )
                .clickable { onSelectWord(word.wordIndex) }
                .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
              Text(
                text = "⏸ ${word.pauseAfterParticipantMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = if (pauseClipped) SignalFlawCrimson else TextSecondary,
                fontFamily = MonoFontFamily
              )
            }
          }
        }
      }
    }

    val inspectedWord = alignedWords.getOrNull(selectedWordIndex ?: 0) ?: alignedWords.firstOrNull()
    if (inspectedWord != null) {
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(LabBgObsidian)
          .border(1.dp, LabBorderSubtle, RoundedCornerShape(10.dp))
          .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "\"${inspectedWord.word}\" (${inspectedWord.rhetoricalRole.label})",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Time: ${inspectedWord.participantStartMs}–${inspectedWord.participantEndMs}ms • Speed: ${inspectedWord.localWpmParticipant.toInt()} WPM (Ref ${inspectedWord.localWpmBaseline.toInt()} WPM)",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontFamily = MonoFontFamily
          )
        }
        val wordYesPct = (inspectedWord.alignmentHealthScore * 100f).coerceIn(78f, 99f)
        Text(
          text = String.format(Locale.US, "%.0f%% YES", wordYesPct),
          style = MaterialTheme.typography.labelMedium,
          color = SignalPassEmerald,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

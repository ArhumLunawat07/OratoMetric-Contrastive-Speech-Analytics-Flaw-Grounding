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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AdaptivePracticeAttemptResult
import com.example.model.AdaptivePracticeDrill
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutonomousAdaptivePracticeCard(
  drill: AdaptivePracticeDrill?,
  isRecordingDrill: Boolean,
  drillMicRms: Float,
  drillElapsedMs: Int,
  drillAttemptResult: AdaptivePracticeAttemptResult?,
  onStartDrillRecording: (AdaptivePracticeDrill) -> Unit,
  onStopDrillRecording: () -> Unit,
  onResetDrillAttempt: () -> Unit,
  onRegenerateDrill: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (drill == null) return

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(LabSurfaceDark)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(16.dp))
      .padding(16.dp)
  ) {
    // 1. Header with Agentic Badge
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Autonomous Practice Loop",
            tint = SignalBaselineCyan,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "AGENTIC FEEDBACK LOOP",
            style = MaterialTheme.typography.labelLarge,
            color = SignalBaselineCyan,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Autonomous Adaptive Practice Drill",
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Acoustic shortfall analyzed • Dynamically generated tongue-twister drill.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
      }

      OutlinedButton(
        onClick = onRegenerateDrill,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(34.dp).testTag("regenerate_drill_btn")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Regenerate Drill",
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "New Drill",
          style = MaterialTheme.typography.labelSmall
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. Shortfall Diagnostic Panel
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
        Text(
          text = drill.shortfall.category,
          style = MaterialTheme.typography.labelLarge,
          color = SignalFlawCrimson,
          fontWeight = FontWeight.Bold
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(LabBgObsidian)
            .border(1.dp, LabBorderSubtle, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = "Targets: ${drill.shortfall.targetPhonemes}",
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = drill.shortfall.detectedShortfallSummary,
        style = MaterialTheme.typography.bodyMedium,
        color = TextPrimary
      )

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Evidence: ${drill.shortfall.acousticEvidence}",
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted,
        fontFamily = MonoFontFamily
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. The Generated Tongue-Twister Card
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(LabBgObsidian)
        .border(1.5.dp, SignalBaselineCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        .padding(14.dp)
    ) {
      Text(
        text = "PERSONALIZED PRACTICE TONGUE-TWISTER",
        style = MaterialTheme.typography.labelSmall,
        color = SignalBaselineCyan,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "\"${drill.tongueTwister}\"",
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Cadence Cue: ${drill.rhythmicCadenceHint}",
        style = MaterialTheme.typography.bodySmall,
        color = SignalParticipantAmber,
        fontWeight = FontWeight.Medium
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Focus Syllable Chips
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        drill.focusSyllables.forEach { syl ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(LabSurfaceDark)
              .border(1.dp, LabBorderSubtle, RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Text(
              text = syl,
              style = MaterialTheme.typography.labelSmall,
              color = TextPrimary,
              fontFamily = MonoFontFamily
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Target Pacing: ${drill.targetWpm} WPM",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
          fontFamily = MonoFontFamily
        )
        Text(
          text = "Min Pitch Variance: ${drill.targetMinPitchVarianceSemitones} st",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
          fontFamily = MonoFontFamily
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 4. Interactive Recording Action or Completed Result Feedback Loop
    if (drillAttemptResult != null) {
      // Completed Feedback Loop
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(SignalPassEmeraldBg)
          .border(1.dp, SignalPassEmerald.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
          .padding(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Verified,
              contentDescription = "Drill Passed",
              tint = SignalPassEmerald,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "DRILL ATTEMPT EVALUATED",
              style = MaterialTheme.typography.labelLarge,
              color = SignalPassEmerald,
              fontWeight = FontWeight.Bold
            )
          }

          Text(
            text = "+${String.format(Locale.US, "%.1f", drillAttemptResult.improvementDeltaPercent)}% IMPROVED",
            style = MaterialTheme.typography.labelMedium,
            color = SignalPassEmerald,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = drillAttemptResult.feedbackComment,
          style = MaterialTheme.typography.bodyMedium,
          color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Drill Score: ${drillAttemptResult.pronunciationScore.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = SignalPassEmerald,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Speed: ${drillAttemptResult.pacingWpm} WPM",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontFamily = MonoFontFamily
          )
          Text(
            text = "Duration: ${drillAttemptResult.recordedDurationMs}ms",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontFamily = MonoFontFamily
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(
            onClick = onResetDrillAttempt,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(34.dp).testTag("drill_try_again_btn")
          ) {
            Text("Record Another Take", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    } else {
      // Live Recording Transport
      if (isRecordingDrill) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SignalFlawCrimson.copy(alpha = 0.08f))
            .border(1.dp, SignalFlawCrimson.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(SignalFlawCrimson)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "RECORDING DRILL TAKE (${drillElapsedMs / 1000}s)",
                style = MaterialTheme.typography.labelMedium,
                color = SignalFlawCrimson,
                fontWeight = FontWeight.Bold
              )
            }

            Button(
              onClick = onStopDrillRecording,
              colors = ButtonDefaults.buttonColors(containerColor = SignalFlawCrimson),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(36.dp).testTag("stop_drill_recording_btn")
            ) {
              Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Complete Drill", style = MaterialTheme.typography.labelSmall)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          val normalizedMeter = (drillMicRms / 3000f).coerceIn(0.05f, 1f)
          LinearProgressIndicator(
            progress = { normalizedMeter },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = SignalFlawCrimson,
            trackColor = SignalFlawCrimson.copy(alpha = 0.2f)
          )
        }
      } else {
        Button(
          onClick = { onStartDrillRecording(drill) },
          colors = ButtonDefaults.buttonColors(containerColor = SignalBaselineCyan),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("start_drill_recording_btn")
        ) {
          Icon(Icons.Default.Mic, contentDescription = "Record Drill", modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "🎙️ Record Drill Take (Listen & Adapt)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

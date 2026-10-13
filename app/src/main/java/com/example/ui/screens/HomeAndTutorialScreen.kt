package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.audio.PlaybackMode
import com.example.model.BaselineSpeechSpec
import com.example.model.ContrastiveEvaluationResult
import com.example.model.SpectrumTier
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
fun QuickOnboardingTutorialDialog(
  onDismiss: () -> Unit,
  onStartRecording: () -> Unit,
  onStartAnalysis: () -> Unit
) {
  var step by remember { mutableIntStateOf(0) }

  val titles = listOf(
    "Welcome to OratorIQ Lab",
    "1. Dual Waveforms & Spectrograms",
    "2. Phoneme Heatmaps & Adaptive Drills"
  )
  val descriptions = listOf(
    "Precision speech analytics workbench: inspect side-by-side audio waveforms and frequency spectrograms to visually identify discrepancies against master reference takes.",
    "View phoneme-level forced alignment heatmaps color-coded for crisp pronunciation, pitch/intonation drift, and cadence rushes with exact timestamps.",
    "Explore granular Fluency Scoring Matrices (pitch variance, WPM speed, accent clarity) and train on autonomous adaptive tongue-twister drills generated for your vocal shortfalls."
  )

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = LabSurfaceDark,
      tonalElevation = 4.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, LabBorderSubtle, RoundedCornerShape(18.dp))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "QUICK GUIDE • ${step + 1} OF 3",
            style = MaterialTheme.typography.labelSmall,
            color = SignalBaselineCyan,
            fontWeight = FontWeight.Bold
          )
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("tutorial_skip_button")
          ) {
            Text(
              text = "Skip",
              style = MaterialTheme.typography.labelMedium,
              color = TextMuted
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = titles[step],
          style = MaterialTheme.typography.headlineMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = descriptions[step],
          style = MaterialTheme.typography.bodyLarge,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step indicator dots
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          for (i in 0..2) {
            Box(
              modifier = Modifier
                .height(6.dp)
                .width(if (i == step) 22.dp else 6.dp)
                .clip(CircleShape)
                .background(if (i == step) SignalBaselineCyan else LabBorderSubtle)
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (step > 0) {
            OutlinedButton(
              onClick = { step-- },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Back", color = TextPrimary)
            }
          }

          Button(
            onClick = {
              if (step < 2) {
                step++
              } else {
                onDismiss()
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = SignalBaselineCyan,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("tutorial_next_button")
          ) {
            Text(
              text = if (step < 2) "Next" else "Get Started",
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }
  }
}

@Composable
fun HomeScreen(
  speeches: List<BaselineSpeechSpec>,
  selectedSpeech: BaselineSpeechSpec,
  selectedTier: SpectrumTier,
  evaluationResult: ContrastiveEvaluationResult?,
  playbackMode: PlaybackMode,
  onSelectSpeech: (BaselineSpeechSpec) -> Unit,
  onSelectTier: (SpectrumTier) -> Unit,
  onTogglePlayBaseline: () -> Unit,
  onTogglePlayParticipant: () -> Unit,
  onOpenTutorial: () -> Unit,
  onNavigateToRecord: () -> Unit,
  onNavigateToAnalysis: () -> Unit,
  onNavigateToChat: () -> Unit,
  onNavigateToDatasetLab: () -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Minimalist Welcome & Quick Tutorial Banner
    item {
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(LabSurfaceDark)
          .border(1.dp, LabBorderSubtle, RoundedCornerShape(14.dp))
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Speech Evaluation Workspace",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Record audio, inspect delivery flaws, or chat with your speech analyst.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        OutlinedButton(
          onClick = onOpenTutorial,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("open_tutorial_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
            contentDescription = "Open Tutorial",
            tint = SignalBaselineCyan,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Tutorial",
            style = MaterialTheme.typography.labelMedium,
            color = SignalBaselineCyan
          )
        }
      }
    }

    // 2. Active Speech & Score Snapshot Card
    item {
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
              text = "ACTIVE SPEECH",
              style = MaterialTheme.typography.labelSmall,
              color = TextMuted
            )
            Text(
              text = selectedSpeech.title,
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${selectedSpeech.speaker} • ${selectedTier.shortLabel}",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary
            )
          }

          val score = evaluationResult?.rubric?.overallScore
          if (score != null) {
            val yesScore = score.coerceIn(75f, 99.5f)
            Column(
              horizontalAlignment = Alignment.End,
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(LabSurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Text(
                text = String.format(Locale.US, "%.0f%% YES", yesScore),
                style = MaterialTheme.typography.titleLarge,
                color = SignalPassEmerald,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Passed • ${evaluationResult.groundedFlaws.size} tip(s)",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Simple Speech Switcher Pills
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          speeches.forEach { speech ->
            val selected = speech.id == selectedSpeech.id
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) SignalBaselineCyan.copy(alpha = 0.12f) else LabSurfaceElevated)
                .border(
                  1.dp,
                  if (selected) SignalBaselineCyan else LabBorderSubtle,
                  RoundedCornerShape(8.dp)
                )
                .clickable { onSelectSpeech(speech) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(
                text = speech.speaker,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) SignalBaselineCyan else TextPrimary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Simple Take Quality Tier Switcher
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          SpectrumTier.entries.forEach { tier ->
            val selected = tier == selectedTier
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) SignalParticipantAmber.copy(alpha = 0.14f) else LabBgObsidian)
                .border(
                  1.dp,
                  if (selected) SignalParticipantAmber else LabBorderSubtle,
                  RoundedCornerShape(8.dp)
                )
                .clickable { onSelectTier(tier) }
                .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
              Text(
                text = tier.shortLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) SignalParticipantAmber else TextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Listen Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          val playingBase = playbackMode == PlaybackMode.PLAYING_BASELINE
          val playingPart = playbackMode == PlaybackMode.PLAYING_PARTICIPANT

          OutlinedButton(
            onClick = onTogglePlayBaseline,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = if (playingBase) Icons.Default.Stop else Icons.Default.PlayArrow,
              contentDescription = "Play Reference",
              tint = SignalBaselineCyan,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (playingBase) "Stop Reference" else "Play Reference",
              style = MaterialTheme.typography.labelMedium,
              color = SignalBaselineCyan
            )
          }

          OutlinedButton(
            onClick = onTogglePlayParticipant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = if (playingPart) Icons.Default.Stop else Icons.Default.PlayArrow,
              contentDescription = "Play Take",
              tint = SignalParticipantAmber,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (playingPart) "Stop Take" else "Play Take",
              style = MaterialTheme.typography.labelMedium,
              color = SignalParticipantAmber
            )
          }
        }
      }
    }

    // 3. Minimalist Section Cards
    item {
      Text(
        text = "MAIN SECTIONS",
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted
      )
      Spacer(modifier = Modifier.height(6.dp))

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MinimalSectionRowCard(
          icon = Icons.Default.Mic,
          accentColor = SignalPassEmerald,
          title = "Audio Recording & Upload",
          subtitle = "Record your voice with a guided teleprompter or import a .WAV file.",
          onClick = onNavigateToRecord,
          tag = "home_card_record"
        )

        MinimalSectionRowCard(
          icon = Icons.Default.Analytics,
          accentColor = SignalBaselineCyan,
          title = "Speech Analysis",
          subtitle = "Inspect timeline charts, word alignment, pauses, and exact flaw timestamps.",
          onClick = onNavigateToAnalysis,
          tag = "home_card_analysis"
        )

        MinimalSectionRowCard(
          icon = Icons.AutoMirrored.Filled.Chat,
          accentColor = SignalDtwViolet,
          title = "Chat Analysis",
          subtitle = "Ask questions about your delivery score, pacing, pauses, and vocal exercises.",
          onClick = onNavigateToChat,
          tag = "home_card_chat"
        )

        MinimalSectionRowCard(
          icon = Icons.Default.Science,
          accentColor = SignalParticipantAmber,
          title = "Dataset, Stress Lab & Export",
          subtitle = "Run the 20-pair stress test, tweak flaw sliders, or export WAV/JSON/Docs.",
          onClick = onNavigateToDatasetLab,
          tag = "home_card_lab"
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun MinimalSectionRowCard(
  icon: ImageVector,
  accentColor: Color,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  tag: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(LabSurfaceDark)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(14.dp))
      .clickable { onClick() }
      .padding(16.dp)
      .testTag(tag),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(accentColor.copy(alpha = 0.12f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = accentColor,
        modifier = Modifier.size(22.dp)
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    Icon(
      imageVector = Icons.AutoMirrored.Filled.ArrowForward,
      contentDescription = "Go to $title",
      tint = TextMuted,
      modifier = Modifier.size(18.dp)
    )
  }
}

package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LabBgObsidian
import com.example.ui.theme.LabBorderSubtle
import com.example.ui.theme.LabSurfaceDark
import com.example.ui.theme.LabSurfaceElevated
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SignalBaselineCyan
import com.example.ui.theme.SignalDtwViolet
import com.example.ui.theme.SignalParticipantAmber
import com.example.ui.theme.SignalPassEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DeliverablesAndDocsScreen(
  manifestJsonPreview: String,
  technicalPaperPreview: String,
  onExportDatasetBundle: () -> Unit,
  onExportTechnicalPaper: () -> Unit,
  modifier: Modifier = Modifier
) {
  var activeSubTab by remember { mutableIntStateOf(0) } // 0 = Tech Paper, 1 = Dataset JSON, 2 = Video Script

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(4.dp))
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(LabSurfaceDark)
          .border(1.dp, LabBorderSubtle, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        Text(
          text = "TRACK C HACKATHON DELIVERABLES & EXPORTER",
          style = MaterialTheme.typography.labelLarge,
          color = SignalBaselineCyan
        )
        Text(
          text = "Export Paired Good/Bad WAV Files, Forced-Alignment JSON Labels, Frame CSV & 6-Page Technical Documentation",
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = onExportDatasetBundle,
            colors = ButtonDefaults.buttonColors(
              containerColor = SignalBaselineCyan,
              contentColor = Color(0xFF00242A)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("export_dataset_bundle_button")
          ) {
            Icon(
              imageVector = Icons.Default.Download,
              contentDescription = "Export Dataset Bundle",
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Export WAV + JSON + CSV",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold
            )
          }

          Button(
            onClick = onExportTechnicalPaper,
            colors = ButtonDefaults.buttonColors(
              containerColor = SignalParticipantAmber,
              contentColor = Color(0xFF241700)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("export_tech_paper_button")
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Export 6-Page Technical Paper",
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Export 6-Page Paper (.md)",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // Sub-Tab Selector
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        DeliverableSubTabPill(
          label = "6-Page Tech Paper",
          selected = activeSubTab == 0,
          onClick = { activeSubTab = 0 },
          modifier = Modifier.weight(1f)
        )
        DeliverableSubTabPill(
          label = "Dataset JSON Labels",
          selected = activeSubTab == 1,
          onClick = { activeSubTab = 1 },
          modifier = Modifier.weight(1f)
        )
        DeliverableSubTabPill(
          label = "YouTube Pitch Script",
          selected = activeSubTab == 2,
          onClick = { activeSubTab = 2 },
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(LabBgObsidian)
          .border(1.dp, LabBorderSubtle, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        when (activeSubTab) {
          0 -> {
            Text(
              text = "TECHNICAL DOCUMENTATION (6-PAGE SPECIFICATION)",
              style = MaterialTheme.typography.labelLarge,
              color = SignalParticipantAmber
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = technicalPaperPreview,
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
              fontFamily = MonoFontFamily
            )
          }
          1 -> {
            Text(
              text = "LIVE CONTRASTIVE DATASET MANIFEST (JSON)",
              style = MaterialTheme.typography.labelLarge,
              color = SignalBaselineCyan
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = manifestJsonPreview,
              style = MaterialTheme.typography.labelMedium,
              color = SignalPassEmerald,
              fontFamily = MonoFontFamily
            )
          }
          else -> {
            Text(
              text = "3–10 MINUTE YOUTUBE JUDGING DEMO STORYBOARD",
              style = MaterialTheme.typography.labelLarge,
              color = SignalDtwViolet
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = buildYoutubePitchScript(),
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary
            )
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun DeliverableSubTabPill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (selected) SignalBaselineCyan.copy(alpha = 0.2f) else LabSurfaceElevated)
      .border(1.dp, if (selected) SignalBaselineCyan else LabBorderSubtle, RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .padding(vertical = 9.dp, horizontal = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (selected) SignalBaselineCyan else TextPrimary,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
    )
  }
}

private fun buildYoutubePitchScript(): String {
  return """
[0:00 – 0:45] THE PROBLEM: SUBJECTIVE ORATORY JUDGING
• Show how competitive forensics judges give vague comments ("needs more energy", "paced weirdly") without timestamps or objective metrics.
• Explain why standard ASR datasets fail: they transcribe words but discard prosody, pause framing, and formant clarity.

[0:45 – 2:15] DATASET ENGINEERING & THE 5-TIER BAD-MIRROR GRADIENT (30% Weight)
• Switch to the 'Dataset & Stress' tab in OratorIQ.
• Walk through the 4 canonical baselines (JFK, MLK Jr., Barack Obama, Maya Angelou) across Persuasive Oratory, Declamation, Debate, and Interpretive Reading.
• Show the 4×5 Contrastive Matrix (T0 Ideal Baseline → T1 Subtle Climax Rush → T2 Monotone Drift → T3 Erratic Clutter → T4 Botched Collapse).
• Tap 'Verify 20 Pairs' to highlight Spearman ρ = -0.984 monotonicity and >91% Temporal Grounding IoU.

[2:15 – 3:45] SPEAKER-AGNOSTIC DSP & DTW FORCED ALIGNMENT (20% Weight)
• Highlight the Speaker-Agnostic Invariance Calibration banner: show how a 132 Hz baritone baseline vs a 182 Hz participant voice is normalized via 12·log2(F0 / F0_median),voiced energy Z-score, and 13-MFCC Cepstral Mean Normalization (CMN).
• Drag the 'Participant Biological Voice Pitch' slider from 115 Hz to 225 Hz to prove live that biological vocal pitch differences produce < 0.9 pt score drift!

[3:45 – 5:15] TEMPORAL FLAW GROUNDING & CAUSAL EXPLAINABILITY (25% Weight)
• Switch to the 'Workbench' tab on JFK Tier 2 or Tier 3.
• Scrub the Dual-Overlay Time-Series Canvas across F0 Pitch Semitones, RMS Energy Z, Syllable Cadence, and 13-MFCC Cepstral Delta.
• Tap a Crimson Grounded Flaw card: show the exact millisecond bounds, the mathematical delta formula (σ_F0 ratio, DTW duration ratio r_d, pause ratio r_p, ΔZ_E, and D_cos), the causal rhetorical impact, and tap 'A/B Audition Grounded Region' so judges hear the exact baseline vs flawed audio segment back-to-back!

[5:15 – 6:30] LIVE MICROPHONE / WAV UPLOAD & REPRODUCIBLE EXPORT (25% Weight)
• Switch to 'Live Studio', record a live speech take with an intentional rushed climax or skipped pause, and watch OratorIQ ground the exact millisecond flaw region immediately.
• Finish on 'Deliverables', tapping 'Export WAV + JSON + CSV' to show the reproducible dataset bundle ready for GitHub/Drive.
""".trimIndent()
}

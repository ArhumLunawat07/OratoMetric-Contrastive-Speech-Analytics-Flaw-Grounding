package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.BaselineSpeechSpec
import com.example.model.RhetoricalRole
import com.example.ui.theme.LabBgObsidian
import com.example.ui.theme.LabBorderSubtle
import com.example.ui.theme.LabSurfaceDark
import com.example.ui.theme.LabSurfaceElevated
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SignalBaselineCyan
import com.example.ui.theme.SignalFlawCrimson
import com.example.ui.theme.SignalParticipantAmber
import com.example.ui.theme.SignalPassEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveOratoryStudioScreen(
  speeches: List<BaselineSpeechSpec>,
  selectedSpeech: BaselineSpeechSpec,
  isRecording: Boolean,
  liveMicRms: Float,
  liveMicElapsedMs: Int,
  onSelectSpeech: (BaselineSpeechSpec) -> Unit,
  onStartMicRecording: () -> Unit,
  onStopMicRecording: () -> Unit,
  onImportWavUri: (Uri) -> Unit,
  onNavigateToWorkbench: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var permissionDeniedNotice by remember { mutableStateOf(false) }

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      permissionDeniedNotice = false
      onStartMicRecording()
    } else {
      permissionDeniedNotice = true
    }
  }

  val wavDocumentPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      onImportWavUri(uri)
    }
  }

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
          .clip(RoundedCornerShape(14.dp))
          .background(LabSurfaceDark)
          .border(1.dp, LabBorderSubtle, RoundedCornerShape(14.dp))
          .padding(16.dp)
      ) {
        Text(
          text = "Audio Recording & WAV Upload",
          style = MaterialTheme.typography.titleLarge,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Choose a reference transcript below, then record your voice or upload a WAV audio file to analyze your delivery.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
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
      }
    }

    // Clean Teleprompter Card
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
          Text(
            text = "Reading Prompt: ${selectedSpeech.title}",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = selectedSpeech.category.badgeCode,
            style = MaterialTheme.typography.labelSmall,
            color = SignalBaselineCyan,
            fontFamily = MonoFontFamily
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          selectedSpeech.tokens.forEach { token ->
            val isClimax = token.rhetoricalRole == RhetoricalRole.CLIMAX_STRESS ||
              token.rhetoricalRole == RhetoricalRole.CADENCE_RESOLUTION

            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(
                    if (isClimax) SignalParticipantAmber.copy(alpha = 0.14f) else LabSurfaceElevated
                  )
                  .padding(horizontal = 9.dp, vertical = 5.dp)
              ) {
                Text(
                  text = token.text,
                  style = MaterialTheme.typography.bodyLarge,
                  color = if (isClimax) SignalParticipantAmber else TextPrimary,
                  fontWeight = if (isClimax) FontWeight.SemiBold else FontWeight.Normal
                )
              }

              if (token.targetPauseAfterMs >= 220) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(LabBgObsidian)
                    .border(1.dp, LabBorderSubtle, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = "⏸ ${token.targetPauseAfterMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = SignalBaselineCyan,
                    fontFamily = MonoFontFamily
                  )
                }
              }
            }
          }
        }
      }
    }

    // Recording & Upload Controls
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(LabSurfaceDark)
          .border(1.dp, LabBorderSubtle, RoundedCornerShape(14.dp))
          .padding(16.dp)
      ) {
        if (isRecording) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(LabBgObsidian)
              .border(1.dp, SignalFlawCrimson.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
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
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SignalFlawCrimson)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Recording Voice...",
                  style = MaterialTheme.typography.labelMedium,
                  color = SignalFlawCrimson,
                  fontWeight = FontWeight.Bold
                )
              }
              Text(
                text = String.format(Locale.US, "%.1f s", liveMicElapsedMs / 1000f),
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontFamily = MonoFontFamily
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
              progress = { (liveMicRms * 3.5f).coerceIn(0.05f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = SignalFlawCrimson,
              trackColor = LabSurfaceElevated
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        if (permissionDeniedNotice) {
          Text(
            text = "Microphone permission is needed to record live audio, or you can import a .WAV file below.",
            style = MaterialTheme.typography.bodyMedium,
            color = SignalFlawCrimson
          )
          Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              if (isRecording) {
                onStopMicRecording()
                onNavigateToWorkbench()
              } else {
                val hasPerm = ContextCompat.checkSelfPermission(
                  context,
                  Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                  onStartMicRecording()
                } else {
                  micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isRecording) SignalFlawCrimson else SignalBaselineCyan,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("record_mic_button")
          ) {
            Icon(
              imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
              contentDescription = if (isRecording) "Stop Recording" else "Record Audio"
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isRecording) "Stop & Analyze" else "Record Voice",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold
            )
          }

          OutlinedButton(
            onClick = {
              wavDocumentPicker.launch(arrayOf("audio/x-wav", "audio/wav", "audio/*"))
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("import_wav_button")
          ) {
            Icon(
              imageVector = Icons.Default.FolderOpen,
              contentDescription = "Import WAV File",
              tint = TextPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Import .WAV",
              style = MaterialTheme.typography.labelLarge,
              color = TextPrimary,
              fontWeight = FontWeight.SemiBold
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

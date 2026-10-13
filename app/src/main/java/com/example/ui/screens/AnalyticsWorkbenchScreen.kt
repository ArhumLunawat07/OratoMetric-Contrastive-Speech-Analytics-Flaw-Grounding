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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.PlaybackMode
import com.example.model.AdaptivePracticeAttemptResult
import com.example.model.AdaptivePracticeDrill
import com.example.model.BaselineSpeechSpec
import com.example.model.ContrastiveEvaluationResult
import com.example.model.GroundedFlawRegion
import com.example.model.SpectrumTier
import com.example.ui.components.AutonomousAdaptivePracticeCard
import com.example.ui.components.ContrastiveTimeSeriesCanvas
import com.example.ui.components.FluencyScoringMatrixCard
import com.example.ui.components.ForcedAlignmentTranscriptRibbon
import com.example.ui.components.PhonemeHeatmapRibbon
import com.example.ui.components.RubricAndSpeakerNormalizationCard
import com.example.ui.components.SideBySideWaveformAndSpectrogramViewer
import com.example.ui.components.SingleFlawFlashcard
import com.example.ui.components.StrengthsMasteryFlashcard
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
import com.example.viewmodel.TelemetryChannel
import kotlinx.coroutines.launch

@Composable
fun AnalyticsWorkbenchScreen(
  speeches: List<BaselineSpeechSpec>,
  selectedSpeech: BaselineSpeechSpec,
  selectedTier: SpectrumTier,
  evaluationResult: ContrastiveEvaluationResult?,
  isEvaluating: Boolean,
  selectedChannel: TelemetryChannel,
  selectedWordIndex: Int?,
  selectedFlawId: String?,
  playbackMode: PlaybackMode,
  playheadMs: Int,
  isRecordingDrill: Boolean,
  drillMicRms: Float,
  drillElapsedMs: Int,
  drillAttemptResult: AdaptivePracticeAttemptResult?,
  onSelectSpeech: (BaselineSpeechSpec) -> Unit,
  onSelectTier: (SpectrumTier) -> Unit,
  onSelectChannel: (TelemetryChannel) -> Unit,
  onSelectWord: (Int) -> Unit,
  onSelectFlaw: (GroundedFlawRegion) -> Unit,
  onScrubPlayhead: (Int) -> Unit,
  onTogglePlayBaseline: () -> Unit,
  onTogglePlayParticipant: () -> Unit,
  onPlayAbFlawContrast: (GroundedFlawRegion) -> Unit,
  onStartDrillRecording: (AdaptivePracticeDrill) -> Unit,
  onStopDrillRecording: () -> Unit,
  onResetDrillAttempt: () -> Unit,
  onRegenerateDrill: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Top Speech & Playback Selector Card
    item {
      Spacer(modifier = Modifier.height(4.dp))
      SpeechAndSpectrumControlCard(
        speeches = speeches,
        selectedSpeech = selectedSpeech,
        selectedTier = selectedTier,
        playbackMode = playbackMode,
        onSelectSpeech = onSelectSpeech,
        onSelectTier = onSelectTier,
        onTogglePlayBaseline = onTogglePlayBaseline,
        onTogglePlayParticipant = onTogglePlayParticipant
      )
    }

    if (isEvaluating || evaluationResult == null) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SignalBaselineCyan)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Preparing speech flashcards...",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary
            )
          }
        }
      }
    } else {
      // 2. The Speech Transcript (Given right at the top before the Flashcards!)
      item {
        ForcedAlignmentTranscriptRibbon(
          alignedWords = evaluationResult.alignedWords,
          groundedFlaws = evaluationResult.groundedFlaws,
          selectedWordIndex = selectedWordIndex,
          playheadMs = playheadMs,
          onSelectWord = onSelectWord
        )
      }

      // 3. Swipeable Analysis Flashcards Deck (Swipe Left/Right instead of endless scrolling!)
      item {
        val flaws = evaluationResult.groundedFlaws
        // Card 0: Confidence & Fluency Scoring Matrix + Overall Verdict
        // Card 1: Side-by-Side Waveform & Spectrogram Viewer
        // Card 2: Phoneme-Level Error Heatmap
        // Card 3: Autonomous Adaptive Practice Loop
        // Card 4: Interactive Time-Series Channels
        // Cards 5..(4 + flaws.size): Individual Flaw Flashcards
        // Final Card: Strengths & Mastery Highlights
        val totalCards = 6 + flaws.size
        val pagerState = rememberPagerState(
          initialPage = 0,
          pageCount = { totalCards }
        )

        val cardTitle = when (pagerState.currentPage) {
          0 -> "1. Fluency Matrix & Sub-Metrics"
          1 -> "2. Side-by-Side Waveforms & Spectrograms"
          2 -> "3. Phoneme-Level Error Heatmap"
          3 -> "4. Autonomous Adaptive Practice Drill"
          4 -> "5. Acoustic Time-Series Channels"
          in 5 until (5 + flaws.size) -> "Flaw #${pagerState.currentPage - 4}: ${flaws[pagerState.currentPage - 5].category.title}"
          else -> "Delivery Strengths & Mastery"
        }

        Column(modifier = Modifier.fillMaxWidth()) {
          // Flashcard Navigation Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "ANALYSIS FLASHCARDS",
                style = MaterialTheme.typography.labelLarge,
                color = SignalBaselineCyan,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = cardTitle,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
              )
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              IconButton(
                onClick = {
                  if (pagerState.currentPage > 0) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                  }
                },
                enabled = pagerState.currentPage > 0,
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Previous Flashcard",
                  tint = if (pagerState.currentPage > 0) SignalBaselineCyan else TextMuted,
                  modifier = Modifier.size(18.dp)
                )
              }

              Text(
                text = "${pagerState.currentPage + 1} / $totalCards",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold
              )

              IconButton(
                onClick = {
                  if (pagerState.currentPage < totalCards - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                  }
                },
                enabled = pagerState.currentPage < totalCards - 1,
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = "Next Flashcard",
                  tint = if (pagerState.currentPage < totalCards - 1) SignalBaselineCyan else TextMuted,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Dot Indicators
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            for (i in 0 until totalCards) {
              val active = i == pagerState.currentPage
              Box(
                modifier = Modifier
                  .padding(horizontal = 2.dp)
                  .height(5.dp)
                  .width(if (active) 18.dp else 5.dp)
                  .clip(CircleShape)
                  .background(if (active) SignalBaselineCyan else LabBorderSubtle)
                  .clickable {
                    scope.launch { pagerState.animateScrollToPage(i) }
                  }
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Horizontal Swipeable Flashcard Pager
          HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.Top,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("analysis_flashcards_pager")
          ) { page ->
            when (page) {
              0 -> {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  FluencyScoringMatrixCard(
                    matrix = evaluationResult.rubric.fluencyMatrix,
                    overallScore = evaluationResult.rubric.overallScore
                  )
                  RubricAndSpeakerNormalizationCard(
                    rubric = evaluationResult.rubric,
                    flawCount = flaws.size
                  )
                }
              }
              1 -> {
                SideBySideWaveformAndSpectrogramViewer(
                  eval = evaluationResult,
                  playbackMode = playbackMode,
                  playheadMs = playheadMs,
                  onScrubMs = onScrubPlayhead,
                  onTogglePlayBaseline = onTogglePlayBaseline,
                  onTogglePlayParticipant = onTogglePlayParticipant
                )
              }
              2 -> {
                PhonemeHeatmapRibbon(
                  alignedWords = evaluationResult.alignedWords,
                  groundedFlaws = evaluationResult.groundedFlaws,
                  selectedWordIndex = selectedWordIndex,
                  playheadMs = playheadMs,
                  onSelectWord = onSelectWord
                )
              }
              3 -> {
                AutonomousAdaptivePracticeCard(
                  drill = evaluationResult.adaptiveDrill,
                  isRecordingDrill = isRecordingDrill,
                  drillMicRms = drillMicRms,
                  drillElapsedMs = drillElapsedMs,
                  drillAttemptResult = drillAttemptResult,
                  onStartDrillRecording = onStartDrillRecording,
                  onStopDrillRecording = onStopDrillRecording,
                  onResetDrillAttempt = onResetDrillAttempt,
                  onRegenerateDrill = onRegenerateDrill
                )
              }
              4 -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    TelemetryChannel.entries.forEach { ch ->
                      val isSelected = ch == selectedChannel
                      Box(
                        modifier = Modifier
                          .clip(RoundedCornerShape(10.dp))
                          .background(if (isSelected) SignalBaselineCyan.copy(alpha = 0.12f) else LabSurfaceDark)
                          .border(
                            1.dp,
                            if (isSelected) SignalBaselineCyan else LabBorderSubtle,
                            RoundedCornerShape(10.dp)
                          )
                          .clickable { onSelectChannel(ch) }
                          .padding(horizontal = 12.dp, vertical = 7.dp)
                          .testTag("channel_tab_${ch.name}")
                      ) {
                        Text(
                          text = ch.label,
                          style = MaterialTheme.typography.labelMedium,
                          color = if (isSelected) SignalBaselineCyan else TextSecondary,
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                      }
                    }
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  ContrastiveTimeSeriesCanvas(
                    eval = evaluationResult,
                    channel = selectedChannel,
                    selectedFlawId = selectedFlawId,
                    playheadMs = playheadMs,
                    onScrubMs = onScrubPlayhead,
                    onSelectFlaw = onSelectFlaw
                  )
                }
              }
              in 5 until (5 + flaws.size) -> {
                val flawIdx = page - 5
                val flaw = flaws[flawIdx]
                SingleFlawFlashcard(
                  index = flawIdx + 1,
                  totalFlaws = flaws.size,
                  flaw = flaw,
                  onSelectFlaw = onSelectFlaw,
                  onPlayAbContrast = onPlayAbFlawContrast
                )
              }
              else -> {
                StrengthsMasteryFlashcard(
                  alignedWords = evaluationResult.alignedWords,
                  onSelectWord = onSelectWord
                )
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun SpeechAndSpectrumControlCard(
  speeches: List<BaselineSpeechSpec>,
  selectedSpeech: BaselineSpeechSpec,
  selectedTier: SpectrumTier,
  playbackMode: PlaybackMode,
  onSelectSpeech: (BaselineSpeechSpec) -> Unit,
  onSelectTier: (SpectrumTier) -> Unit,
  onTogglePlayBaseline: () -> Unit,
  onTogglePlayParticipant: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(LabSurfaceDark)
      .border(1.dp, LabBorderSubtle, RoundedCornerShape(14.dp))
      .padding(14.dp)
  ) {
    Text(
      text = "Speech & Take Selection",
      style = MaterialTheme.typography.titleMedium,
      color = TextPrimary,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))

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
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SignalBaselineCyan.copy(alpha = 0.12f) else LabSurfaceElevated)
            .border(
              width = 1.dp,
              color = if (selected) SignalBaselineCyan else LabBorderSubtle,
              shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSelectSpeech(speech) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("speech_chip_${speech.id}")
        ) {
          Column {
            Text(
              text = speech.speaker,
              style = MaterialTheme.typography.titleMedium,
              color = if (selected) SignalBaselineCyan else TextPrimary,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = speech.title,
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      SpectrumTier.entries.forEach { tier ->
        val isSelected = tier == selectedTier
        val tierColor = when (tier.level) {
          0 -> SignalPassEmerald
          1 -> SignalBaselineCyan
          2 -> SignalParticipantAmber
          3, 4 -> SignalFlawCrimson
          else -> SignalBaselineCyan
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) tierColor.copy(alpha = 0.14f) else LabBgObsidian)
            .border(
              width = 1.dp,
              color = if (isSelected) tierColor else LabBorderSubtle,
              shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelectTier(tier) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("tier_chip_${tier.level}")
        ) {
          Text(
            text = tier.shortLabel,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) tierColor else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      val playingBase = playbackMode == PlaybackMode.PLAYING_BASELINE
      val playingPart = playbackMode == PlaybackMode.PLAYING_PARTICIPANT

      Button(
        onClick = onTogglePlayBaseline,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (playingBase) SignalFlawCrimson else SignalBaselineCyan,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("play_baseline_button")
      ) {
        Icon(
          imageVector = if (playingBase) Icons.Default.Stop else Icons.Default.PlayArrow,
          contentDescription = "Play Reference",
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (playingBase) "Stop Reference" else "Play Reference",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold
        )
      }

      OutlinedButton(
        onClick = onTogglePlayParticipant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("play_participant_button")
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
          color = SignalParticipantAmber,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

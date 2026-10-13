package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AnalyticsWorkbenchScreen
import com.example.ui.screens.ChatAnalysisScreen
import com.example.ui.screens.DatasetAndStressScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveOratoryStudioScreen
import com.example.ui.screens.QuickOnboardingTutorialDialog
import com.example.ui.theme.LabBgObsidian
import com.example.ui.theme.LabBorderSubtle
import com.example.ui.theme.LabSurfaceDark
import com.example.ui.theme.LabSurfaceElevated
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SignalBaselineCyan
import com.example.ui.theme.SignalFlawCrimson
import com.example.ui.theme.SignalParticipantAmber
import com.example.ui.theme.SignalPassEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.OratorIqViewModel
import java.util.Locale

enum class MainNavTab(
  val title: String,
  val icon: ImageVector,
  val tag: String
) {
  HOME("Home", Icons.Default.Home, "nav_tab_home"),
  RECORD("Record", Icons.Default.Mic, "nav_tab_studio"),
  ANALYSIS("Analysis", Icons.Default.Analytics, "nav_tab_workbench"),
  CHAT("Chat", Icons.AutoMirrored.Filled.Chat, "nav_tab_chat"),
  LAB("Lab", Icons.Default.Science, "nav_tab_dataset")
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        OratorIqAppRoot()
      }
    }
  }
}

@Composable
fun OratorIqAppRoot(
  viewModel: OratorIqViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var currentTab by rememberSaveable { mutableStateOf(MainNavTab.HOME) }
  val context = LocalContext.current

  if (currentTab != MainNavTab.HOME) {
    BackHandler {
      currentTab = MainNavTab.HOME
    }
  }

  if (uiState.showTutorial) {
    QuickOnboardingTutorialDialog(
      onDismiss = { viewModel.dismissTutorial() },
      onStartRecording = {
        viewModel.dismissTutorial()
        currentTab = MainNavTab.RECORD
      },
      onStartAnalysis = {
        viewModel.dismissTutorial()
        currentTab = MainNavTab.ANALYSIS
      }
    )
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(LabBgObsidian)
      .windowInsetsPadding(WindowInsets.safeDrawing),
    containerColor = LabBgObsidian,
    topBar = {
      OratorIqMinimalTopBar(
        overallScore = uiState.evaluationResult?.rubric?.overallScore,
        onOpenTutorial = { viewModel.openTutorial() },
        onScoreClick = { currentTab = MainNavTab.ANALYSIS }
      )
    },
    bottomBar = {
      Column {
        HorizontalDivider(color = LabBorderSubtle, thickness = 1.dp)
        NavigationBar(
          containerColor = LabSurfaceDark,
          tonalElevation = 0.dp
        ) {
          MainNavTab.entries.forEach { tab ->
            val selected = tab == currentTab
            NavigationBarItem(
              selected = selected,
              onClick = { currentTab = tab },
              icon = {
                Icon(
                  imageVector = tab.icon,
                  contentDescription = tab.title
                )
              },
              label = {
                Text(
                  text = tab.title,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SignalBaselineCyan,
                selectedTextColor = SignalBaselineCyan,
                indicatorColor = SignalBaselineCyan.copy(alpha = 0.12f),
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
              ),
              modifier = Modifier.testTag(tab.tag)
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      AnimatedVisibility(visible = uiState.bannerToast != null) {
        uiState.bannerToast?.let { msg ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 6.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(LabSurfaceDark)
              .border(1.dp, SignalBaselineCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = msg,
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
              modifier = Modifier.weight(1f)
            )
            IconButton(
              onClick = { viewModel.dismissToast() },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss notification",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      when (currentTab) {
        MainNavTab.HOME -> {
          HomeScreen(
            speeches = uiState.speeches,
            selectedSpeech = uiState.selectedSpeech,
            selectedTier = uiState.selectedTier,
            evaluationResult = uiState.evaluationResult,
            playbackMode = uiState.playbackMode,
            onSelectSpeech = { viewModel.selectSpeech(it) },
            onSelectTier = { viewModel.selectTier(it) },
            onTogglePlayBaseline = { viewModel.togglePlayBaseline() },
            onTogglePlayParticipant = { viewModel.togglePlayParticipant() },
            onOpenTutorial = { viewModel.openTutorial() },
            onNavigateToRecord = { currentTab = MainNavTab.RECORD },
            onNavigateToAnalysis = { currentTab = MainNavTab.ANALYSIS },
            onNavigateToChat = { currentTab = MainNavTab.CHAT },
            onNavigateToDatasetLab = { currentTab = MainNavTab.LAB }
          )
        }

        MainNavTab.RECORD -> {
          LiveOratoryStudioScreen(
            speeches = uiState.speeches,
            selectedSpeech = uiState.selectedSpeech,
            isRecording = uiState.isRecordingMic,
            liveMicRms = uiState.liveMicRms,
            liveMicElapsedMs = uiState.liveMicElapsedMs,
            onSelectSpeech = { viewModel.selectSpeech(it) },
            onStartMicRecording = { viewModel.startLiveMicRecording() },
            onStopMicRecording = { viewModel.stopLiveMicRecording() },
            onImportWavUri = { uri ->
              viewModel.importExternalWavUri(context, uri) {
                currentTab = MainNavTab.ANALYSIS
              }
            },
            onNavigateToWorkbench = { currentTab = MainNavTab.ANALYSIS }
          )
        }

        MainNavTab.ANALYSIS -> {
          AnalyticsWorkbenchScreen(
            speeches = uiState.speeches,
            selectedSpeech = uiState.selectedSpeech,
            selectedTier = uiState.selectedTier,
            evaluationResult = uiState.evaluationResult,
            isEvaluating = uiState.isEvaluating,
            selectedChannel = uiState.selectedTelemetryChannel,
            selectedWordIndex = uiState.selectedWordIndex,
            selectedFlawId = uiState.selectedFlawId,
            playbackMode = uiState.playbackMode,
            playheadMs = uiState.playheadMs,
            isRecordingDrill = uiState.isRecordingDrill,
            drillMicRms = uiState.drillMicRms,
            drillElapsedMs = uiState.drillElapsedMs,
            drillAttemptResult = uiState.drillAttemptResult,
            onSelectSpeech = { viewModel.selectSpeech(it) },
            onSelectTier = { viewModel.selectTier(it) },
            onSelectChannel = { viewModel.selectTelemetryChannel(it) },
            onSelectWord = { viewModel.selectWordIndex(it) },
            onSelectFlaw = { viewModel.selectFlaw(it) },
            onScrubPlayhead = { viewModel.scrubPlayhead(it) },
            onTogglePlayBaseline = { viewModel.togglePlayBaseline() },
            onTogglePlayParticipant = { viewModel.togglePlayParticipant() },
            onPlayAbFlawContrast = { viewModel.playAbFlawComparison(it) },
            onStartDrillRecording = { viewModel.startDrillRecording(it) },
            onStopDrillRecording = { viewModel.stopDrillRecording() },
            onResetDrillAttempt = { viewModel.resetDrillAttempt() },
            onRegenerateDrill = { viewModel.regenerateAdaptiveDrill() }
          )
        }

        MainNavTab.CHAT -> {
          ChatAnalysisScreen(
            eval = uiState.evaluationResult,
            messages = uiState.chatMessages,
            isLoading = uiState.isChatLoading,
            onSendMessage = { viewModel.sendChatMessage(it) }
          )
        }

        MainNavTab.LAB -> {
          DatasetAndStressScreen(
            speeches = uiState.speeches,
            selectedSpeech = uiState.selectedSpeech,
            selectedTier = uiState.selectedTier,
            customConfig = uiState.customPerturbation,
            stressReport = uiState.stressReport,
            isRunningStress = uiState.isRunningStressSuite,
            onSelectSpeechAndTier = { s, t ->
              viewModel.selectSpeech(s)
              viewModel.selectTier(t)
            },
            onUpdateCustomConfig = { viewModel.updateCustomPerturbation(it) },
            onRunStressSuite = { viewModel.runFullStressSuite() },
            onNavigateToWorkbench = { currentTab = MainNavTab.ANALYSIS },
            onExportDatasetBundle = { viewModel.exportCurrentDatasetBundle(context) },
            onExportTechnicalPaper = { viewModel.exportTechnicalPaper(context) }
          )
        }
      }
    }
  }
}

@Composable
private fun OratorIqMinimalTopBar(
  overallScore: Float?,
  onOpenTutorial: () -> Unit,
  onScoreClick: () -> Unit
) {
  Column {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(LabSurfaceDark)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SignalBaselineCyan.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = "OratorIQ Logo",
            tint = SignalBaselineCyan,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "OratorIQ",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Speech & Delivery Coach",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (overallScore != null) {
          val yesPct = overallScore.coerceIn(75f, 99.5f)
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(LabSurfaceElevated)
              .clickable { onScoreClick() }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("top_bar_score_badge")
          ) {
            Text(
              text = String.format(Locale.US, "%.0f%% YES", yesPct),
              style = MaterialTheme.typography.labelMedium,
              color = SignalPassEmerald,
              fontFamily = MonoFontFamily,
              fontWeight = FontWeight.Bold
            )
          }
        }

        IconButton(
          onClick = onOpenTutorial,
          modifier = Modifier.size(34.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
            contentDescription = "Quick Tutorial",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
    HorizontalDivider(color = LabBorderSubtle, thickness = 1.dp)
  }
}

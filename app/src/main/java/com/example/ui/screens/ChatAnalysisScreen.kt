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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.ChatMessage
import com.example.model.ContrastiveEvaluationResult
import com.example.ui.theme.LabBgObsidian
import com.example.ui.theme.LabBorderSubtle
import com.example.ui.theme.LabSurfaceDark
import com.example.ui.theme.LabSurfaceElevated
import com.example.ui.theme.SignalBaselineCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ChatAnalysisScreen(
  eval: ContrastiveEvaluationResult?,
  messages: List<ChatMessage>,
  isLoading: Boolean,
  onSendMessage: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var inputText by rememberSaveable { mutableStateOf("") }

  val suggestedPrompts = listOf(
    "Summarize my delivery",
    "Tips to save my API quota",
    "How can I improve my pacing and pauses?",
    "Explain the math behind my score",
    "Give me a vocal coaching drill"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(4.dp))

    // Active Speech Context Pill
    if (eval != null) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(LabSurfaceDark)
          .border(1.dp, LabBorderSubtle, RoundedCornerShape(12.dp))
          .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Chat Context: ${eval.speechSpec.title}",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "${eval.tier.shortLabel} • ${eval.groundedFlaws.size} grounded flaw(s)",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
          )
        }
        Text(
          text = String.format(Locale.US, "%.0f%% YES", eval.rubric.overallScore.coerceIn(75f, 99.5f)),
          style = MaterialTheme.typography.labelLarge,
          color = SignalBaselineCyan,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Quick Question Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      suggestedPrompts.forEach { prompt ->
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(LabSurfaceDark)
            .border(1.dp, LabBorderSubtle, RoundedCornerShape(20.dp))
            .clickable { onSendMessage(prompt) }
            .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
          Text(
            text = prompt,
            style = MaterialTheme.typography.labelMedium,
            color = SignalBaselineCyan
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Chat Messages List
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(messages, key = { it.id }) { msg ->
        ChatBubble(message = msg)
      }

      if (isLoading) {
        item {
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(LabSurfaceDark)
              .border(1.dp, LabBorderSubtle, RoundedCornerShape(12.dp))
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(
              color = SignalBaselineCyan,
              strokeWidth = 2.dp,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Analyzing speech telemetry...",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Minimalist Chat Input Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = inputText,
        onValueChange = { inputText = it },
        placeholder = {
          Text(
            text = "Ask about your speech delivery, timestamps, or score...",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
          )
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .background(LabSurfaceDark, RoundedCornerShape(12.dp))
          .testTag("chat_input_field"),
        singleLine = true
      )

      Spacer(modifier = Modifier.width(8.dp))

      IconButton(
        onClick = {
          if (inputText.isNotBlank()) {
            onSendMessage(inputText)
            inputText = ""
          }
        },
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(SignalBaselineCyan)
          .testTag("chat_send_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Send question",
          tint = Color.White
        )
      }
    }
  }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
  val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
  val bgColor = if (message.isUser) SignalBaselineCyan else LabSurfaceDark
  val textColor = if (message.isUser) Color.White else TextPrimary

  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = alignment
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth(0.90f)
        .clip(RoundedCornerShape(14.dp))
        .background(bgColor)
        .border(
          width = if (message.isUser) 0.dp else 1.dp,
          color = if (message.isUser) Color.Transparent else LabBorderSubtle,
          shape = RoundedCornerShape(14.dp)
        )
        .padding(14.dp)
    ) {
      Text(
        text = if (message.isUser) "You" else "Speech Analyst",
        style = MaterialTheme.typography.labelSmall,
        color = if (message.isUser) Color.White.copy(alpha = 0.8f) else SignalBaselineCyan,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = message.text,
        style = MaterialTheme.typography.bodyLarge,
        color = textColor
      )
    }
  }
}

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DeepOlive
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.EmptyStateView
import com.example.ui.theme.GuardianCard
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StatusPill
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TimelineStepRow
import com.example.ui.viewmodel.GuardianMainUiState
import com.example.ui.viewmodel.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    state: GuardianMainUiState,
    onSyncNow: () -> Unit,
    onOpenAssistant: () -> Unit,
) {
    val bn = state.prefs.language == "bn"
    val fmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = if (bn) "কার্যক্রম ও সিঙ্ক" else "Activity & Synchronization",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
            Text(
                text = if (bn) "অফলাইন আউটবক্স কিউ ও ইভেন্ট ইতিহাস" else "Offline outbox queue, timeline & audit events",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        // Sync Outbox Status Card
        item {
            GuardianCard(highlighted = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (bn) "অফলাইন আউটবক্স সিঙ্ক" else "Outbox Sync Queue",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (state.pendingSyncCount > 0) "${state.pendingSyncCount} item(s) waiting safely in Room"
                            else "All local records synchronized",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.pendingSyncCount > 0) AmberWarning else EmeraldSafe,
                        )
                    }
                    Button(
                        onClick = onSyncNow,
                        modifier = Modifier.testTag("sync_now_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (bn) "সিঙ্ক" else "Sync Now")
                    }
                }
            }
        }

        item {
            GuardianCard(onClick = onOpenAssistant) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = LimeAccent)
                    Text(
                        text = if (bn) "Guardian AI সহকারী খুলুন" else "Open Guardian AI Security Assistant",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                }
            }
        }

        if (state.recentEvents.isEmpty() && state.shareLogs.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Filled.History,
                    title = if (bn) "এখনও কোনো কার্যক্রম নেই" else "No Activity Logged Yet",
                    description = if (bn) "আপনি ঘটনা যোগ করলে বা চেকলিস্ট আপডেট করলে এখানে টাইমলাইন দেখাবে।"
                    else "Timeline events, checklist updates, and consent-based shares appear here.",
                )
            }
        } else {
            item {
                GuardianCard {
                    Text(if (bn) "সাম্প্রতিক টাইমলাইন" else "Chronological Event Log", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(10.dp))
                    state.recentEvents.forEachIndexed { idx, ev ->
                        TimelineStepRow(
                            title = ev.eventType.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            subtitle = ev.note,
                            timestamp = fmt.format(Date(ev.eventTimestampMillis)),
                            isLast = idx == state.recentEvents.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantScreen(
    state: GuardianMainUiState,
    isBusy: Boolean,
    viewModel: GuardianViewModel,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val bn = state.prefs.language == "bn"
    var prompt by remember { mutableStateOf("") }
    var lastUserPrompt by remember { mutableStateOf("") }

    val suggested = remember(bn) {
        if (bn) {
            listOf(
                "ফোন চুরি হলে প্রথমে কী করব?",
                "সিম কীভাবে ব্লক করব?",
                "অনলাইন জিডি করতে কী কী তথ্য লাগে?",
                "আমার সক্রিয় ঘটনার সারাংশ দিন",
            )
        } else {
            listOf(
                "What should I do immediately after my phone is stolen?",
                "How can I block my SIM in Bangladesh?",
                "What information do I need for an Online GD?",
                "Summarise my active incident timeline",
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column {
                    Text("Guardian AI Assistant", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("RAG-grounded • Bangla & English", style = MaterialTheme.typography.labelSmall, color = LimeAccent)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = !bn,
                    onClick = { viewModel.toggleLanguage("en") },
                    label = { Text("EN") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LimeAccent, selectedLabelColor = ObsidianBlack),
                )
                FilterChip(
                    selected = bn,
                    onClick = { viewModel.toggleLanguage("bn") },
                    label = { Text("বাং") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LimeAccent, selectedLabelColor = ObsidianBlack),
                )
                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear chat", tint = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggested) { q ->
                SuggestionChip(
                    onClick = {
                        lastUserPrompt = q
                        viewModel.askAssistant(q)
                    },
                    label = { Text(q, maxLines = 1) },
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.chatMessages.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Filled.AutoAwesome,
                        title = if (bn) "যাচাইকৃত নিরাপত্তা সহায়তা" else "Verified Security Guidance",
                        description = if (bn) "উপরের প্রস্তাবিত প্রশ্ন বাছুন অথবা বাংলা/ইংরেজিতে প্রশ্ন করুন।"
                        else "Ask about Online GD, SIM blocking, bKash/Nagad freeze, or your incident status.",
                    )
                }
            } else {
                items(state.chatMessages, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isUser) DeepOlive else CardSurface)
                            .padding(14.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StatusPill(
                                    text = if (isUser) "You" else msg.providerUsed,
                                    color = if (isUser) LimeAccent else TextSecondary,
                                )
                                if (!isUser) {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("Guardian AI", msg.text))
                                                viewModel.showMessage("Response copied.")
                                            },
                                            modifier = Modifier.size(28.dp),
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                        if (lastUserPrompt.isNotBlank()) {
                                            IconButton(
                                                onClick = { viewModel.askAssistant(lastUserPrompt) },
                                                modifier = Modifier.size(28.dp),
                                            ) {
                                                Icon(Icons.Filled.Refresh, contentDescription = "Retry", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            Text(msg.text, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            if (msg.citationsSummary.isNotBlank()) {
                                Text(
                                    text = "Sources:\n${msg.citationsSummary}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LimeAccent,
                                )
                            }
                            if (!msg.uncertaintyNotice.isNullOrBlank()) {
                                Text(msg.uncertaintyNotice, style = MaterialTheme.typography.labelSmall, color = AmberWarning)
                            }
                        }
                    }
                }
            }
            if (isBusy) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = LimeAccent, strokeWidth = 2.dp)
                        Text("Retrieving verified guidance...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = { Text(if (bn) "বাংলা বা ইংরেজিতে প্রশ্ন লিখুন..." else "Ask in English or Bangla...") },
                modifier = Modifier.weight(1f).testTag("assistant_prompt_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LimeAccent,
                    unfocusedBorderColor = SubtleBorder,
                    focusedContainerColor = CharcoalSurface,
                    unfocusedContainerColor = CharcoalSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
            )
            IconButton(
                onClick = {
                    if (prompt.isNotBlank()) {
                        lastUserPrompt = prompt
                        viewModel.askAssistant(prompt)
                        prompt = ""
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(LimeAccent)
                    .testTag("assistant_send_button"),
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = ObsidianBlack)
            }
        }
    }
}

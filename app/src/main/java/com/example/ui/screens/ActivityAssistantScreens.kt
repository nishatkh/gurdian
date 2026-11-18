package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
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
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ButterYellow
import com.example.ui.theme.ButterYellowSoft
import com.example.ui.theme.CreamFieldSurface
import com.example.ui.theme.EmptyStateView
import com.example.ui.theme.GuardianCard
import com.example.ui.theme.OnCharcoal
import com.example.ui.theme.OrganicEditorialCard
import com.example.ui.theme.PastelPink
import com.example.ui.theme.PowderBlue
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SageGreenSoft
import com.example.ui.theme.SoftCharcoal
import com.example.ui.theme.SoftCreamSurface
import com.example.ui.theme.SoftOutline
import com.example.ui.theme.StatusPill
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TimelineStepRow
import com.example.ui.theme.WarmCoral
import com.example.ui.theme.WarmIvory
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
            .background(WarmIvory),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text(
                text = if (bn) "কার্যক্রম ও সিঙ্ক" else "Activity & Sync",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (bn) "অফলাইন আউটবক্স কিউ ও ইভেন্ট ইতিহাস" else "Offline outbox queue, timeline & audit events",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        // Sync Outbox Status Card with Bold Numerical Indicator
        item {
            OrganicEditorialCard(
                containerColor = SageGreenSoft,
                blobColor = SageGreen,
                accentShapeColor = ButterYellow,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "${state.pendingSyncCount}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (bn) "অফলাইন আউটবক্স কিউ" else "Outbox Items Queued",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (state.pendingSyncCount > 0) "Waiting safely in encrypted local storage"
                            else "All local records synchronized",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                    Button(
                        onClick = onSyncNow,
                        modifier = Modifier.height(48.dp).testTag("sync_now_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
                    ) {
                        Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (bn) "সিঙ্ক" else "Sync Now")
                    }
                }
            }
        }

        item {
            OrganicEditorialCard(
                containerColor = ButterYellowSoft,
                blobColor = ButterYellow,
                accentShapeColor = PastelPink,
                onClick = onOpenAssistant,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = SoftCharcoal,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = ButterYellow, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(
                            text = if (bn) "Guardian AI সহকারী খুলুন" else "Open Guardian AI Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (state.recentEvents.isEmpty() && state.shareLogs.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.History,
                    title = if (bn) "এখনও কোনো কার্যক্রম নেই" else "No Activity Logged Yet",
                    description = if (bn) "আপনি ঘটনা যোগ করলে বা চেকলিস্ট আপডেট করলে এখানে টাইমলাইন দেখাবে।"
                    else "Timeline events, checklist updates, and consent-based shares appear here.",
                )
            }
        } else {
            item {
                GuardianCard(containerColor = SoftCreamSurface) {
                    Text(if (bn) "সাম্প্রতিক টাইমলাইন" else "Chronological Event Log", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(14.dp))
                    state.recentEvents.forEachIndexed { idx, ev ->
                        TimelineStepRow(
                            title = ev.eventType.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            subtitle = ev.note,
                            timestamp = fmt.format(Date(ev.eventTimestampMillis)),
                            isLast = idx == state.recentEvents.lastIndex,
                            accentColor = if (idx == 0) ButterYellow else PowderBlue,
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
            .background(WarmIvory)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = SoftCreamSurface,
            border = BorderStroke(1.dp, SoftOutline),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = SoftCharcoal)
                    }
                    Column {
                        Text("Guardian AI", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("RAG-grounded • Bangla & English", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = !bn,
                        onClick = { viewModel.toggleLanguage("en") },
                        label = { Text("EN") },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftCharcoal,
                            selectedLabelColor = OnCharcoal,
                            containerColor = CreamFieldSurface,
                            labelColor = TextSecondary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = !bn,
                            borderColor = SoftOutline,
                            selectedBorderColor = SoftCharcoal,
                        ),
                    )
                    FilterChip(
                        selected = bn,
                        onClick = { viewModel.toggleLanguage("bn") },
                        label = { Text("বাং") },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftCharcoal,
                            selectedLabelColor = OnCharcoal,
                            containerColor = CreamFieldSurface,
                            labelColor = TextSecondary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = bn,
                            borderColor = SoftOutline,
                            selectedBorderColor = SoftCharcoal,
                        ),
                    )
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear chat", tint = TextSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggested) { q ->
                SuggestionChip(
                    onClick = {
                        lastUserPrompt = q
                        viewModel.askAssistant(q)
                    },
                    label = { Text(q, maxLines = 1, color = TextPrimary) },
                    shape = RoundedCornerShape(50),
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = ButterYellowSoft),
                    border = BorderStroke(1.dp, SoftOutline),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            if (state.chatMessages.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.AutoAwesome,
                        title = if (bn) "যাচাইকৃত নিরাপত্তা সহায়তা" else "Verified Security Guidance",
                        description = if (bn) "উপরের প্রস্তাবিত প্রশ্ন বাছুন অথবা বাংলা/ইংরেজিতে প্রশ্ন করুন।"
                        else "Ask about Online GD, SIM blocking, bKash/Nagad freeze, or your incident status.",
                    )
                }
            } else {
                items(state.chatMessages, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    ) {
                        Surface(
                            modifier = Modifier.widthIn(max = 340.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = if (isUser) SoftCharcoal else SoftCreamSurface,
                            border = if (isUser) null else BorderStroke(1.dp, SoftOutline),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (isUser) {
                                        Text(
                                            text = "You",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ButterYellow,
                                        )
                                    } else {
                                        StatusPill(
                                            text = msg.providerUsed,
                                            color = SoftCharcoal,
                                            backgroundColor = ButterYellowSoft,
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    cm.setPrimaryClip(ClipData.newPlainText("Guardian AI", msg.text))
                                                    viewModel.showMessage("Response copied.")
                                                },
                                                modifier = Modifier.size(28.dp),
                                            ) {
                                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                            }
                                            if (lastUserPrompt.isNotBlank()) {
                                                IconButton(
                                                    onClick = { viewModel.askAssistant(lastUserPrompt) },
                                                    modifier = Modifier.size(28.dp),
                                                ) {
                                                    Icon(Icons.Outlined.Refresh, contentDescription = "Retry", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isUser) OnCharcoal else TextPrimary,
                                )
                                if (msg.citationsSummary.isNotBlank()) {
                                    Text(
                                        text = "Sources:\n${msg.citationsSummary}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUser) ButterYellowSoft else TextSecondary,
                                    )
                                }
                                if (!msg.uncertaintyNotice.isNullOrBlank()) {
                                    Text(
                                        text = msg.uncertaintyNotice,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUser) PastelPink else WarmCoral,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (isBusy) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SoftCharcoal, strokeWidth = 2.dp)
                        Text("Retrieving verified guidance...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = { Text(if (bn) "বাংলা বা ইংরেজিতে প্রশ্ন লিখুন..." else "Ask in English or Bangla...", color = TextMuted) },
                modifier = Modifier.weight(1f).testTag("assistant_prompt_input"),
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SoftCharcoal,
                    unfocusedBorderColor = SoftOutline,
                    focusedContainerColor = SoftCreamSurface,
                    unfocusedContainerColor = CreamFieldSurface,
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
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SoftCharcoal)
                    .testTag("assistant_send_button"),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send", tint = ButterYellow)
            }
        }
    }
}

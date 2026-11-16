package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneLocked
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.BilingualStrings
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.EmptyStateView
import com.example.ui.theme.GuardianCard
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.LimeGlowTint
import com.example.ui.theme.LimeGradientCard
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StatusPill
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TimelineStepRow
import com.example.ui.viewmodel.GuardianMainUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    state: GuardianMainUiState,
    onOpenEmergency: () -> Unit,
    onOpenIncident: (String) -> Unit,
    onOpenChecklist: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenResources: () -> Unit,
    onOpenAssistant: () -> Unit,
    onSyncNow: () -> Unit,
) {
    val context = LocalContext.current
    val bn = state.prefs.language == "bn"
    val totalChecklist = state.checklist.size.coerceAtLeast(1)
    val doneChecklist = state.completedChecklistCount
    val progress = doneChecklist.toFloat() / totalChecklist.toFloat()
    val hasVaultImei = state.prefs.deviceImeiLast4.isNotBlank()
    val timeFmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = BilingualStrings.greeting(state.prefs.fullName, bn),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = BilingualStrings.subtitleHome(bn),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                StatusPill(
                    text = if (state.pendingSyncCount > 0) "${state.pendingSyncCount} Pending" else "Synced",
                    color = if (state.pendingSyncCount > 0) AmberWarning else LimeAccent,
                    modifier = Modifier.clickable { onSyncNow() },
                )
            }
        }

        // Security Status & Device Vault Summary
        item {
            GuardianCard(highlighted = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(LimeGlowTint),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = LimeAccent)
                        }
                        Column {
                            Text(
                                text = if (bn) "ডিভাইস সুরক্ষা অবস্থা" else "Device Security Readiness",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Text(
                                text = if (hasVaultImei) {
                                    "${state.prefs.deviceBrand} ${state.prefs.deviceModel} • IMEI ••••${state.prefs.deviceImeiLast4}"
                                } else {
                                    if (bn) "প্রোফাইলে আপনার IMEI ও ডিভাইস সংরক্ষণ করুন"
                                    else "Save your device model & IMEI in Profile Vault"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasVaultImei) LimeAccent else AmberWarning,
                            )
                        }
                    }
                }
            }
        }

        // Emergency Theft Mode Hero Card
        item {
            LimeGradientCard(
                modifier = Modifier.testTag("home_emergency_card"),
                onClick = onOpenEmergency,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(
                        text = if (bn) "তাৎক্ষণিক পদক্ষেপ" else "RAPID RESPONSE",
                        color = CrimsonAlert,
                    )
                    Icon(Icons.Filled.GppMaybe, contentDescription = null, tint = LimeAccent)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = BilingualStrings.emergencyTitle(bn),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = BilingualStrings.emergencyDesc(bn),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onOpenEmergency,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("home_activate_emergency_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                ) {
                    Icon(Icons.Filled.PhoneLocked, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (bn) "ইমার্জেন্সি / চুরি মোড চালু করুন" else "Open Emergency Workflow",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        // Active / Recent Incident Overview
        item {
            val active = state.activeIncident
            if (active != null) {
                GuardianCard(onClick = { onOpenIncident(active.id) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (bn) "সক্রিয় ঘটনা" else "Recent Incident",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                        )
                        StatusPill(text = BilingualStrings.statusLabel(active.status, bn))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(active.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${BilingualStrings.categoryLabel(active.category, bn)} • ${active.locationLabel.ifBlank { "Location not set" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            } else {
                EmptyStateView(
                    icon = Icons.Filled.CheckCircle,
                    title = if (bn) "কোনো সক্রিয় ঘটনা নেই" else "No Active Incidents",
                    description = if (bn) "আপনার সব ডিভাইস সুরক্ষিত। জরুরি প্রয়োজনে Emergency ট্যাব ব্যবহার করুন।"
                    else "Everything looks calm. Use Emergency Mode anytime to log a lost or stolen phone.",
                )
            }
        }

        // Account Protection Checklist Progress
        item {
            GuardianCard(onClick = onOpenChecklist, modifier = Modifier.testTag("home_checklist_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (bn) "অ্যাকাউন্ট সুরক্ষা চেকলিস্ট" else "Account Protection Checklist",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (bn) "$doneChecklist / $totalChecklist ধাপ সম্পন্ন" else "$doneChecklist of $totalChecklist steps completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LimeAccent)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = LimeAccent,
                    trackColor = ElevatedSurface,
                )
            }
        }

        // AI Security Assistant Banner
        item {
            GuardianCard(
                highlighted = true,
                onClick = onOpenAssistant,
                modifier = Modifier.testTag("home_ai_assistant_card"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LimeAccent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ObsidianBlack)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (bn) "Guardian AI সহকারী (বাংলা ও English)" else "Ask Guardian AI Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (bn) "Online GD, সিম ব্লক, বিকাশ/নগদ ও টাইমলাইন সহায়তা"
                            else "Grounded answers with verified Bangladesh citations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LimeAccent)
                }
            }
        }

        // Official Protection Quick Access
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (bn) "অফিসিয়াল সুরক্ষা পোর্টাল" else "Official Protection Links",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = if (bn) "সব দেখুন" else "Directory",
                        style = MaterialTheme.typography.labelLarge,
                        color = LimeAccent,
                        modifier = Modifier.clickable { onOpenResources() },
                    )
                }
                val quickLinks = listOf(
                    Triple("Android Find Hub", "https://www.google.com/android/find/", "Google Lock"),
                    Triple("Online GD", "https://gd.police.gov.bd/", "BD Police"),
                    Triple("BTRC / NEIR", "https://www.btrc.gov.bd/", "IMEI Registry"),
                    Triple("Apple Find My", "https://www.icloud.com/find", "iOS Lost Mode"),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(quickLinks) { (label, url, sub) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardSurface)
                                .clickable {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column {
                                    Text(label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(sub, style = MaterialTheme.typography.labelSmall, color = LimeAccent)
                                }
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Trusted Contacts Summary + Recent Timeline
        item {
            GuardianCard(onClick = onOpenContacts) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Contacts, contentDescription = null, tint = LimeAccent)
                        Column {
                            Text(
                                text = if (bn) "বিশ্বস্ত পরিচিতি" else "Trusted Contacts",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            Text(
                                text = if (bn) "${state.contacts.size} জন সংরক্ষিত (সম্মতি-ভিত্তিক)"
                                else "${state.contacts.size} saved • consent-gated sharing",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary)
                }
            }
        }

        if (state.recentEvents.isNotEmpty()) {
            item {
                GuardianCard {
                    Text(
                        text = if (bn) "সাম্প্রতিক কার্যক্রম" else "Recent Activity Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(12.dp))
                    val topEvents = state.recentEvents.take(3)
                    topEvents.forEachIndexed { index, ev ->
                        TimelineStepRow(
                            title = ev.eventType.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            subtitle = ev.note,
                            timestamp = timeFmt.format(Date(ev.eventTimestampMillis)),
                            isLast = index == topEvents.lastIndex,
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = BilingualStrings.disclaimer(bn),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
}

package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.PhoneLocked
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.BilingualStrings
import com.example.ui.theme.ButterYellow
import com.example.ui.theme.ButterYellowSoft
import com.example.ui.theme.EmptyStateView
import com.example.ui.theme.GuardianCard
import com.example.ui.theme.OnCharcoal
import com.example.ui.theme.OrganicEditorialCard
import com.example.ui.theme.PastelPink
import com.example.ui.theme.PastelPinkSoft
import com.example.ui.theme.PowderBlue
import com.example.ui.theme.PowderBlueSoft
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SageGreenSoft
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.SoftCharcoal
import com.example.ui.theme.SoftCircularProgressRing
import com.example.ui.theme.SoftCreamSurface
import com.example.ui.theme.SoftOutline
import com.example.ui.theme.StatusPill
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TimelineStepRow
import com.example.ui.theme.WarmCoral
import com.example.ui.theme.WarmCoralSoft
import com.example.ui.theme.WarmIvory
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
    val readinessPercent = (progress * 100).toInt()
    val hasVaultImei = state.prefs.deviceImeiLast4.isNotBlank()
    val timeFmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmIvory),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Editorial Greeting Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = if (bn) "স্বাগতম • Guardian AI" else "PERSONAL SAFETY WELLNESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = BilingualStrings.greeting(state.prefs.fullName, bn),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = BilingualStrings.subtitleHome(bn),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                StatusPill(
                    text = if (state.pendingSyncCount > 0) "${state.pendingSyncCount} Pending" else "Synced",
                    color = SoftCharcoal,
                    backgroundColor = if (state.pendingSyncCount > 0) ButterYellow else SageGreenSoft,
                    modifier = Modifier.clickable { onSyncNow() },
                )
            }
        }

        // Hero Organic Wellness Card with Large Circular Progress & Bold Numerical Readiness
        item {
            OrganicEditorialCard(
                containerColor = ButterYellowSoft,
                blobColor = ButterYellow,
                accentShapeColor = PastelPink,
                onClick = onOpenChecklist,
                modifier = Modifier.testTag("home_checklist_card"),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    SoftCircularProgressRing(
                        progress = progress,
                        valueText = "$readinessPercent%",
                        captionText = "$doneChecklist/$totalChecklist",
                        size = 98.dp,
                        progressColor = SoftCharcoal,
                        trackColor = SoftCreamSurface,
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(
                            text = if (bn) "সুরক্ষা প্রস্তুতি" else "Protection Score",
                            color = SoftCharcoal,
                            backgroundColor = SoftCreamSurface,
                        )
                        Text(
                            text = if (bn) "ডিভাইস ও অ্যাকাউন্ট" else "Device & Account",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (hasVaultImei) {
                                "${state.prefs.deviceBrand} ${state.prefs.deviceModel} • ••••${state.prefs.deviceImeiLast4}"
                            } else {
                                if (bn) "চেকলিস্ট ও IMEI ভল্ট আপডেট করুন"
                                else "Tap to complete checklist & save IMEI"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        // Emergency Rapid Response Card — Pastel Pink with Dotted Accent & Dark Charcoal Pill Button
        item {
            OrganicEditorialCard(
                modifier = Modifier.testTag("home_emergency_card"),
                containerColor = PastelPinkSoft,
                blobColor = PastelPink,
                accentShapeColor = ButterYellow,
                dottedBorder = true,
                onClick = onOpenEmergency,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(
                        text = if (bn) "তাৎক্ষণিক পদক্ষেপ" else "RAPID RESPONSE",
                        color = WarmCoral,
                        backgroundColor = WarmCoralSoft,
                    )
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = SoftCreamSurface,
                        border = BorderStroke(1.dp, SoftOutline),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.GppMaybe, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = BilingualStrings.emergencyTitle(bn),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = BilingualStrings.emergencyDesc(bn),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onOpenEmergency,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("home_activate_emergency_btn"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
                ) {
                    Icon(Icons.Outlined.PhoneLocked, contentDescription = null, modifier = Modifier.size(18.dp))
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
                OrganicEditorialCard(
                    containerColor = PowderBlueSoft,
                    blobColor = PowderBlue,
                    accentShapeColor = SageGreen,
                    onClick = { onOpenIncident(active.id) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (bn) "সক্রিয় ঘটনা" else "RECENT INCIDENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        StatusPill(
                            text = BilingualStrings.statusLabel(active.status, bn),
                            color = SoftCharcoal,
                            backgroundColor = SoftCreamSurface,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
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
                    icon = Icons.Outlined.CheckCircle,
                    title = if (bn) "কোনো সক্রিয় ঘটনা নেই" else "Everything Looks Calm",
                    description = if (bn) "আপনার সব ডিভাইস সুরক্ষিত। জরুরি প্রয়োজনে নিচের + বাটন ব্যবহার করুন।"
                    else "No active incidents logged. Tap the center Emergency button anytime to start a guided report.",
                )
            }
        }

        // AI Security Assistant Card — Sage Green Organic Card
        item {
            OrganicEditorialCard(
                containerColor = SageGreenSoft,
                blobColor = SageGreen,
                accentShapeColor = PowderBlue,
                onClick = onOpenAssistant,
                modifier = Modifier.testTag("home_ai_assistant_card"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = SoftCharcoal,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = ButterYellow, modifier = Modifier.size(22.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (bn) "Guardian AI সহকারী (বাংলা ও English)" else "Ask Guardian AI Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (bn) "Online GD, সিম ব্লক, বিকাশ/নগদ ও টাইমলাইন সহায়তা"
                            else "Grounded answers with verified Bangladesh citations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = SoftCreamSurface,
                        border = BorderStroke(1.dp, SoftOutline),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Official Protection Quick Access
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(
                    title = if (bn) "অফিসিয়াল সুরক্ষা পোর্টাল" else "Official Portals",
                    actionLabel = if (bn) "সব দেখুন" else "Directory",
                    onActionClick = onOpenResources,
                )
                val quickLinks = listOf(
                    Triple("Android Find Hub", "https://www.google.com/android/find/", "Google Lock"),
                    Triple("Online GD", "https://gd.police.gov.bd/", "BD Police"),
                    Triple("BTRC / NEIR", "https://www.btrc.gov.bd/", "IMEI Registry"),
                    Triple("Apple Find My", "https://www.icloud.com/find", "iOS Lost Mode"),
                )
                val chipTints = listOf(ButterYellowSoft, PowderBlueSoft, SageGreenSoft, PastelPinkSoft)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(quickLinks) { idx, (label, url, sub) ->
                        Surface(
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = chipTints[idx % chipTints.size],
                            border = BorderStroke(1.dp, SoftOutline),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column {
                                    Text(label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(sub, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Trusted Contacts Summary Card with Large Bold Count
        item {
            GuardianCard(onClick = onOpenContacts, containerColor = SoftCreamSurface) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            color = PowderBlueSoft,
                            border = BorderStroke(1.dp, SoftOutline),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Contacts, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text(
                                text = if (bn) "বিশ্বস্ত পরিচিতি" else "Trusted Contacts",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Text(
                                text = if (bn) "জরুরি মুহূর্তে সম্মতি-ভিত্তিক শেয়ার"
                                else "Consent-gated alert sharing",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${state.contacts.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (state.recentEvents.isNotEmpty()) {
            item {
                GuardianCard(containerColor = SoftCreamSurface) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Shield, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (bn) "সাম্প্রতিক কার্যক্রম" else "Recent Activity Timeline",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    val topEvents = state.recentEvents.take(3)
                    topEvents.forEachIndexed { index, ev ->
                        TimelineStepRow(
                            title = ev.eventType.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            subtitle = ev.note,
                            timestamp = timeFmt.format(Date(ev.eventTimestampMillis)),
                            isLast = index == topEvents.lastIndex,
                            accentColor = if (index == 0) ButterYellow else SageGreen,
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

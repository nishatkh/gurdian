package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.IncidentEntity
import com.example.domain.BilingualStrings
import com.example.security.KeystoreCryptoManager
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElevatedSurface
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
fun IncidentsScreen(
    state: GuardianMainUiState,
    onCreateIncident: () -> Unit,
    onSelectIncident: (String) -> Unit,
) {
    val bn = state.prefs.language == "bn"
    var filterStatus by remember { mutableStateOf("all") }
    val filtered = remember(state.incidents, filterStatus) {
        if (filterStatus == "all") state.incidents
        else state.incidents.filter { it.status == filterStatus }
    }
    val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = if (bn) "ঘটনার তালিকা" else "Incident Records",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                )
                Text(
                    text = if (bn) "অফলাইন ও অনলাইন ঘটনা, টাইমলাইন এবং GD খসড়া" else "Offline-first incident logs, timelines & GD drafts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

            item {
                val statuses = listOf("all", "active", "draft", "submitted_for_review", "closed", "cancelled")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statuses) { st ->
                        FilterChip(
                            selected = filterStatus == st,
                            onClick = { filterStatus = st },
                            label = {
                                Text(if (st == "all") (if (bn) "সব" else "All") else BilingualStrings.statusLabel(st, bn))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = ObsidianBlack,
                            ),
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Filled.Report,
                        title = if (bn) "কোনো ঘটনা পাওয়া যায়নি" else "No Incidents Recorded",
                        description = if (bn) "নতুন ঘটনা যোগ করতে নিচের + বাটনে চাপুন।"
                        else "Tap + below or activate Emergency Mode to record a lost or stolen phone incident.",
                    )
                }
            } else {
                items(filtered, key = { it.id }) { inc ->
                    GuardianCard(
                        highlighted = inc.status == "active",
                        onClick = { onSelectIncident(inc.id) },
                        modifier = Modifier.testTag("incident_card_${inc.id}"),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StatusPill(
                                text = BilingualStrings.statusLabel(inc.status, bn),
                                color = when (inc.status) {
                                    "active" -> CrimsonAlert
                                    "closed", "approved" -> EmeraldSafe
                                    "cancelled" -> TextMuted
                                    else -> LimeAccent
                                },
                            )
                            StatusPill(
                                text = if (inc.syncStatus == "synced") "Synced" else "Offline Saved",
                                color = if (inc.syncStatus == "synced") EmeraldSafe else AmberWarning,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(inc.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${BilingualStrings.categoryLabel(inc.category, bn)} • ${inc.deviceBrand} ${inc.deviceModel}".trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = dateFmt.format(Date(inc.occurredAtMillis)),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            onClick = onCreateIncident,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("new_incident_fab"),
            containerColor = LimeAccent,
            contentColor = ObsidianBlack,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create new incident")
        }
    }
}

@Composable
fun IncidentDetailScreen(
    incidentId: String,
    viewModel: GuardianViewModel,
    bn: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val incident by viewModel.observeIncident(incidentId).collectAsStateWithLifecycle(initialValue = null)
    val events by viewModel.observeEvents(incidentId).collectAsStateWithLifecycle(initialValue = emptyList())
    val evidence by viewModel.observeEvidence(incidentId).collectAsStateWithLifecycle(initialValue = emptyList())

    var revealImei by remember { mutableStateOf(false) }
    var newNote by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    var showReportModal by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.attachEvidence(incidentId, uri.toString(), "Evidence Photo (${uri.lastPathSegment ?: "image"})")
            }
        },
    )

    val current: IncidentEntity? = incident
    if (current == null) {
        Box(modifier = Modifier.fillMaxSize().background(ObsidianBlack), contentAlignment = Alignment.Center) {
            Text("Incident not found", color = TextSecondary)
        }
        return
    }

    val decryptedImei = remember(current.imeiEncrypted) { viewModel.decryptImei(current.imeiEncrypted) }
    val assessment = remember(current, events.size) { viewModel.computeAssessment(current, events.size) }
    val timeFmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = LimeAccent,
        unfocusedBorderColor = SubtleBorder,
        focusedContainerColor = CharcoalSurface,
        unfocusedContainerColor = CharcoalSurface,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("incident_detail_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(current.title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Text("ID: ${current.id.take(8).uppercase()}", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
                StatusPill(text = BilingualStrings.statusLabel(current.status, bn))
            }
        }

        // Core Device & Masked IMEI Card
        item {
            GuardianCard(highlighted = true) {
                Text(if (bn) "ডিভাইস ও ঘটনার তথ্য" else "Device & Incident Summary", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("${BilingualStrings.categoryLabel(current.category, bn)} • ${current.locationLabel.ifBlank { "Location not recorded" }}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("${current.deviceBrand} ${current.deviceModel}".trim().ifBlank { "Device details not set" }, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (revealImei && decryptedImei.isNotBlank()) "IMEI: $decryptedImei"
                        else "IMEI: ${KeystoreCryptoManager.maskImei(decryptedImei)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = LimeAccent,
                    )
                    IconButton(onClick = { revealImei = !revealImei }) {
                        Icon(
                            imageVector = if (revealImei) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle IMEI visibility",
                            tint = TextSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(current.description.ifBlank { "No narrative added yet." }, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showEditDialog = true }, modifier = Modifier.weight(1f)) {
                        Text(if (bn) "তথ্য সম্পাদনা" else "Edit Details", color = LimeAccent)
                    }
                    if (current.status in setOf("draft", "active", "additional_info_requested")) {
                        Button(
                            onClick = { viewModel.transitionIncident(current.id, "submitted_for_review") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                        ) {
                            Text(if (bn) "রিভিউতে পাঠান" else "Submit Review")
                        }
                    }
                }
            }
        }

        // Explainable AI Deterministic Rule Assessment
        item {
            GuardianCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (bn) "GD প্রস্তুতি ও XAI বিশ্লেষণ" else "GD Readiness & Explainable AI", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    StatusPill(text = "${assessment.readinessScore}%", color = if (assessment.readinessScore >= 75) LimeAccent else AmberWarning)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { assessment.readinessScore / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = LimeAccent,
                    trackColor = ElevatedSurface,
                )
                Spacer(Modifier.height(10.dp))
                Text(assessment.recommendedNextActionSummaryOrFallback(assessment.recommendation), style = MaterialTheme.typography.bodyMedium, color = LimeAccent)
                Spacer(Modifier.height(8.dp))
                assessment.ruleTrace.forEach { (rule, passed) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(
                            imageVector = if (passed) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            contentDescription = null,
                            tint = if (passed) EmeraldSafe else AmberWarning,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(rule, style = MaterialTheme.typography.bodyMedium, color = if (passed) TextPrimary else TextSecondary)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(assessment.uncertaintyNotice, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }

        // Report Draft & PDF Export Card
        item {
            GuardianCard(highlighted = true) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Description, contentDescription = null, tint = LimeAccent)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (bn) "পুলিশ GD খসড়া ও PDF" else "Police GD Draft & PDF Export", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(if (bn) "শুধুমাত্র খসড়া — অফিসিয়াল GD নয়" else "Marked clearly as DRAFT ONLY — verify before filing at gd.police.gov.bd", style = MaterialTheme.typography.labelSmall, color = AmberWarning)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.generateReportDraft(current)
                            showReportModal = true
                        },
                        modifier = Modifier.weight(1f).testTag("generate_report_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                    ) {
                        Text(if (bn) "খসড়া দেখুন" else "Prepare Draft")
                    }
                    OutlinedButton(
                        onClick = { viewModel.exportAndSharePdf(current) },
                        modifier = Modifier.weight(1f).testTag("export_pdf_btn"),
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = LimeAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF", color = LimeAccent)
                    }
                }
            }
        }

        // Evidence via Zero-Permission Photo Picker
        item {
            GuardianCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (bn) "সংযুক্ত প্রমাণ (${evidence.size})" else "Attached Evidence (${evidence.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = LimeAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (bn) "ছবি যোগ" else "Photo Picker", color = LimeAccent)
                    }
                }
                if (evidence.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(evidence) { ev ->
                            AsyncImage(
                                model = ev.uriString,
                                contentDescription = ev.displayName,
                                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(12.dp)),
                            )
                        }
                    }
                }
            }
        }

        // Event Timeline + Add Observation
        item {
            GuardianCard {
                Text(if (bn) "ঘটনার টাইমলাইন" else "Event Timeline", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                events.forEachIndexed { idx, ev ->
                    TimelineStepRow(
                        title = ev.eventType.replace('_', ' ').replaceFirstChar { it.uppercase() },
                        subtitle = ev.note,
                        timestamp = timeFmt.format(Date(ev.eventTimestampMillis)),
                        isLast = idx == events.lastIndex,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newNote,
                        onValueChange = { newNote = it },
                        placeholder = { Text(if (bn) "নতুন পর্যবেক্ষণ যোগ করুন..." else "Add timeline observation...") },
                        modifier = Modifier.weight(1f),
                        colors = fieldColors,
                    )
                    IconButton(
                        onClick = {
                            if (newNote.isNotBlank()) {
                                viewModel.addObservation(current.id, newNote)
                                newNote = ""
                            }
                        },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(LimeAccent),
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Add observation", tint = ObsidianBlack)
                    }
                }
            }
        }

        if (current.status != "cancelled" && current.status != "closed") {
            item {
                TextButton(
                    onClick = { showCancelConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (bn) "ঘটনা বাতিল করুন" else "Cancel Incident", color = CrimsonAlert)
                }
            }
        }
    }

    if (showEditDialog) {
        var desc by remember { mutableStateOf(current.description) }
        var loc by remember { mutableStateOf(current.locationLabel) }
        var brand by remember { mutableStateOf(current.deviceBrand) }
        var model by remember { mutableStateOf(current.deviceModel) }
        var imei by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = CharcoalSurface,
            title = { Text("Update Incident Details", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Device Brand") }, colors = fieldColors)
                    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Device Model") }, colors = fieldColors)
                    OutlinedTextField(value = imei, onValueChange = { imei = it }, label = { Text("Update 15-digit IMEI (optional)") }, colors = fieldColors)
                    OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Location / Thana") }, colors = fieldColors)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Narrative") }, minLines = 3, colors = fieldColors)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateIncident(current.id, desc, loc, brand, model, imei.ifBlank { null })
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Close", color = TextSecondary) }
            },
        )
    }

    if (showReportModal) {
        val draftBody = current.reportDraftText ?: "Generating draft..."
        AlertDialog(
            onDismissRequest = { showReportModal = false },
            containerColor = CharcoalSurface,
            title = { Text("Verified Draft Preview", color = LimeAccent) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Compare confirmed fields carefully. Guardian AI does NOT submit to Bangladesh Police.", style = MaterialTheme.typography.labelSmall, color = AmberWarning)
                    Text(draftBody, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.exportAndSharePdf(current) },
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                ) { Text("Export PDF") }
            },
            dismissButton = {
                TextButton(onClick = { showReportModal = false }) { Text("Close", color = TextSecondary) }
            },
        )
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            containerColor = CharcoalSurface,
            title = { Text("Cancel this incident?", color = TextPrimary) },
            text = { Text("Your timeline history will remain preserved locally for auditability.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.transitionIncident(current.id, "cancelled")
                        showCancelConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert),
                ) { Text("Confirm Cancel") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("Keep Active", color = TextSecondary) }
            },
        )
    }
}

private fun com.example.domain.ExplainableAssessment.recommendedNextActionSummaryOrFallback(s: String): String = s

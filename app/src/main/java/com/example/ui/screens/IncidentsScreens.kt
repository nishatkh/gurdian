package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.ui.theme.ButterYellow
import com.example.ui.theme.ButterYellowSoft
import com.example.ui.theme.CreamFieldSurface
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
            .background(WarmIvory),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = if (bn) "ঘটনার তালিকা" else "Incident Records",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
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
                        val isSel = filterStatus == st
                        FilterChip(
                            selected = isSel,
                            onClick = { filterStatus = st },
                            label = {
                                Text(if (st == "all") (if (bn) "সব" else "All") else BilingualStrings.statusLabel(st, bn))
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftCharcoal,
                                selectedLabelColor = OnCharcoal,
                                containerColor = SoftCreamSurface,
                                labelColor = TextSecondary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSel,
                                borderColor = SoftOutline,
                                selectedBorderColor = SoftCharcoal,
                            ),
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.Report,
                        title = if (bn) "কোনো ঘটনা পাওয়া যায়নি" else "No Incidents Recorded",
                        description = if (bn) "নতুন ঘটনা যোগ করতে নিচের + বাটনে চাপুন।"
                        else "Tap + below or open Emergency Mode to record a lost or stolen phone incident.",
                    )
                }
            } else {
                items(filtered, key = { it.id }) { inc ->
                    val cardTint = when (inc.status) {
                        "active" -> PastelPinkSoft
                        "closed", "approved" -> SageGreenSoft
                        "cancelled" -> SoftCreamSurface
                        else -> PowderBlueSoft
                    }
                    GuardianCard(
                        highlighted = inc.status == "active",
                        containerColor = cardTint,
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
                                color = if (inc.status == "active") WarmCoral else SoftCharcoal,
                                backgroundColor = if (inc.status == "active") WarmCoralSoft else SoftCreamSurface,
                            )
                            StatusPill(
                                text = if (inc.syncStatus == "synced") "Synced" else "Offline Saved",
                                color = SoftCharcoal,
                                backgroundColor = if (inc.syncStatus == "synced") SageGreenSoft else ButterYellowSoft,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
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
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(54.dp)) }
        }

        FloatingActionButton(
            onClick = onCreateIncident,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 112.dp)
                .testTag("new_incident_fab"),
            shape = CircleShape,
            containerColor = SoftCharcoal,
            contentColor = ButterYellow,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Create new incident")
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
        Box(modifier = Modifier.fillMaxSize().background(WarmIvory), contentAlignment = Alignment.Center) {
            Text("Incident not found", color = TextSecondary)
        }
        return
    }

    val decryptedImei = remember(current.imeiEncrypted) { viewModel.decryptImei(current.imeiEncrypted) }
    val assessment = remember(current, events.size) { viewModel.computeAssessment(current, events.size) }
    val timeFmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SoftCharcoal,
        unfocusedBorderColor = SoftOutline,
        focusedContainerColor = SoftCreamSurface,
        unfocusedContainerColor = CreamFieldSurface,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmIvory),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = CircleShape,
                    color = SoftCreamSurface,
                    border = BorderStroke(1.dp, SoftOutline),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("incident_detail_back")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = SoftCharcoal)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(current.title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Text("ID: ${current.id.take(8).uppercase()}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                StatusPill(
                    text = BilingualStrings.statusLabel(current.status, bn),
                    color = SoftCharcoal,
                    backgroundColor = ButterYellowSoft,
                )
            }
        }

        // Core Device & Masked IMEI Card
        item {
            OrganicEditorialCard(
                containerColor = PowderBlueSoft,
                blobColor = PowderBlue,
                accentShapeColor = ButterYellow,
            ) {
                Text(if (bn) "ডিভাইস ও ঘটনার তথ্য" else "Device & Incident Summary", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("${BilingualStrings.categoryLabel(current.category, bn)} • ${current.locationLabel.ifBlank { "Location not recorded" }}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("${current.deviceBrand} ${current.deviceModel}".trim().ifBlank { "Device details not set" }, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (revealImei && decryptedImei.isNotBlank()) "IMEI: $decryptedImei"
                        else "IMEI: ${KeystoreCryptoManager.maskImei(decryptedImei)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = SoftCharcoal,
                    )
                    IconButton(onClick = { revealImei = !revealImei }) {
                        Icon(
                            imageVector = if (revealImei) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Toggle IMEI visibility",
                            tint = SoftCharcoal,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(current.description.ifBlank { "No narrative added yet." }, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, SoftCharcoal.copy(alpha = 0.28f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = SoftCreamSurface),
                    ) {
                        Text(if (bn) "তথ্য সম্পাদনা" else "Edit Details", color = SoftCharcoal)
                    }
                    if (current.status in setOf("draft", "active", "additional_info_requested")) {
                        Button(
                            onClick = { viewModel.transitionIncident(current.id, "submitted_for_review") },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
                        ) {
                            Text(if (bn) "রিভিউতে পাঠান" else "Submit Review")
                        }
                    }
                }
            }
        }

        // Explainable AI Deterministic Rule Assessment with Circular Numerical Ring
        item {
            OrganicEditorialCard(
                containerColor = ButterYellowSoft,
                blobColor = ButterYellow,
                accentShapeColor = PastelPink,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SoftCircularProgressRing(
                        progress = assessment.readinessScore / 100f,
                        valueText = "${assessment.readinessScore}%",
                        captionText = "GD Ready",
                        size = 90.dp,
                        progressColor = SoftCharcoal,
                        trackColor = SoftCreamSurface,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (bn) "GD প্রস্তুতি ও XAI বিশ্লেষণ" else "GD Readiness & XAI", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(assessment.recommendedNextActionSummaryOrFallback(assessment.recommendation), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
                Spacer(Modifier.height(14.dp))
                assessment.ruleTrace.forEach { (rule, passed) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                        Icon(
                            imageVector = if (passed) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                            contentDescription = null,
                            tint = if (passed) SoftCharcoal else WarmCoral,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(rule, style = MaterialTheme.typography.bodyMedium, color = if (passed) TextPrimary else TextSecondary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(assessment.uncertaintyNotice, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }

        // Report Draft & PDF Export Card
        item {
            OrganicEditorialCard(
                containerColor = SageGreenSoft,
                blobColor = SageGreen,
                accentShapeColor = PowderBlue,
                dottedBorder = true,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = SoftCharcoal)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (bn) "পুলিশ GD খসড়া ও PDF" else "Police GD Draft & PDF Export", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(if (bn) "শুধুমাত্র খসড়া — অফিসিয়াল GD নয়" else "Marked clearly as DRAFT ONLY — verify before filing at gd.police.gov.bd", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.generateReportDraft(current)
                            showReportModal = true
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("generate_report_btn"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
                    ) {
                        Text(if (bn) "খসড়া দেখুন" else "Prepare Draft")
                    }
                    OutlinedButton(
                        onClick = { viewModel.exportAndSharePdf(current) },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("export_pdf_btn"),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, SoftCharcoal.copy(alpha = 0.28f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = SoftCreamSurface),
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF", color = SoftCharcoal)
                    }
                }
            }
        }

        // Evidence via Zero-Permission Photo Picker
        item {
            GuardianCard(containerColor = SoftCreamSurface) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (bn) "সংযুক্ত প্রমাণ (${evidence.size})" else "Attached Evidence (${evidence.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, SoftCharcoal.copy(alpha = 0.25f)),
                    ) {
                        Icon(Icons.Outlined.AddAPhoto, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (bn) "ছবি যোগ" else "Photo Picker", color = SoftCharcoal)
                    }
                }
                if (evidence.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(evidence) { ev ->
                            AsyncImage(
                                model = ev.uriString,
                                contentDescription = ev.displayName,
                                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(18.dp)),
                            )
                        }
                    }
                }
            }
        }

        // Event Timeline + Add Observation
        item {
            GuardianCard(containerColor = SoftCreamSurface) {
                Text(if (bn) "ঘটনার টাইমলাইন" else "Event Timeline", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                events.forEachIndexed { idx, ev ->
                    TimelineStepRow(
                        title = ev.eventType.replace('_', ' ').replaceFirstChar { it.uppercase() },
                        subtitle = ev.note,
                        timestamp = timeFmt.format(Date(ev.eventTimestampMillis)),
                        isLast = idx == events.lastIndex,
                        accentColor = if (idx == 0) ButterYellow else SageGreen,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newNote,
                        onValueChange = { newNote = it },
                        placeholder = { Text(if (bn) "নতুন পর্যবেক্ষণ যোগ করুন..." else "Add timeline observation...", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = fieldColors,
                    )
                    IconButton(
                        onClick = {
                            if (newNote.isNotBlank()) {
                                viewModel.addObservation(current.id, newNote)
                                newNote = ""
                            }
                        },
                        modifier = Modifier.size(50.dp).clip(CircleShape).background(SoftCharcoal),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Add observation", tint = OnCharcoal)
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
                    Text(if (bn) "ঘটনা বাতিল করুন" else "Cancel Incident", color = WarmCoral)
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
            containerColor = SoftCreamSurface,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Update Incident Details", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Device Brand") }, shape = RoundedCornerShape(18.dp), colors = fieldColors)
                    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Device Model") }, shape = RoundedCornerShape(18.dp), colors = fieldColors)
                    OutlinedTextField(value = imei, onValueChange = { imei = it }, label = { Text("Update 15-digit IMEI (optional)") }, shape = RoundedCornerShape(18.dp), colors = fieldColors)
                    OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Location / Thana") }, shape = RoundedCornerShape(18.dp), colors = fieldColors)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Narrative") }, minLines = 3, shape = RoundedCornerShape(18.dp), colors = fieldColors)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateIncident(current.id, desc, loc, brand, model, imei.ifBlank { null })
                        showEditDialog = false
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
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
            containerColor = SoftCreamSurface,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Verified Draft Preview", color = SoftCharcoal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Compare confirmed fields carefully. Guardian AI does NOT submit to Bangladesh Police.", style = MaterialTheme.typography.labelSmall, color = WarmCoral)
                    Text(draftBody, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.exportAndSharePdf(current) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
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
            containerColor = SoftCreamSurface,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Cancel this incident?", color = TextPrimary) },
            text = { Text("Your timeline history will remain preserved locally for auditability.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.transitionIncident(current.id, "cancelled")
                        showCancelConfirm = false
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmCoral, contentColor = OnCharcoal),
                ) { Text("Confirm Cancel") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("Keep Active", color = TextSecondary) }
            },
        )
    }
}

private fun com.example.domain.ExplainableAssessment.recommendedNextActionSummaryOrFallback(s: String): String = s

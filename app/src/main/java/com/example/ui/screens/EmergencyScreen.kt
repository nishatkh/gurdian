package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.BilingualStrings
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.GuardianCard
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.LimeGradientCard
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StatusPill
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.GuardianMainUiState
import com.example.ui.viewmodel.GuardianViewModel

@SuppressLint("MissingPermission")
@Composable
fun EmergencyScreen(
    state: GuardianMainUiState,
    viewModel: GuardianViewModel,
    onIncidentCreated: (String) -> Unit,
) {
    val context = LocalContext.current
    val bn = state.prefs.language == "bn"

    var selectedCategory by remember { mutableStateOf("suspected_theft") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var locationLabel by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf(state.prefs.deviceBrand) }
    var model by remember { mutableStateOf(state.prefs.deviceModel) }
    var rawImei by remember { mutableStateOf(viewModel.decryptImei(state.prefs.deviceImeiEncrypted)) }

    var capturedLat by remember { mutableStateOf<Double?>(null) }
    var capturedLng by remember { mutableStateOf<Double?>(null) }
    var capturedAcc by remember { mutableStateOf<Float?>(null) }
    var locationConsent by remember { mutableStateOf(false) }

    var showActivateConfirm by remember { mutableStateOf(false) }
    var showCall999Confirm by remember { mutableStateOf(false) }
    var showShareContactDialog by remember { mutableStateOf(false) }

    fun tryReadLastLocation() {
        val lm = context.getSystemService(LocationManager::class.java)
        val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (loc != null) {
            capturedLat = loc.latitude
            capturedLng = loc.longitude
            capturedAcc = loc.accuracy
            locationConsent = true
            if (locationLabel.isBlank()) {
                locationLabel = "GPS (${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}) ±${loc.accuracy.toInt()}m"
            }
            viewModel.showMessage("Consent-based location captured.")
        } else {
            locationConsent = true
            viewModel.showMessage("Location permission granted, but GPS fix unavailable right now. Enter landmark manually.")
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                tryReadLastLocation()
            } else {
                locationConsent = false
                viewModel.showMessage("Location permission declined. Enter area/Thana name manually.")
            }
        },
    )

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
            .background(ObsidianBlack)
            .testTag("emergency_list"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LimeGradientCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(text = if (bn) "জরুরি সহায়তা" else "EMERGENCY RESPONSE", color = CrimsonAlert)
                    Icon(Icons.Filled.GppMaybe, contentDescription = null, tint = LimeAccent)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = BilingualStrings.emergencyTitle(bn),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = BilingualStrings.disclaimer(bn),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showCall999Confirm = true },
                        modifier = Modifier.weight(1f).testTag("dial_999_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert),
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (bn) "৯৯৯ কল" else "Call 999")
                    }
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://gd.police.gov.bd/")))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = LimeAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Online GD", color = LimeAccent)
                    }
                }
            }
        }

        // Incident Logging Form
        item {
            GuardianCard(highlighted = true) {
                Text(
                    text = if (bn) "১. ঘটনার ধরন ও তথ্য সংরক্ষণ" else "1. Log Incident Offline-First",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(10.dp))

                val categories = listOf("suspected_theft", "lost_phone", "snatching", "sim_compromise")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(BilingualStrings.categoryLabel(cat, bn)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = ObsidianBlack,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (bn) "শিরোনাম (যেমন: ফার্মগেটে ফোন চুরি)" else "Short Title (e.g. Phone stolen near Farmgate)") },
                    modifier = Modifier.fillMaxWidth().testTag("emergency_title_input"),
                    colors = fieldColors,
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (bn) "ঘটনার বিবরণ (বাংলা বা English)" else "Describe what happened (English or Bangla)") },
                    modifier = Modifier.fillMaxWidth().testTag("emergency_desc_input"),
                    minLines = 3,
                    colors = fieldColors,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand") },
                        modifier = Modifier.weight(1f),
                        colors = fieldColors,
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model") },
                        modifier = Modifier.weight(1f),
                        colors = fieldColors,
                        singleLine = true,
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = rawImei,
                    onValueChange = { rawImei = it },
                    label = { Text("15-digit IMEI (Encrypted in Keystore)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = locationLabel,
                    onValueChange = { locationLabel = it },
                    label = { Text(if (bn) "সম্ভাব্য স্থান / থানা এলাকা" else "Area / Thana / Landmark") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (hasFine) tryReadLastLocation() else locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = LimeAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (locationConsent) (if (bn) "লোকেশন যুক্ত হয়েছে" else "Consent Location Attached")
                        else (if (bn) "সম্মতিসহ বর্তমান অবস্থান যোগ করুন" else "Capture Location (Consent Required)"),
                        color = LimeAccent,
                    )
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showActivateConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("activate_theft_mode_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                ) {
                    Text(
                        text = if (bn) "ঘটনা সংরক্ষণ ও ইমার্জেন্সি মোড শুরু করুন" else "Save Incident & Start Response",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        // Quick Checklist & Consent-Based Contact Alert
        item {
            GuardianCard {
                Text(
                    text = if (bn) "২. দ্রুত সুরক্ষা ও বিশ্বস্ত পরিচিতি" else "2. Immediate Checklist & Trusted Sharing",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(8.dp))
                state.checklist.take(4).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleChecklist(item) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = item.isCompleted,
                            onCheckedChange = { viewModel.toggleChecklist(item) },
                            colors = CheckboxDefaults.colors(checkedColor = LimeAccent, checkmarkColor = ObsidianBlack),
                        )
                        Text(
                            text = if (bn) item.titleBn else item.titleEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.isCompleted) TextMuted else TextPrimary,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showShareContactDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = LimeAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (bn) "বিশ্বস্ত পরিচিতিকে সতর্কতা পাঠান" else "Alert Trusted Contact (With Consent)", color = LimeAccent)
                }
            }
        }
    }

    if (showActivateConfirm) {
        AlertDialog(
            onDismissRequest = { showActivateConfirm = false },
            containerColor = CharcoalSurface,
            title = { Text(if (bn) "ঘটনা নিশ্চিত করুন" else "Confirm Emergency Incident", color = TextPrimary) },
            text = {
                Text(
                    text = if (bn) "এটি আপনার ডিভাইসে অফলাইনে সংরক্ষিত হবে এবং নেটওয়ার্ক পেলে সিঙ্ক হবে। এটি সরাসরি পুলিশ বা ৯৯৯-এ কল করবে না।"
                    else "This saves the incident locally in encrypted storage and queues it for sync. It does NOT automatically contact police or external services.",
                    color = TextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showActivateConfirm = false
                        val finalTitle = title.ifBlank { "${BilingualStrings.categoryLabel(selectedCategory, false)} Report" }
                        viewModel.createEmergencyIncident(
                            category = selectedCategory,
                            title = finalTitle,
                            description = description,
                            deviceBrand = brand,
                            deviceModel = model,
                            rawImei = rawImei,
                            locationLabel = locationLabel,
                            latitude = capturedLat,
                            longitude = capturedLng,
                            accuracy = capturedAcc,
                            locationConsent = locationConsent,
                            onCreated = onIncidentCreated,
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                ) { Text("Confirm & Save") }
            },
            dismissButton = {
                TextButton(onClick = { showActivateConfirm = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    if (showCall999Confirm) {
        AlertDialog(
            onDismissRequest = { showCall999Confirm = false },
            containerColor = CharcoalSurface,
            title = { Text("Open Dialer for 999?", color = TextPrimary) },
            text = { Text("This opens your phone dialer with 999 (Bangladesh National Emergency Service). You decide when to place the call.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showCall999Confirm = false
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:999")))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert),
                ) { Text("Open Dialer") }
            },
            dismissButton = {
                TextButton(onClick = { showCall999Confirm = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    if (showShareContactDialog) {
        val active = state.activeIncident
        var includeLoc by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showShareContactDialog = false },
            containerColor = CharcoalSurface,
            title = { Text("Consent-Gated Contact Sharing", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.contacts.isEmpty()) {
                        Text("No trusted contacts saved yet. Add one in Profile -> Trusted Contacts.", color = TextSecondary)
                    } else if (active == null) {
                        Text("Log an incident first before sharing incident details.", color = TextSecondary)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includeLoc, onCheckedChange = { includeLoc = it })
                            Text("Include recorded area label (explicit consent)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                        state.contacts.forEach { contact ->
                            Button(
                                onClick = {
                                    viewModel.shareIncidentWithContact(active.id, contact, includeLoc)
                                    val shareText = "Guardian AI Alert: ${active.title} (${active.category}). " +
                                        (if (includeLoc && active.locationLabel.isNotBlank()) "Area: ${active.locationLabel}. " else "") +
                                        "Please assist if unreachable."
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share with ${contact.name}"))
                                    showShareContactDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                            ) {
                                Text("Share with ${contact.name} (${contact.phoneMasked})")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareContactDialog = false }) { Text("Close", color = TextSecondary) }
            },
        )
    }
}

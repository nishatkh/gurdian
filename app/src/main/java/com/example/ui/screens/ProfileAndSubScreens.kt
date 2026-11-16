package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.ChecklistItemEntity
import com.example.security.KeystoreCryptoManager
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.EmptyStateView
import com.example.ui.theme.GuardianCard
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.LimeGlowTint
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StatusPill
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.GuardianMainUiState
import com.example.ui.viewmodel.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    state: GuardianMainUiState,
    viewModel: GuardianViewModel,
    onOpenChecklist: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenResources: () -> Unit,
) {
    val bn = state.prefs.language == "bn"
    var showVaultDialog by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("profile_list"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = if (bn) "প্রোফাইল ও ডিভাইস ভল্ট" else "Profile & Device Vault",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
            Text(
                text = if (bn) "এনক্রিপ্টেড IMEI ভল্ট, ভাষা এবং গোপনীয়তা সেটিংস"
                else "Keystore-encrypted vault, language & explicit privacy controls",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        // Account Summary Card
        item {
            GuardianCard(highlighted = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(LimeGlowTint),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = "User profile", tint = LimeAccent)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(state.prefs.fullName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            text = if (state.prefs.offlineModeOnly) {
                                if (bn) "অফলাইন লোকাল মোড (${state.prefs.email.ifBlank { "local" }})"
                                else "Offline-First Workspace"
                            } else {
                                state.prefs.email
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                    StatusPill(
                        text = if (state.prefs.offlineModeOnly) "Offline" else "Synced Auth",
                        color = if (state.prefs.offlineModeOnly) AmberWarning else EmeraldSafe,
                    )
                }
            }
        }

        // Language Selector
        item {
            GuardianCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = LimeAccent)
                        Column {
                            Text(if (bn) "অ্যাপের ভাষা" else "App Language", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(if (bn) "বাংলা অথবা English" else "Switch between English & Bangla", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !bn,
                            onClick = { viewModel.toggleLanguage("en") },
                            label = { Text("EN") },
                            modifier = Modifier.testTag("lang_en_chip"),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LimeAccent, selectedLabelColor = ObsidianBlack),
                        )
                        FilterChip(
                            selected = bn,
                            onClick = { viewModel.toggleLanguage("bn") },
                            label = { Text("বাংলা") },
                            modifier = Modifier.testTag("lang_bn_chip"),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LimeAccent, selectedLabelColor = ObsidianBlack),
                        )
                    }
                }
            }
        }

        // Device & Encrypted IMEI Vault
        item {
            val hasImei = state.prefs.deviceImeiLast4.isNotBlank()
            GuardianCard(highlighted = hasImei) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = LimeAccent)
                        Column {
                            Text(if (bn) "ডিভাইস ও IMEI ভল্ট" else "Device & Encrypted IMEI Vault", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                text = if (hasImei) {
                                    "${state.prefs.deviceBrand} ${state.prefs.deviceModel} • ••••${state.prefs.deviceImeiLast4}"
                                } else {
                                    if (bn) "এখনও কোনো IMEI সংরক্ষিত হয়নি" else "No device IMEI saved yet"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasImei) LimeAccent else TextSecondary,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showVaultDialog = true },
                        modifier = Modifier.testTag("edit_device_vault_btn"),
                    ) {
                        Text(if (hasImei) (if (bn) "সম্পাদনা" else "Update") else (if (bn) "যোগ করুন" else "Set Up"), color = LimeAccent)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (bn) "Android Keystore AES-256-GCM দ্বারা এনক্রিপ্টেড এবং শুধু আপনার ডিভাইসে সংরক্ষিত।"
                    else "Encrypted locally with Android Keystore AES-GCM. Raw IMEI is masked in UI & never exposed in logs.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }

        // Protection Hub Links
        item {
            GuardianCard {
                Text(if (bn) "সুরক্ষা টুলস" else "Protection Hub", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                ProfileActionRow(
                    icon = Icons.Filled.Checklist,
                    title = if (bn) "অ্যাকাউন্ট সুরক্ষা চেকলিস্ট (${state.completedChecklistCount}/${state.checklist.size})"
                    else "Account Protection Checklist (${state.completedChecklistCount}/${state.checklist.size})",
                    subtitle = if (bn) "সিম, বিকাশ/নগদ, ব্যাংক, Google, Apple ও GD ধাপ" else "SIM, MFS, Banking, Google, Apple & GD steps",
                    tag = "profile_checklist_row",
                    onClick = onOpenChecklist,
                )
                ProfileActionRow(
                    icon = Icons.Filled.Contacts,
                    title = if (bn) "বিশ্বস্ত পরিচিতি (${state.contacts.size})" else "Trusted Contacts (${state.contacts.size})",
                    subtitle = if (bn) "জরুরি অবস্থায় সম্মতি-ভিত্তিক সতর্কতা শেয়ার" else "Consent-gated sharing & access revocation",
                    tag = "profile_contacts_row",
                    onClick = onOpenContacts,
                )
                ProfileActionRow(
                    icon = Icons.Filled.MenuBook,
                    title = if (bn) "যাচাইকৃত বাংলাদেশি সহায়তা ডিরেক্টরি" else "Verified Bangladesh Resources",
                    subtitle = if (bn) "999, Online GD, BTRC/NEIR, মোবাইল অপারেটর ও MFS" else "999, Online GD, BTRC, GP/Robi/BL, bKash/Nagad/Rocket",
                    tag = "profile_resources_row",
                    onClick = onOpenResources,
                )
            }
        }

        // Privacy & Explicit Consent Settings
        item {
            GuardianCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = LimeAccent)
                    Text(if (bn) "গোপনীয়তা ও সম্মতি নিয়ন্ত্রণ" else "Privacy & Consent Controls", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Spacer(Modifier.height(10.dp))
                ConsentSwitchRow(
                    label = if (bn) "ইমার্জেন্সি অবস্থায় লোকেশন ক্যাপচার অনুমতি" else "Allow consent-based location in Emergency Mode",
                    checked = state.prefs.consentLocation,
                    onCheckedChange = {
                        viewModel.updatePrivacyPrefs(it, state.prefs.consentAi, state.prefs.notifySync)
                    },
                )
                ConsentSwitchRow(
                    label = if (bn) "AI সহায়কের জন্য সক্রিয় ঘটনার তথ্য ব্যবহার" else "Share active incident summary with AI Assistant",
                    checked = state.prefs.consentAi,
                    onCheckedChange = {
                        viewModel.updatePrivacyPrefs(state.prefs.consentLocation, it, state.prefs.notifySync)
                    },
                )
                ConsentSwitchRow(
                    label = if (bn) "ব্যাকগ্রাউন্ড আউটবক্স সিঙ্ক সক্রিয় রাখুন" else "Auto-sync queued incidents when online",
                    checked = state.prefs.notifySync,
                    onCheckedChange = {
                        viewModel.updatePrivacyPrefs(state.prefs.consentLocation, state.prefs.consentAi, it)
                    },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Backend Endpoint: ${BuildConfig.BACKEND_BASE_URL}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }

        item {
            OutlinedButton(
                onClick = { showSignOutConfirm = true },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = CrimsonAlert, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (bn) "সাইন আউট" else "Sign Out", color = CrimsonAlert)
            }
        }
    }

    if (showVaultDialog) {
        var brand by remember { mutableStateOf(state.prefs.deviceBrand) }
        var model by remember { mutableStateOf(state.prefs.deviceModel) }
        var imei by remember { mutableStateOf(viewModel.decryptImei(state.prefs.deviceImeiEncrypted)) }
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LimeAccent,
            unfocusedBorderColor = SubtleBorder,
            focusedContainerColor = CharcoalSurface,
            unfocusedContainerColor = CharcoalSurface,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
        )
        AlertDialog(
            onDismissRequest = { showVaultDialog = false },
            containerColor = CharcoalSurface,
            title = { Text(if (bn) "ডিভাইস ও IMEI ভল্ট আপডেট" else "Device & Encrypted IMEI Vault", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (bn) "ফোনের বক্স বা *#06# থেকে প্রাপ্ত ১৫-ডিজিটের IMEI দিন।"
                        else "Enter your phone's 15-digit IMEI (*#06# or retail box). Luhn checksum is validated.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand (e.g. Samsung)") }, colors = fieldColors, singleLine = true)
                    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model (e.g. Galaxy A55)") }, colors = fieldColors, singleLine = true)
                    OutlinedTextField(
                        value = imei,
                        onValueChange = { imei = it.filter { ch -> ch.isDigit() }.take(15) },
                        label = { Text("15-digit IMEI") },
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.testTag("vault_imei_input"),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveDeviceVault(brand, model, imei)
                        showVaultDialog = false
                    },
                    modifier = Modifier.testTag("vault_save_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                ) { Text(if (bn) "সংরক্ষণ" else "Save Encrypted") }
            },
            dismissButton = {
                TextButton(onClick = { showVaultDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            containerColor = CharcoalSurface,
            title = { Text(if (bn) "সাইন আউট নিশ্চিত করুন" else "Sign Out?", color = TextPrimary) },
            text = { Text(if (bn) "লোকাল অফলাইন তথ্য ডিভাইসে সংরক্ষিত থাকবে।" else "Your offline incidents remain stored safely on this device.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        viewModel.signOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert),
                ) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }
}

@Composable
private fun ProfileActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = LimeAccent)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary)
    }
}

@Composable
private fun ConsentSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBlack, checkedTrackColor = LimeAccent),
        )
    }
}

@Composable
fun AccountProtectionScreen(
    state: GuardianMainUiState,
    viewModel: GuardianViewModel,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val bn = state.prefs.language == "bn"
    val total = state.checklist.size.coerceAtLeast(1)
    val done = state.completedChecklistCount
    val fmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    var noteTarget by remember { mutableStateOf<ChecklistItemEntity?>(null) }
    var dialTarget by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("checklist_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (bn) "অ্যাকাউন্ট সুরক্ষা চেকলিস্ট" else "Account Protection Checklist", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Text(if (bn) "$done / $total ধাপ সম্পন্ন" else "$done of $total steps completed", style = MaterialTheme.typography.bodyMedium, color = LimeAccent)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { done.toFloat() / total.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = LimeAccent,
                trackColor = ElevatedSurface,
            )
        }

        items(state.checklist, key = { it.id }) { item ->
            GuardianCard(highlighted = !item.isCompleted, modifier = Modifier.testTag("checklist_item_${item.id}")) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = item.isCompleted,
                        onCheckedChange = { viewModel.toggleChecklist(item) },
                        modifier = Modifier.testTag("checklist_toggle_${item.id}"),
                        colors = CheckboxDefaults.colors(checkedColor = LimeAccent, checkmarkColor = ObsidianBlack),
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${item.sortOrder}. ${if (bn) item.titleBn else item.titleEn}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (item.isCompleted) TextMuted else TextPrimary,
                        )
                        Text(
                            text = if (bn) item.whatToDoBn else item.whatToDoEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = "${if (bn) "কেন জরুরি: " else "Why it matters: "}${if (bn) item.whyItMattersBn else item.whyItMattersEn}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                        if (item.completedAtMillis != null) {
                            StatusPill(text = "Done ${fmt.format(Date(item.completedAtMillis))}", color = EmeraldSafe)
                        }
                        if (item.notes.isNotBlank()) {
                            Text("Note: ${item.notes}", style = MaterialTheme.typography.labelMedium, color = LimeAccent)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!item.officialUrl.isNullOrBlank()) {
                                OutlinedButton(onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.officialUrl)))
                                }) {
                                    Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = LimeAccent, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (bn) "পোর্টাল" else "Official", color = LimeAccent)
                                }
                            }
                            if (!item.officialPhone.isNullOrBlank()) {
                                OutlinedButton(onClick = { dialTarget = item.officialPhone }) {
                                    Icon(Icons.Filled.Call, contentDescription = null, tint = LimeAccent, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(item.officialPhone, color = LimeAccent)
                                }
                            }
                            TextButton(onClick = { noteTarget = item }) {
                                Text(if (bn) "নোট" else "Note", color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    noteTarget?.let { target ->
        var noteText by remember(target.id) { mutableStateOf(target.notes) }
        AlertDialog(
            onDismissRequest = { noteTarget = null },
            containerColor = CharcoalSurface,
            title = { Text("Step Note", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Reference / GD / Operator Ticket #") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LimeAccent,
                        unfocusedBorderColor = SubtleBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateChecklistNotes(target, noteText)
                        noteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { noteTarget = null }) { Text("Cancel", color = TextSecondary) } },
        )
    }

    dialTarget?.let { number ->
        AlertDialog(
            onDismissRequest = { dialTarget = null },
            containerColor = CharcoalSurface,
            title = { Text("Open Dialer for $number?", color = TextPrimary) },
            text = { Text("This opens your phone dialer without auto-calling.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        dialTarget = null
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                ) { Text("Open Dialer") }
            },
            dismissButton = { TextButton(onClick = { dialTarget = null }) { Text("Cancel", color = TextSecondary) } },
        )
    }
}

@Composable
fun TrustedContactsScreen(
    state: GuardianMainUiState,
    viewModel: GuardianViewModel,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val bn = state.prefs.language == "bn"
    var showAddDialog by remember { mutableStateOf(false) }
    val fmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("contacts_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Text(if (bn) "বিশ্বস্ত পরিচিতি" else "Trusted Contacts", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                }
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_trusted_contact_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (bn) "যোগ" else "Add")
                }
            }
        }

        if (state.contacts.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Filled.Contacts,
                    title = if (bn) "কোনো বিশ্বস্ত পরিচিতি নেই" else "No Trusted Contacts Added",
                    description = if (bn) "পরিবারের সদস্য বা বিশ্বস্ত বন্ধুর নম্বর যোগ করুন (লোকাল এনক্রিপ্টেড)।"
                    else "Add a family member or trusted friend. Numbers are encrypted locally and shared ONLY when you explicitly confirm.",
                )
            }
        } else {
            items(state.contacts, key = { it.id }) { contact ->
                GuardianCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(contact.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("${contact.relationshipLabel} • ${contact.phoneMasked}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(
                                text = if (contact.notifySmsConsent) "Explicit SMS share consent saved" else "Share confirmation required per event",
                                style = MaterialTheme.typography.labelSmall,
                                color = LimeAccent,
                            )
                        }
                        IconButton(onClick = { viewModel.deleteTrustedContact(contact.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete contact", tint = CrimsonAlert)
                        }
                    }
                }
            }
        }

        if (state.shareLogs.isNotEmpty()) {
            item {
                Text(if (bn) "শেয়ার ইতিহাস ও অনুমতি বাতিল" else "Share Access History & Revocation", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            items(state.shareLogs, key = { it.id }) { log ->
                GuardianCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shared with ${log.contactName}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                text = "${fmt.format(Date(log.sharedAtMillis))} • Location: ${if (log.includeLocation) "Included" else "Hidden"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                        if (log.isRevoked) {
                            StatusPill(text = "Revoked", color = TextMuted)
                        } else {
                            OutlinedButton(onClick = { viewModel.revokeShare(log.id) }) {
                                Text("Revoke", color = CrimsonAlert)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var relation by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var consent by remember { mutableStateOf(false) }
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LimeAccent,
            unfocusedBorderColor = SubtleBorder,
            focusedContainerColor = CharcoalSurface,
            unfocusedContainerColor = CharcoalSurface,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
        )
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CharcoalSurface,
            title = { Text("Add Trusted Contact", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, colors = fieldColors, singleLine = true)
                    OutlinedTextField(value = relation, onValueChange = { relation = it }, label = { Text("Relationship (e.g. Sister)") }, colors = fieldColors, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (+8801...)") }, colors = fieldColors, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = consent, onCheckedChange = { consent = it }, colors = CheckboxDefaults.colors(checkedColor = LimeAccent))
                        Text("I consent to manually send emergency summaries to this contact.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addTrustedContact(name, relation.ifBlank { "Family" }, phone, consent)
                        showAddDialog = false
                    },
                    enabled = name.isNotBlank() && phone.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = TextSecondary) } },
        )
    }
}

@Composable
fun ResourcesDirectoryScreen(
    state: GuardianMainUiState,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val bn = state.prefs.language == "bn"
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var dialTarget by remember { mutableStateOf<String?>(null) }

    val categories = remember(state.resources) {
        listOf("All") + state.resources.map { it.category }.distinct()
    }
    val filtered = remember(state.resources, query, selectedCategory) {
        state.resources.filter { res ->
            val matchCat = selectedCategory == "All" || res.category == selectedCategory
            val matchQ = query.isBlank() || res.name.contains(query, ignoreCase = true) ||
                res.contactNumber.contains(query, ignoreCase = true) ||
                res.category.contains(query, ignoreCase = true)
            matchCat && matchQ
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("resources_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column {
                    Text(if (bn) "যাচাইকৃত সহায়তা ডিরেক্টরি" else "Verified Bangladesh Directory", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Text(if (bn) "যাচাইয়ের তারিখসহ অফিসিয়াল পোর্টাল ও নম্বর" else "Official portals & helplines with verification dates", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                placeholder = { Text(if (bn) "নাম বা নম্বর দিয়ে খুঁজুন..." else "Search 999, bKash, GP, Online GD...") },
                modifier = Modifier.fillMaxWidth().testTag("resources_search_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LimeAccent,
                    unfocusedBorderColor = SubtleBorder,
                    focusedContainerColor = CharcoalSurface,
                    unfocusedContainerColor = CharcoalSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LimeAccent, selectedLabelColor = ObsidianBlack),
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { res ->
            val dialNum = res.contactNumber.substringBefore('/').filter { it.isDigit() || it == '+' }
            GuardianCard(highlighted = res.contactNumber == "999") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(text = res.category, color = LimeAccent)
                    StatusPill(text = "Verified ${res.verificationDate}", color = EmeraldSafe)
                }
                Spacer(Modifier.height(8.dp))
                Text(res.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(res.address, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dialNum.isNotBlank()) {
                        Button(
                            onClick = { dialTarget = dialNum },
                            colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(res.contactNumber)
                        }
                    }
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(res.sourceUrl)))
                    }) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = LimeAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Source", color = LimeAccent)
                    }
                }
            }
        }
    }

    dialTarget?.let { num ->
        AlertDialog(
            onDismissRequest = { dialTarget = null },
            containerColor = CharcoalSurface,
            title = { Text("Open Dialer for $num?", color = TextPrimary) },
            text = { Text("Guardian AI opens your device dialer so you can confirm before calling.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        dialTarget = null
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num")))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = ObsidianBlack),
                ) { Text("Open Dialer") }
            },
            dismissButton = { TextButton(onClick = { dialTarget = null }) { Text("Cancel", color = TextSecondary) } },
        )
    }
}

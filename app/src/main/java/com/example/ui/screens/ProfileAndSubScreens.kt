package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.ChecklistItemEntity
import com.example.ui.theme.ButterYellow
import com.example.ui.theme.ButterYellowSoft
import com.example.ui.theme.CreamFieldSurface
import com.example.ui.theme.EmptyStateView
import com.example.ui.theme.GuardianCard
import com.example.ui.theme.OliveTrack
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
import com.example.ui.theme.WarmCoral
import com.example.ui.theme.WarmCoralSoft
import com.example.ui.theme.WarmIvory
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
            .background(WarmIvory)
            .testTag("profile_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text(
                text = if (bn) "প্রোফাইল ও ডিভাইস ভল্ট" else "Profile & Device Vault",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (bn) "এনক্রিপ্টেড IMEI ভল্ট, ভাষা এবং গোপনীয়তা সেটিংস"
                else "Keystore-encrypted vault, language & explicit privacy controls",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        // Account Summary Card
        item {
            OrganicEditorialCard(
                containerColor = ButterYellowSoft,
                blobColor = ButterYellow,
                accentShapeColor = PastelPink,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = SoftCharcoal,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Person, contentDescription = "User profile", tint = ButterYellow)
                        }
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
                        text = if (state.prefs.offlineModeOnly) "Offline" else "Synced",
                        color = SoftCharcoal,
                        backgroundColor = if (state.prefs.offlineModeOnly) SoftCreamSurface else SageGreenSoft,
                    )
                }
            }
        }

        // Language Selector
        item {
            GuardianCard(containerColor = SoftCreamSurface) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    ) {
                        Icon(Icons.Outlined.Language, contentDescription = null, tint = SoftCharcoal)
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
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("lang_en_chip"),
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
                            label = { Text("বাংলা") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("lang_bn_chip"),
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
                    }
                }
            }
        }

        // Device & Encrypted IMEI Vault
        item {
            val hasImei = state.prefs.deviceImeiLast4.isNotBlank()
            OrganicEditorialCard(
                containerColor = PowderBlueSoft,
                blobColor = PowderBlue,
                accentShapeColor = SageGreen,
                dottedBorder = !hasImei,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    ) {
                        Icon(Icons.Outlined.Key, contentDescription = null, tint = SoftCharcoal)
                        Column {
                            Text(if (bn) "ডিভাইস ও IMEI ভল্ট" else "Device & IMEI Vault", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                text = if (hasImei) {
                                    "${state.prefs.deviceBrand} ${state.prefs.deviceModel} • ••••${state.prefs.deviceImeiLast4}"
                                } else {
                                    if (bn) "এখনও কোনো IMEI সংরক্ষিত হয়নি" else "No device IMEI saved yet"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                    Button(
                        onClick = { showVaultDialog = true },
                        modifier = Modifier.testTag("edit_device_vault_btn"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
                    ) {
                        Text(if (hasImei) (if (bn) "সম্পাদনা" else "Update") else (if (bn) "যোগ করুন" else "Set Up"))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (bn) "Android Keystore AES-256-GCM দ্বারা এনক্রিপ্টেড এবং শুধু আপনার ডিভাইসে সংরক্ষিত।"
                    else "Encrypted locally with Android Keystore AES-GCM. Raw IMEI is masked in UI & never exposed in logs.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }

        // Protection Hub Links
        item {
            GuardianCard(containerColor = SoftCreamSurface) {
                Text(if (bn) "সুরক্ষা টুলস" else "Protection Hub", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                ProfileActionRow(
                    icon = Icons.Outlined.Checklist,
                    badgeColor = ButterYellowSoft,
                    title = if (bn) "অ্যাকাউন্ট সুরক্ষা চেকলিস্ট (${state.completedChecklistCount}/${state.checklist.size})"
                    else "Account Protection Checklist (${state.completedChecklistCount}/${state.checklist.size})",
                    subtitle = if (bn) "সিম, বিকাশ/নগদ, ব্যাংক, Google, Apple ও GD ধাপ" else "SIM, MFS, Banking, Google, Apple & GD steps",
                    tag = "profile_checklist_row",
                    onClick = onOpenChecklist,
                )
                ProfileActionRow(
                    icon = Icons.Outlined.Contacts,
                    badgeColor = PastelPinkSoft,
                    title = if (bn) "বিশ্বস্ত পরিচিতি (${state.contacts.size})" else "Trusted Contacts (${state.contacts.size})",
                    subtitle = if (bn) "জরুরি অবস্থায় সম্মতি-ভিত্তিক সতর্কতা শেয়ার" else "Consent-gated sharing & access revocation",
                    tag = "profile_contacts_row",
                    onClick = onOpenContacts,
                )
                ProfileActionRow(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    badgeColor = SageGreenSoft,
                    title = if (bn) "যাচাইকৃত বাংলাদেশি সহায়তা ডিরেক্টরি" else "Verified Bangladesh Resources",
                    subtitle = if (bn) "999, Online GD, BTRC/NEIR, মোবাইল অপারেটর ও MFS" else "999, Online GD, BTRC, GP/Robi/BL, bKash/Nagad/Rocket",
                    tag = "profile_resources_row",
                    onClick = onOpenResources,
                )
            }
        }

        // Privacy & Explicit Consent Settings
        item {
            GuardianCard(containerColor = SoftCreamSurface) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.PrivacyTip, contentDescription = null, tint = SoftCharcoal)
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
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("sign_out_button"),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, WarmCoral.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = WarmCoralSoft),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = WarmCoral, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (bn) "সাইন আউট" else "Sign Out", color = WarmCoral)
            }
        }
    }

    if (showVaultDialog) {
        var brand by remember { mutableStateOf(state.prefs.deviceBrand) }
        var model by remember { mutableStateOf(state.prefs.deviceModel) }
        var imei by remember { mutableStateOf(viewModel.decryptImei(state.prefs.deviceImeiEncrypted)) }
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SoftCharcoal,
            unfocusedBorderColor = SoftOutline,
            focusedContainerColor = SoftCreamSurface,
            unfocusedContainerColor = CreamFieldSurface,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
        )
        AlertDialog(
            onDismissRequest = { showVaultDialog = false },
            containerColor = SoftCreamSurface,
            shape = RoundedCornerShape(28.dp),
            title = { Text(if (bn) "ডিভাইস ও IMEI ভল্ট আপডেট" else "Device & Encrypted IMEI Vault", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (bn) "ফোনের বক্স বা *#06# থেকে প্রাপ্ত ১৫-ডিজিটের IMEI দিন।"
                        else "Enter your phone's 15-digit IMEI (*#06# or retail box). Luhn checksum is validated.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand (e.g. Samsung)", color = TextMuted) },
                        shape = RoundedCornerShape(20.dp),
                        colors = fieldColors,
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model (e.g. Galaxy A55)", color = TextMuted) },
                        shape = RoundedCornerShape(20.dp),
                        colors = fieldColors,
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = imei,
                        onValueChange = { imei = it.filter { ch -> ch.isDigit() }.take(15) },
                        label = { Text("15-digit IMEI", color = TextMuted) },
                        shape = RoundedCornerShape(20.dp),
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
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
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
            containerColor = SoftCreamSurface,
            shape = RoundedCornerShape(28.dp),
            title = { Text(if (bn) "সাইন আউট নিশ্চিত করুন" else "Sign Out?", color = TextPrimary) },
            text = { Text(if (bn) "লোকাল অফলাইন তথ্য ডিভাইসে সংরক্ষিত থাকবে।" else "Your offline incidents remain stored safely on this device.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        viewModel.signOut()
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmCoral, contentColor = OnCharcoal),
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
    badgeColor: Color,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = badgeColor,
            border = BorderStroke(1.dp, SoftOutline),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(19.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
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
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnCharcoal,
                checkedTrackColor = SageGreen,
                uncheckedThumbColor = SoftCharcoal,
                uncheckedTrackColor = OliveTrack,
                uncheckedBorderColor = SoftOutline,
            ),
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
            .background(WarmIvory),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = CircleShape,
                    color = SoftCreamSurface,
                    border = BorderStroke(1.dp, SoftOutline),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("checklist_back_btn")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = SoftCharcoal)
                    }
                }
                Text(
                    text = if (bn) "অ্যাকাউন্ট সুরক্ষা চেকলিস্ট" else "Protection Checklist",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                )
            }
        }

        item {
            OrganicEditorialCard(
                containerColor = ButterYellowSoft,
                blobColor = ButterYellow,
                accentShapeColor = SageGreen,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SoftCircularProgressRing(
                        progress = done.toFloat() / total.toFloat(),
                        valueText = "$done/$total",
                        captionText = "Steps",
                        size = 86.dp,
                        progressColor = SoftCharcoal,
                        trackColor = SoftCreamSurface,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (bn) "ধাপভিত্তিক সুরক্ষা অগ্রগতি" else "Recovery Progress",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (bn) "$done / $total ধাপ সম্পন্ন" else "$done of $total steps completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        items(state.checklist, key = { it.id }) { item ->
            GuardianCard(
                highlighted = !item.isCompleted,
                containerColor = if (item.isCompleted) SageGreenSoft.copy(alpha = 0.65f) else SoftCreamSurface,
                modifier = Modifier.testTag("checklist_item_${item.id}"),
            ) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = item.isCompleted,
                        onCheckedChange = { viewModel.toggleChecklist(item) },
                        modifier = Modifier.testTag("checklist_toggle_${item.id}"),
                        colors = CheckboxDefaults.colors(
                            checkedColor = SoftCharcoal,
                            checkmarkColor = OnCharcoal,
                            uncheckedColor = TextSecondary,
                        ),
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
                            StatusPill(
                                text = "Done ${fmt.format(Date(item.completedAtMillis))}",
                                color = SoftCharcoal,
                                backgroundColor = SageGreenSoft,
                            )
                        }
                        if (item.notes.isNotBlank()) {
                            Text("Note: ${item.notes}", style = MaterialTheme.typography.labelMedium, color = SoftCharcoal)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!item.officialUrl.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.officialUrl)))
                                    },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, SoftCharcoal.copy(alpha = 0.25f)),
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (bn) "পোর্টাল" else "Official", color = SoftCharcoal)
                                }
                            }
                            if (!item.officialPhone.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { dialTarget = item.officialPhone },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, SoftCharcoal.copy(alpha = 0.25f)),
                                ) {
                                    Icon(Icons.Outlined.Call, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(item.officialPhone, color = SoftCharcoal)
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
            containerColor = SoftCreamSurface,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Step Note", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Reference / GD / Operator Ticket #", color = TextMuted) },
                    minLines = 2,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftCharcoal,
                        unfocusedBorderColor = SoftOutline,
                        focusedContainerColor = SoftCreamSurface,
                        unfocusedContainerColor = CreamFieldSurface,
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
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { noteTarget = null }) { Text("Cancel", color = TextSecondary) } },
        )
    }

    dialTarget?.let { number ->
        AlertDialog(
            onDismissRequest = { dialTarget = null },
            containerColor = SoftCreamSurface,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Open Dialer for $number?", color = TextPrimary) },
            text = { Text("This opens your phone dialer without auto-calling.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        dialTarget = null
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
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
            .background(WarmIvory),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = SoftCreamSurface,
                        border = BorderStroke(1.dp, SoftOutline),
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("contacts_back_btn")) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = SoftCharcoal)
                        }
                    }
                    Text(if (bn) "বিশ্বস্ত পরিচিতি" else "Trusted Contacts", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                }
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_trusted_contact_btn"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (bn) "যোগ" else "Add")
                }
            }
        }

        if (state.contacts.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.Contacts,
                    title = if (bn) "কোনো বিশ্বস্ত পরিচিতি নেই" else "No Trusted Contacts Added",
                    description = if (bn) "পরিবারের সদস্য বা বিশ্বস্ত বন্ধুর নম্বর যোগ করুন (লোকাল এনক্রিপ্টেড)।"
                    else "Add a family member or trusted friend. Numbers are encrypted locally and shared ONLY when you explicitly confirm.",
                )
            }
        } else {
            items(state.contacts, key = { it.id }) { contact ->
                GuardianCard(containerColor = PowderBlueSoft) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(contact.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("${contact.relationshipLabel} • ${contact.phoneMasked}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (contact.notifySmsConsent) "Explicit SMS share consent saved" else "Share confirmation required per event",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftCharcoal,
                            )
                        }
                        IconButton(onClick = { viewModel.deleteTrustedContact(contact.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete contact", tint = WarmCoral)
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
                GuardianCard(containerColor = SoftCreamSurface) {
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
                            OutlinedButton(
                                onClick = { viewModel.revokeShare(log.id) },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, WarmCoral.copy(alpha = 0.4f)),
                            ) {
                                Text("Revoke", color = WarmCoral)
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
            focusedBorderColor = SoftCharcoal,
            unfocusedBorderColor = SoftOutline,
            focusedContainerColor = SoftCreamSurface,
            unfocusedContainerColor = CreamFieldSurface,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
        )
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = SoftCreamSurface,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Add Trusted Contact", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name", color = TextMuted) }, shape = RoundedCornerShape(20.dp), colors = fieldColors, singleLine = true)
                    OutlinedTextField(value = relation, onValueChange = { relation = it }, label = { Text("Relationship (e.g. Sister)", color = TextMuted) }, shape = RoundedCornerShape(20.dp), colors = fieldColors, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (+8801...)", color = TextMuted) }, shape = RoundedCornerShape(20.dp), colors = fieldColors, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = consent, onCheckedChange = { consent = it }, colors = CheckboxDefaults.colors(checkedColor = SoftCharcoal, checkmarkColor = OnCharcoal))
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
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
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
            .background(WarmIvory),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = CircleShape,
                    color = SoftCreamSurface,
                    border = BorderStroke(1.dp, SoftOutline),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("resources_back_btn")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = SoftCharcoal)
                    }
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
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary) },
                placeholder = { Text(if (bn) "নাম বা নম্বর দিয়ে খুঁজুন..." else "Search 999, bKash, GP, Online GD...", color = TextMuted) },
                modifier = Modifier.fillMaxWidth().testTag("resources_search_input"),
                singleLine = true,
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
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSel = selectedCategory == cat
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
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

        items(filtered, key = { it.id }) { res ->
            val dialNum = res.contactNumber.substringBefore('/').filter { it.isDigit() || it == '+' }
            val is999 = res.contactNumber == "999"
            GuardianCard(
                highlighted = is999,
                containerColor = if (is999) PastelPinkSoft else SoftCreamSurface,
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(text = res.category, color = SoftCharcoal, backgroundColor = PowderBlueSoft)
                    StatusPill(text = "Verified ${res.verificationDate}", color = SoftCharcoal, backgroundColor = SageGreenSoft)
                }
                Spacer(Modifier.height(10.dp))
                Text(res.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(res.address, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dialNum.isNotBlank()) {
                        Button(
                            onClick = { dialTarget = dialNum },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
                        ) {
                            Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(res.contactNumber)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(res.sourceUrl)))
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, SoftCharcoal.copy(alpha = 0.25f)),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Source", color = SoftCharcoal)
                    }
                }
            }
        }
    }

    dialTarget?.let { num ->
        AlertDialog(
            onDismissRequest = { dialTarget = null },
            containerColor = SoftCreamSurface,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Open Dialer for $num?", color = TextPrimary) },
            text = { Text("Guardian AI opens your device dialer so you can confirm before calling.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        dialTarget = null
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num")))
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
                ) { Text("Open Dialer") }
            },
            dismissButton = { TextButton(onClick = { dialTarget = null }) { Text("Cancel", color = TextSecondary) } },
        )
    }
}

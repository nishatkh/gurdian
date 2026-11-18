package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ButterYellow
import com.example.ui.theme.ButterYellowSoft
import com.example.ui.theme.CreamFieldSurface
import com.example.ui.theme.DottedStroke
import com.example.ui.theme.GuardianCard
import com.example.ui.theme.OnCharcoal
import com.example.ui.theme.OrganicWellnessBadge
import com.example.ui.theme.PastelPink
import com.example.ui.theme.PowderBlue
import com.example.ui.theme.PowderBlueSoft
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SageGreenSoft
import com.example.ui.theme.SoftCharcoal
import com.example.ui.theme.SoftCreamSurface
import com.example.ui.theme.SoftOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmIvory

private data class OnboardingPage(
    val icon: ImageVector,
    val titleEn: String,
    val titleBn: String,
    val bodyEn: String,
    val bodyBn: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color,
)

@Composable
fun OnboardingScreen(
    language: String,
    onLanguageChange: (String) -> Unit,
    onFinishOnboarding: () -> Unit,
) {
    val bn = language == "bn"
    var page by remember { mutableIntStateOf(0) }

    val pages = remember {
        listOf(
            OnboardingPage(
                icon = Icons.Outlined.Shield,
                titleEn = "Calm, Guided\nTheft & Loss Response",
                titleBn = "মোবাইল হারানো পরবর্তী\nশান্ত ও দ্রুত সুরক্ষা",
                bodyEn = "Guardian AI walks you gently through SIM blocking, bKash & Nagad protection, device lock steps, and Online GD preparation in English & Bangla.",
                bodyBn = "ফোন হারালে বা চুরি হলে সিম ব্লক, বিকাশ/নগদ সুরক্ষা এবং অনলাইন জিডি খসড়া তৈরিতে Guardian AI ধাপে ধাপে সহায়তা করে।",
                primaryColor = ButterYellow,
                secondaryColor = PastelPink,
                tertiaryColor = SageGreen,
            ),
            OnboardingPage(
                icon = Icons.Outlined.CloudOff,
                titleEn = "Offline-First &\nPrivate by Design",
                titleBn = "ইন্টারনেট ছাড়াই\nনিরাপদ সংরক্ষণ",
                bodyEn = "Log incidents, preserve timelines, and view verified Bangladesh helplines without internet. IMEIs stay encrypted inside Android Keystore.",
                bodyBn = "ইন্টারনেট না থাকলেও ঘটনা ও টাইমলাইন সংরক্ষণ করুন। আপনার IMEI অ্যান্ড্রয়েড কী-স্টোরে এনক্রিপ্টেড অবস্থায় থাকে।",
                primaryColor = PowderBlueSoft,
                secondaryColor = ButterYellow,
                tertiaryColor = PastelPink,
            ),
            OnboardingPage(
                icon = Icons.Outlined.Lock,
                titleEn = "Zero Unnecessary\nPermissions",
                titleBn = "শুধুমাত্র আপনার\nসম্মতিতে অনুমতি",
                bodyEn = "We never ask for storage or location during setup. Location is captured ONLY when you explicitly tap Capture Location in Emergency Mode.",
                bodyBn = "সেটআপের সময় অপ্রয়োজনীয় অনুমতি চাওয়া হয় না। শুধুমাত্র আপনি চাইলে জরুরি অবস্থায় লোকেশন নেওয়া হবে।",
                primaryColor = SageGreenSoft,
                secondaryColor = PowderBlue,
                tertiaryColor = ButterYellow,
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmIvory)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = ButterYellow,
                        border = BorderStroke(1.dp, SoftOutline),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Shield, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(20.dp))
                        }
                    }
                    Text("Guardian AI", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !bn,
                        onClick = { onLanguageChange("en") },
                        label = { Text("EN") },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftCharcoal,
                            selectedLabelColor = OnCharcoal,
                            containerColor = SoftCreamSurface,
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
                        onClick = { onLanguageChange("bn") },
                        label = { Text("বাংলা") },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftCharcoal,
                            selectedLabelColor = OnCharcoal,
                            containerColor = SoftCreamSurface,
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                AnimatedContent(targetState = page, label = "onboarding_slide") { idx ->
                    val item = pages[idx]
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        OrganicWellnessBadge(
                            icon = item.icon,
                            primaryBlobColor = item.primaryColor,
                            secondaryBlobColor = item.secondaryColor,
                            tertiaryCircleColor = item.tertiaryColor,
                            size = 104.dp,
                        )
                        Spacer(Modifier.height(22.dp))
                        Text(
                            text = if (bn) item.titleBn else item.titleEn,
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (bn) item.bodyBn else item.bodyEn,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(20.dp))
                        GuardianCard(dottedBorder = true, containerColor = SoftCreamSurface) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = CircleShape,
                                    color = ButterYellowSoft,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Description, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text(
                                    text = if (bn) "সহায়ক প্ল্যাটফর্ম — বাংলাদেশ পুলিশ বা ৯৯৯-এর বিকল্প নয়"
                                    else "Response Companion — Does not replace 999, Online GD, or BTRC.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { idx ->
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .weight(if (idx == page) 2.2f else 1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (idx == page) SoftCharcoal else DottedStroke),
                        )
                    }
                }
                Button(
                    onClick = {
                        if (page < pages.lastIndex) page++ else onFinishOnboarding()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_next_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCharcoal, contentColor = OnCharcoal),
                ) {
                    Text(
                        text = when {
                            page < pages.lastIndex -> if (bn) "পরবর্তী" else "Continue"
                            else -> if (bn) "শুরু করুন" else "Get Started"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun AuthScreen(
    language: String,
    isBusy: Boolean,
    bannerMessage: String?,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String?, String) -> Unit,
    onOfflineWorkspace: (String) -> Unit,
) {
    val bn = language == "bn"
    var isRegisterMode by remember { mutableStateOf(false) }
    var showForgotMsg by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SoftCharcoal,
        unfocusedBorderColor = SoftOutline,
        focusedContainerColor = SoftCreamSurface,
        unfocusedContainerColor = CreamFieldSurface,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmIvory)
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OrganicWellnessBadge(
                    icon = Icons.Outlined.Shield,
                    primaryBlobColor = ButterYellow,
                    secondaryBlobColor = PastelPink,
                    tertiaryCircleColor = PowderBlue,
                    size = 72.dp,
                )
                Column {
                    Text(
                        text = if (isRegisterMode) (if (bn) "অ্যাকাউন্ট তৈরি করুন" else "Create Account")
                        else (if (bn) "নিরাপদ সাইন ইন" else "Welcome Back"),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = if (bn) "সার্ভার সিঙ্ক বা অফলাইন লোকাল মোড" else "Sync safely or start in Offline Local Mode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (bannerMessage != null) {
                GuardianCard(containerColor = ButterYellowSoft) {
                    Text(text = bannerMessage, style = MaterialTheme.typography.bodyMedium, color = SoftCharcoal)
                }
            }

            if (isRegisterMode) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(if (bn) "পূর্ণ নাম" else "Full Name", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().testTag("auth_name_input"),
                    shape = RoundedCornerShape(22.dp),
                    colors = fieldColors,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (bn) "মোবাইল নম্বর (ঐচ্ছিক)" else "Phone (optional)", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = fieldColors,
                    singleLine = true,
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(if (bn) "ইমেইল" else "Email Address", color = TextMuted) },
                modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                shape = RoundedCornerShape(22.dp),
                colors = fieldColors,
                singleLine = true,
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (bn) "পাসওয়ার্ড" else "Password (min 8 chars)", color = TextMuted) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                shape = RoundedCornerShape(22.dp),
                colors = fieldColors,
                singleLine = true,
            )

            if (showForgotMsg) {
                GuardianCard(dottedBorder = true) {
                    Text(
                        text = if (bn) "পাসওয়ার্ড রিসেট অনুরোধের জন্য আপনার ইমেইল দিন এবং আপনার অ্যাকাউন্ট প্রশাসকের সাথে যোগাযোগ করুন বা অফলাইন মোড ব্যবহার করুন।"
                        else "Password recovery requests are logged on your configured Guardian AI server. You can continue using Local Offline Mode anytime.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            Button(
                onClick = {
                    if (isRegisterMode) {
                        onRegister(fullName.ifBlank { "Guardian User" }, email, password, phone.ifBlank { null }, language)
                    } else {
                        onLogin(email, password)
                    }
                },
                enabled = !isBusy && email.isNotBlank() && password.length >= 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("auth_submit_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftCharcoal,
                    contentColor = OnCharcoal,
                    disabledContainerColor = SoftCharcoal.copy(alpha = 0.25f),
                    disabledContentColor = OnCharcoal,
                ),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnCharcoal, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (isRegisterMode) (if (bn) "নিবন্ধন করুন" else "Register") else (if (bn) "সাইন ইন" else "Sign In"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                    Text(
                        text = if (isRegisterMode) (if (bn) "ইতিমধ্যে অ্যাকাউন্ট আছে?" else "Have an account? Sign in")
                        else (if (bn) "নতুন অ্যাকাউন্ট খুলুন" else "Create new account"),
                        color = SoftCharcoal,
                    )
                }
                TextButton(onClick = { showForgotMsg = !showForgotMsg }) {
                    Text(if (bn) "পাসওয়ার্ড ভুলে গেছেন?" else "Forgot password?", color = TextSecondary)
                }
            }

            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = { onOfflineWorkspace(fullName.ifBlank { "Guardian User" }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("offline_workspace_button"),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.2.dp, SoftCharcoal.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = ButterYellowSoft),
            ) {
                Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = SoftCharcoal, modifier = Modifier.size(18.dp))
                Text(
                    text = if (bn) "  অফলাইন লোকাল মোডে প্রবেশ করুন" else "  Continue in Offline-First Mode",
                    color = SoftCharcoal,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

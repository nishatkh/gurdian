package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AccountProtectionScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EmergencyScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IncidentDetailScreen
import com.example.ui.screens.IncidentsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ResourcesDirectoryScreen
import com.example.ui.screens.TrustedContactsScreen
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepOlive
import com.example.ui.theme.GuardianAITheme
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.GuardianViewModel

object GuardianRoutes {
    const val HOME = "home"
    const val INCIDENTS = "incidents"
    const val EMERGENCY = "emergency"
    const val ACTIVITY = "activity"
    const val PROFILE = "profile"
    const val ASSISTANT = "assistant"
    const val CHECKLIST = "checklist"
    const val CONTACTS = "contacts"
    const val RESOURCES = "resources"
    const val INCIDENT_DETAIL = "incident_detail/{incidentId}"

    fun incidentDetail(id: String) = "incident_detail/$id"
}

private data class NavTabItem(
    val route: String,
    val labelEn: String,
    val labelBn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isEmergency: Boolean = false,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuardianAITheme {
                GuardianAppRoot()
            }
        }
    }
}

@Composable
fun GuardianAppRoot(
    viewModel: GuardianViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val banner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(banner) {
        if (!banner.isNullOrBlank()) {
            snackbarHostState.showSnackbar(banner!!)
            viewModel.clearBanner()
        }
    }

    if (!state.prefs.onboardingDone) {
        OnboardingScreen(
            language = state.prefs.language,
            onLanguageChange = { viewModel.toggleLanguage(it) },
            onFinishOnboarding = { viewModel.completeOnboarding() },
        )
        return
    }

    if (!state.prefs.isLoggedIn) {
        AuthScreen(
            language = state.prefs.language,
            isBusy = isBusy,
            bannerMessage = banner,
            onLogin = { e, p -> viewModel.login(e, p) },
            onRegister = { n, e, p, ph, l -> viewModel.register(n, e, p, ph, l) },
            onOfflineWorkspace = { name -> viewModel.enterOfflineMode(name) },
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: GuardianRoutes.HOME
    val bn = state.prefs.language == "bn"

    val tabs = remember {
        listOf(
            NavTabItem(GuardianRoutes.HOME, "Home", "হোম", Icons.Filled.Home, Icons.Outlined.Home),
            NavTabItem(GuardianRoutes.INCIDENTS, "Incidents", "ঘটনা", Icons.Filled.Report, Icons.Outlined.Report),
            NavTabItem(GuardianRoutes.EMERGENCY, "Emergency", "জরুরি", Icons.Filled.GppMaybe, Icons.Outlined.GppMaybe, isEmergency = true),
            NavTabItem(GuardianRoutes.ACTIVITY, "Activity", "কার্যক্রম", Icons.Filled.History, Icons.Outlined.History),
            NavTabItem(GuardianRoutes.PROFILE, "Profile", "প্রোফাইল", Icons.Filled.Person, Icons.Outlined.Person),
        )
    }
    val topRoutes = remember { tabs.map { it.route }.toSet() }
    val showTopChrome = currentRoute in topRoutes

    fun navigateTop(route: String) {
        navController.navigate(route) {
            popUpTo(GuardianRoutes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
    ) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBlack,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = CardSurface,
                        contentColor = LimeAccent,
                    )
                }
            },
            bottomBar = {
                if (showTopChrome && !useRail) {
                    NavigationBar(
                        containerColor = CharcoalSurface,
                        contentColor = TextPrimary,
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTop(tab.route) },
                                modifier = Modifier.testTag("nav_${tab.route}"),
                                icon = {
                                    if (tab.isEmergency) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (selected) LimeAccent else CrimsonAlert.copy(alpha = 0.22f))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                        ) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.labelEn,
                                                tint = if (selected) ObsidianBlack else CrimsonAlert,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.labelEn,
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (bn) tab.labelBn else tab.labelEn,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = LimeAccent,
                                    selectedTextColor = LimeAccent,
                                    indicatorColor = DeepOlive,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (showTopChrome && useRail) {
                    NavigationRail(
                        containerColor = CharcoalSurface,
                        contentColor = TextPrimary,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTop(tab.route) },
                                modifier = Modifier.testTag("nav_${tab.route}"),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.labelEn,
                                        tint = when {
                                            tab.isEmergency && !selected -> CrimsonAlert
                                            selected -> LimeAccent
                                            else -> TextSecondary
                                        },
                                    )
                                },
                                label = { Text(if (bn) tab.labelBn else tab.labelEn) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = LimeAccent,
                                    selectedTextColor = LimeAccent,
                                    indicatorColor = DeepOlive,
                                ),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = GuardianRoutes.HOME,
                        ) {
                            composable(GuardianRoutes.HOME) {
                                HomeScreen(
                                    state = state,
                                    onOpenEmergency = { navigateTop(GuardianRoutes.EMERGENCY) },
                                    onOpenIncident = { id -> navController.navigate(GuardianRoutes.incidentDetail(id)) },
                                    onOpenChecklist = { navController.navigate(GuardianRoutes.CHECKLIST) },
                                    onOpenContacts = { navController.navigate(GuardianRoutes.CONTACTS) },
                                    onOpenResources = { navController.navigate(GuardianRoutes.RESOURCES) },
                                    onOpenAssistant = { navController.navigate(GuardianRoutes.ASSISTANT) },
                                    onSyncNow = { viewModel.triggerSyncNow() },
                                )
                            }
                            composable(GuardianRoutes.INCIDENTS) {
                                IncidentsScreen(
                                    state = state,
                                    onCreateIncident = { navigateTop(GuardianRoutes.EMERGENCY) },
                                    onSelectIncident = { id -> navController.navigate(GuardianRoutes.incidentDetail(id)) },
                                )
                            }
                            composable(GuardianRoutes.EMERGENCY) {
                                EmergencyScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    onIncidentCreated = { id -> navController.navigate(GuardianRoutes.incidentDetail(id)) },
                                )
                            }
                            composable(GuardianRoutes.ACTIVITY) {
                                ActivityScreen(
                                    state = state,
                                    onSyncNow = { viewModel.triggerSyncNow() },
                                    onOpenAssistant = { navController.navigate(GuardianRoutes.ASSISTANT) },
                                )
                            }
                            composable(GuardianRoutes.PROFILE) {
                                ProfileScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    onOpenChecklist = { navController.navigate(GuardianRoutes.CHECKLIST) },
                                    onOpenContacts = { navController.navigate(GuardianRoutes.CONTACTS) },
                                    onOpenResources = { navController.navigate(GuardianRoutes.RESOURCES) },
                                )
                            }
                            composable(
                                route = GuardianRoutes.INCIDENT_DETAIL,
                                arguments = listOf(navArgument("incidentId") { type = NavType.StringType }),
                            ) { entry ->
                                val id = entry.arguments?.getString("incidentId") ?: ""
                                IncidentDetailScreen(
                                    incidentId = id,
                                    viewModel = viewModel,
                                    bn = bn,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                            composable(GuardianRoutes.ASSISTANT) {
                                AssistantScreen(
                                    state = state,
                                    isBusy = isBusy,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                            composable(GuardianRoutes.CHECKLIST) {
                                AccountProtectionScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                            composable(GuardianRoutes.CONTACTS) {
                                TrustedContactsScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                            composable(GuardianRoutes.RESOURCES) {
                                ResourcesDirectoryScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

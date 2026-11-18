package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.ButterYellow
import com.example.ui.theme.GuardianAITheme
import com.example.ui.theme.OnCharcoal
import com.example.ui.theme.PastelPink
import com.example.ui.theme.SoftCharcoal
import com.example.ui.theme.SoftShadow
import com.example.ui.theme.WarmIvory
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
    val icon: ImageVector,
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
            NavTabItem(GuardianRoutes.HOME, "Home", "হোম", Icons.Outlined.Home),
            NavTabItem(GuardianRoutes.INCIDENTS, "Incidents", "ঘটনা", Icons.Outlined.Report),
            NavTabItem(GuardianRoutes.EMERGENCY, "Emergency", "জরুরি", Icons.Outlined.GppMaybe, isEmergency = true),
            NavTabItem(GuardianRoutes.ACTIVITY, "Activity", "কার্যক্রম", Icons.Outlined.History),
            NavTabItem(GuardianRoutes.PROFILE, "Profile", "প্রোফাইল", Icons.Outlined.Person),
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
            .background(WarmIvory),
    ) {
        val useRail = maxWidth >= 720.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = WarmIvory,
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(bottom = if (showTopChrome && !useRail) 96.dp else 0.dp),
                    ) { data ->
                        Surface(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = SoftCharcoal,
                        ) {
                            Snackbar(
                                snackbarData = data,
                                containerColor = Color.Transparent,
                                contentColor = OnCharcoal,
                                actionColor = ButterYellow,
                            )
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
                            containerColor = SoftCharcoal,
                            contentColor = OnCharcoal,
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
                                            imageVector = tab.icon,
                                            contentDescription = tab.labelEn,
                                            tint = when {
                                                selected -> SoftCharcoal
                                                tab.isEmergency -> PastelPink
                                                else -> OnCharcoal.copy(alpha = 0.72f)
                                            },
                                        )
                                    },
                                    label = { Text(if (bn) tab.labelBn else tab.labelEn) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = SoftCharcoal,
                                        selectedTextColor = ButterYellow,
                                        indicatorColor = if (tab.isEmergency) PastelPink else ButterYellow,
                                        unselectedIconColor = OnCharcoal.copy(alpha = 0.72f),
                                        unselectedTextColor = OnCharcoal.copy(alpha = 0.72f),
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

            if (showTopChrome && !useRail) {
                WellnessFloatingCharcoalDock(
                    tabs = tabs,
                    currentRoute = currentRoute,
                    bn = bn,
                    onSelectTab = { navigateTop(it) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }
    }
}

/**
 * Floating dark charcoal navigation bar with thin-line icons, evenly spaced items,
 * and a prominent circular center Emergency FAB wrapped in a warm cream halo.
 */
@Composable
private fun WellnessFloatingCharcoalDock(
    tabs: List<NavTabItem>,
    currentRoute: String,
    bn: Boolean,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leftTabs = tabs.take(2)
    val centerTab = tabs[2]
    val rightTabs = tabs.drop(3)
    val dockShape = RoundedCornerShape(36.dp)

    Box(
        modifier = modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .shadow(elevation = 4.dp, shape = dockShape, ambientColor = SoftShadow, spotColor = SoftShadow),
            shape = dockShape,
            color = SoftCharcoal,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                leftTabs.forEach { tab ->
                    WellnessNavBarItem(
                        tab = tab,
                        isSelected = currentRoute == tab.route,
                        bn = bn,
                        onClick = { onSelectTab(tab.route) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.width(72.dp))

                rightTabs.forEach { tab ->
                    WellnessNavBarItem(
                        tab = tab,
                        isSelected = currentRoute == tab.route,
                        bn = bn,
                        onClick = { onSelectTab(tab.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Prominent circular Floating Action Button positioned in the center
        val isEmergencySelected = currentRoute == centerTab.route
        val centerScale by animateFloatAsState(
            targetValue = if (isEmergencySelected) 1.07f else 1.0f,
            animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
            label = "center_fab_scale",
        )
        val centerBg by animateColorAsState(
            targetValue = if (isEmergencySelected) PastelPink else ButterYellow,
            animationSpec = spring(),
            label = "center_fab_bg",
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp)
                .size(64.dp)
                .scale(centerScale)
                .semantics { selected = isEmergencySelected }
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = SoftCharcoal),
                    role = Role.Tab,
                    onClick = { onSelectTab(centerTab.route) },
                )
                .testTag("nav_${centerTab.route}"),
            shape = CircleShape,
            color = centerBg,
            border = BorderStroke(4.dp, WarmIvory),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = centerTab.icon,
                    contentDescription = centerTab.labelEn,
                    tint = SoftCharcoal,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun WellnessNavBarItem(
    tab: NavTabItem,
    isSelected: Boolean,
    bn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (isSelected) ButterYellow else OnCharcoal.copy(alpha = 0.65f),
        animationSpec = spring(),
        label = "nav_item_tint_${tab.route}",
    )
    Column(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(24.dp))
            .semantics { selected = isSelected }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = ButterYellow),
                role = Role.Tab,
                onClick = onClick,
            )
            .testTag("nav_${tab.route}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.labelEn,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = if (bn) tab.labelBn else tab.labelEn,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
            maxLines = 1,
            softWrap = false,
        )
    }
}

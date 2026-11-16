package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.GuardianDatabase
import com.example.data.local.IncidentEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.GuardianRepository
import com.example.domain.LocalSecurityEngine
import com.example.security.KeystoreCryptoManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appNameMatchesGuardianAI() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Guardian AI", appName)
    }

    @Test
    fun imeiLuhnValidationMaskingAndRoundTripWork() {
        val validImei = "356938035643809" // Luhn-valid 15-digit IMEI
        val invalidImei = "356938035643800"
        assertTrue(KeystoreCryptoManager.isValidLuhnImei(validImei))
        assertFalse(KeystoreCryptoManager.isValidLuhnImei(invalidImei))
        assertEquals("•••• •••• •••• 3809", KeystoreCryptoManager.maskImei(validImei))

        val encrypted = KeystoreCryptoManager.encrypt(validImei)
        assertNotEquals(validImei, encrypted)
        assertEquals(validImei, KeystoreCryptoManager.decrypt(encrypted))
    }

    @Test
    fun localRagGroundsBangladeshQueriesAndRefusesOutOfScope() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = LocalSecurityEngine(context)

        val enResult = engine.answerOfflineQuery(
            query = "How do I file Bangladesh Police Online GD for a stolen phone?",
            language = "en",
            activeIncident = null,
            checklist = emptyList(),
        )
        assertTrue(enResult.grounded)
        assertTrue(enResult.citations.isNotEmpty())

        val bnResult = engine.answerOfflineQuery(
            query = "সিম ব্লক ও বিকাশ অ্যাকাউন্ট কীভাবে সুরক্ষিত করব?",
            language = "bn",
            activeIncident = null,
            checklist = emptyList(),
        )
        assertTrue(bnResult.grounded)
        assertTrue(bnResult.citations.isNotEmpty())

        val oosResult = engine.answerOfflineQuery(
            query = "How do I bake a chocolate croissant?",
            language = "en",
            activeIncident = null,
            checklist = emptyList(),
        )
        assertFalse(oosResult.grounded)
        assertTrue(oosResult.answer.contains("Insufficient verified evidence"))
    }

    @Test
    fun explainableAssessmentAndReportDraftEnforceTransparency() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = LocalSecurityEngine(context)
        val checklist = GuardianDatabase.defaultChecklistSeed().mapIndexed { i, item ->
            if (i < 2) item.copy(isCompleted = true, completedAtMillis = 1_700_000_000_000L) else item
        }
        val incident = IncidentEntity(
            id = "inc-test-001",
            category = "suspected_theft",
            status = "active",
            title = "Phone stolen near Farmgate",
            description = "My Samsung Galaxy A55 was pickpocketed while boarding a bus at Farmgate.",
            occurredAtMillis = 1_700_000_000_000L,
            deviceBrand = "Samsung",
            deviceModel = "Galaxy A55",
            imeiEncrypted = KeystoreCryptoManager.encrypt("356938035643809"),
            imeiLast4 = "3809",
            locationLabel = "Farmgate, Dhaka",
        )

        val assessment = engine.assessIncident(incident, checklist, eventCount = 1, bn = false)
        assertEquals(100, assessment.readinessScore)
        assertTrue(assessment.missingInformation.isEmpty())
        assertTrue(assessment.uncertaintyNotice.contains("never declares a phone stolen"))

        val draft = engine.buildReportDraft(incident, checklist, bn = false)
        assertTrue(draft.contains("DRAFT ONLY — NOT AN OFFICIALLY SUBMITTED GENERAL DIARY"))
        assertTrue(draft.contains("•••• •••• •••• 3809"))
        assertFalse(draft.contains("356938035643809"))
    }

    @Test
    fun roomOfflineFirstIncidentOutboxAndChecklistWork() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GuardianDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefsRepo = UserPreferencesRepository(context)
        val repo = GuardianRepository(context, db, prefsRepo)

        try {
            repo.ensureSeeded()
            assertEquals(9, repo.checklistFlow.first().size)
            assertEquals(11, repo.resourcesFlow.first().size)

            val incidentId = repo.createIncidentOfflineFirst(
                category = "suspected_theft",
                title = "Robbery at Mirpur 10",
                description = "Phone snatched near Mirpur 10 metro station around 8 PM.",
                deviceBrand = "Xiaomi",
                deviceModel = "Redmi Note 13",
                rawImei = "356938035643809",
                locationLabel = "Mirpur 10, Dhaka",
                latitude = null,
                longitude = null,
                accuracy = null,
                locationConsent = false,
            )

            val incidents = repo.incidentsFlow.first()
            assertEquals(1, incidents.size)
            assertEquals("pending", incidents.first().syncStatus)
            assertEquals("3809", incidents.first().imeiLast4)

            val outbox = repo.outboxFlow.first()
            assertTrue(outbox.any { it.entityType == "incident_upsert" })

            repo.transitionIncidentStatus(incidentId, "submitted_for_review")
            assertEquals("submitted_for_review", db.incidentDao().getIncidentById(incidentId)?.status)

            val simItem = repo.checklistFlow.first().first()
            repo.toggleChecklistItem(simItem)
            assertTrue(repo.checklistFlow.first().first().isCompleted)
        } finally {
            db.close()
        }
    }

    @Test
    fun composeOnboardingOfflineModeAndBottomNavigationCuj() {
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithTag("onboarding_next_button").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag("offline_workspace_button").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty()
        }

        if (composeRule.onAllNodesWithTag("onboarding_next_button").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("onboarding_next_button").performClick()
            composeRule.onNodeWithTag("onboarding_next_button").performClick()
            composeRule.onNodeWithTag("onboarding_next_button").performClick()
            composeRule.waitUntil(5000) {
                composeRule.onAllNodesWithTag("offline_workspace_button").fetchSemanticsNodes().isNotEmpty() ||
                    composeRule.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty()
            }
        }

        if (composeRule.onAllNodesWithTag("offline_workspace_button").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("offline_workspace_button").performScrollTo().performClick()
            composeRule.waitUntil(5000) {
                composeRule.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty()
            }
        }

        // Verify 5-tab navigation shell is active on Home
        composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_incidents").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Incident Records").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_emergency").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("emergency_list").performScrollToNode(hasTestTag("activate_theft_mode_button"))
        composeRule.onNodeWithTag("activate_theft_mode_button").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("nav_activity").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sync_now_button").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_profile").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("profile_checklist_row"))
        composeRule.onNodeWithTag("profile_checklist_row").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("checklist_back_btn").assertIsDisplayed()
    }
}

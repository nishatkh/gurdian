package com.example.data.repository

import android.content.Context
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChecklistItemEntity
import com.example.data.local.ContactShareLogEntity
import com.example.data.local.EmergencyResourceEntity
import com.example.data.local.GuardianDatabase
import com.example.data.local.IncidentEntity
import com.example.data.local.IncidentEventEntity
import com.example.data.local.IncidentEvidenceEntity
import com.example.data.local.SyncOutboxEntity
import com.example.data.local.TrustedContactEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSessionPrefs
import com.example.data.remote.AiChatReq
import com.example.data.remote.AuthLoginReq
import com.example.data.remote.AuthRegisterReq
import com.example.data.remote.GuardianApiService
import com.example.data.remote.ReportDraftReq
import com.example.data.sync.IncidentSyncWorker
import com.example.domain.LocalSecurityEngine
import com.example.security.KeystoreCryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.util.UUID

class GuardianRepository(
    private val context: Context,
    private val db: GuardianDatabase,
    val prefsRepo: UserPreferencesRepository,
    private val api: GuardianApiService = GuardianApiService.create(),
) {
    val localEngine = LocalSecurityEngine(context)

    val prefsFlow: Flow<UserSessionPrefs> = prefsRepo.prefsFlow
    val incidentsFlow: Flow<List<IncidentEntity>> = db.incidentDao().observeAllIncidents()
    val recentEventsFlow: Flow<List<IncidentEventEntity>> = db.incidentDao().observeRecentEvents()
    val checklistFlow: Flow<List<ChecklistItemEntity>> = db.checklistDao().observeAllItems()
    val contactsFlow: Flow<List<TrustedContactEntity>> = db.trustedContactDao().observeContacts()
    val shareLogsFlow: Flow<List<ContactShareLogEntity>> = db.trustedContactDao().observeShareLogs()
    val resourcesFlow: Flow<List<EmergencyResourceEntity>> = db.resourceDao().observeResources()
    val outboxFlow: Flow<List<SyncOutboxEntity>> = db.syncOutboxDao().observeOutbox()
    val chatMessagesFlow: Flow<List<ChatMessageEntity>> = db.chatDao().observeMessages()

    fun observeIncident(id: String): Flow<IncidentEntity?> = db.incidentDao().observeIncident(id)
    fun observeEvents(incidentId: String): Flow<List<IncidentEventEntity>> = db.incidentDao().observeEventsForIncident(incidentId)
    fun observeEvidence(incidentId: String): Flow<List<IncidentEvidenceEntity>> = db.incidentDao().observeEvidenceForIncident(incidentId)

    suspend fun ensureSeeded() {
        if (db.checklistDao().countItems() == 0) {
            db.checklistDao().insertAll(GuardianDatabase.defaultChecklistSeed())
        }
        if (db.resourceDao().countResources() == 0) {
            db.resourceDao().insertAll(GuardianDatabase.defaultResourcesSeed())
        }
    }

    suspend fun loginRemoteOrError(email: String, password: String): Result<Unit> {
        return try {
            val resp = api.login(AuthLoginReq(email.trim(), password))
            if (resp.isSuccessful && resp.body() != null) {
                val b = resp.body()!!
                prefsRepo.setSession(b.fullName, b.email, b.accessToken, b.refreshToken, offlineOnly = false)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Server login failed (${resp.code()}) — verify credentials or continue Offline Mode."))
            }
        } catch (ex: Exception) {
            Result.failure(Exception("Backend unreachable (${ex.message}). You can use Local Offline Mode safely."))
        }
    }

    suspend fun registerRemoteOrOffline(
        fullName: String,
        email: String,
        password: String,
        phone: String?,
        language: String,
    ): Result<String> {
        return try {
            val resp = api.register(AuthRegisterReq(email.trim(), password, fullName.trim(), phone, language))
            if (resp.isSuccessful && resp.body() != null) {
                val b = resp.body()!!
                prefsRepo.setSession(b.fullName, b.email, b.accessToken, b.refreshToken, offlineOnly = false)
                Result.success("Registered with backend")
            } else {
                Result.failure(Exception("Registration error (${resp.code()})"))
            }
        } catch (_: Exception) {
            // Allow graceful local-first workspace if backend server isn't running
            prefsRepo.setSession(fullName.ifBlank { "Guardian User" }, email, "", "", offlineOnly = true)
            Result.success("Offline local workspace created")
        }
    }

    suspend fun activateOfflineWorkspace(name: String) {
        prefsRepo.setSession(name.ifBlank { "Guardian User" }, "offline@local", "", "", offlineOnly = true)
    }

    suspend fun createIncidentOfflineFirst(
        category: String,
        title: String,
        description: String,
        deviceBrand: String,
        deviceModel: String,
        rawImei: String,
        locationLabel: String,
        latitude: Double?,
        longitude: Double?,
        accuracy: Float?,
        locationConsent: Boolean,
        status: String = "active",
    ): String {
        val now = System.currentTimeMillis()
        val cleanImei = rawImei.filter { it.isDigit() }
        val encryptedImei = if (cleanImei.isNotBlank()) KeystoreCryptoManager.encrypt(cleanImei) else ""
        val last4 = if (cleanImei.length >= 4) cleanImei.takeLast(4) else ""
        val id = UUID.randomUUID().toString()

        val inc = IncidentEntity(
            id = id,
            category = category,
            status = status,
            title = title.trim(),
            description = description.trim(),
            occurredAtMillis = now,
            deviceBrand = deviceBrand.trim(),
            deviceModel = deviceModel.trim(),
            imeiEncrypted = encryptedImei,
            imeiLast4 = last4,
            locationLabel = locationLabel.trim(),
            latitude = if (locationConsent) latitude else null,
            longitude = if (locationConsent) longitude else null,
            locationAccuracyMeters = if (locationConsent) accuracy else null,
            locationConsentGranted = locationConsent,
            syncStatus = "pending",
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        db.incidentDao().upsertIncident(inc)
        db.incidentDao().insertEvent(
            IncidentEventEntity(
                incidentId = id,
                eventType = "incident_created",
                note = "Incident logged locally (${category.replace('_', ' ')})",
                eventTimestampMillis = now,
            )
        )

        val payload = JSONObject().apply {
            put("client_incident_id", id)
            put("category", category)
            put("status", status)
            put("title", inc.title)
            put("description", inc.description)
            put("device_brand", inc.deviceBrand)
            put("device_model", inc.deviceModel)
            if (cleanImei.isNotBlank()) put("imei", cleanImei)
            put("location_label", inc.locationLabel)
            put("consent_granted", locationConsent)
            if (accuracy != null) put("accuracy_meters", accuracy.toDouble())
        }
        db.syncOutboxDao().enqueue(
            SyncOutboxEntity(
                clientEventId = "inc-$id",
                entityType = "incident_upsert",
                payloadJson = payload.toString(),
                originalTimestampMillis = now,
            )
        )
        IncidentSyncWorker.enqueue(context)
        return id
    }

    suspend fun addTimelineObservation(incidentId: String, note: String, eventType: String = "observation") {
        val now = System.currentTimeMillis()
        val evId = UUID.randomUUID().toString()
        db.incidentDao().insertEvent(
            IncidentEventEntity(
                id = evId,
                incidentId = incidentId,
                eventType = eventType,
                note = note.trim(),
                eventTimestampMillis = now,
            )
        )
        val payload = JSONObject().apply {
            put("client_incident_id", incidentId)
            put("event_type", eventType)
            put("note", note.trim())
        }
        db.syncOutboxDao().enqueue(
            SyncOutboxEntity(
                clientEventId = evId,
                entityType = "incident_event",
                payloadJson = payload.toString(),
                originalTimestampMillis = now,
            )
        )
        IncidentSyncWorker.enqueue(context)
    }

    suspend fun transitionIncidentStatus(incidentId: String, nextStatus: String) {
        val current = db.incidentDao().getIncidentById(incidentId) ?: return
        val allowed = mapOf(
            "draft" to setOf("active", "submitted_for_review", "cancelled"),
            "active" to setOf("submitted_for_review", "closed", "cancelled"),
            "submitted_for_review" to setOf("additional_info_requested", "cancelled"),
            "additional_info_requested" to setOf("submitted_for_review", "cancelled"),
        )
        if (nextStatus !in (allowed[current.status] ?: emptySet())) return
        val now = System.currentTimeMillis()
        db.incidentDao().upsertIncident(current.copy(status = nextStatus, updatedAtMillis = now, syncStatus = "pending"))
        addTimelineObservation(incidentId, "Status changed from ${current.status} to $nextStatus", "status_changed")
    }

    suspend fun updateIncidentDetails(
        incidentId: String,
        description: String,
        locationLabel: String,
        deviceBrand: String,
        deviceModel: String,
        rawImei: String?,
    ) {
        val current = db.incidentDao().getIncidentById(incidentId) ?: return
        val cleanImei = rawImei?.filter { it.isDigit() } ?: ""
        val enc = if (cleanImei.isNotBlank()) KeystoreCryptoManager.encrypt(cleanImei) else current.imeiEncrypted
        val last4 = if (cleanImei.length >= 4) cleanImei.takeLast(4) else current.imeiLast4
        db.incidentDao().upsertIncident(
            current.copy(
                description = description.trim(),
                locationLabel = locationLabel.trim(),
                deviceBrand = deviceBrand.trim(),
                deviceModel = deviceModel.trim(),
                imeiEncrypted = enc,
                imeiLast4 = last4,
                updatedAtMillis = System.currentTimeMillis(),
                syncStatus = "pending",
            )
        )
        addTimelineObservation(incidentId, "Incident details updated locally", "details_updated")
    }

    suspend fun attachEvidenceUri(incidentId: String, uriString: String, displayName: String, notes: String) {
        db.incidentDao().insertEvidence(
            IncidentEvidenceEntity(
                incidentId = incidentId,
                uriString = uriString,
                displayName = displayName,
                notes = notes,
            )
        )
        addTimelineObservation(incidentId, "Evidence attached: $displayName", "evidence_added")
    }

    suspend fun toggleChecklistItem(item: ChecklistItemEntity) {
        val now = System.currentTimeMillis()
        val next = !item.isCompleted
        db.checklistDao().updateItem(
            item.copy(
                isCompleted = next,
                completedAtMillis = if (next) now else null,
            )
        )
        val payload = JSONObject().apply {
            put("category", item.category)
            put("is_completed", next)
            put("notes", item.notes)
        }
        db.syncOutboxDao().enqueue(
            SyncOutboxEntity(
                clientEventId = "chk-${item.category}-$now",
                entityType = "checklist_toggle",
                payloadJson = payload.toString(),
                originalTimestampMillis = now,
            )
        )
        IncidentSyncWorker.enqueue(context)
    }

    suspend fun updateChecklistNotes(item: ChecklistItemEntity, notes: String) {
        db.checklistDao().updateItem(item.copy(notes = notes))
    }

    suspend fun addTrustedContact(name: String, relation: String, rawPhone: String, smsConsent: Boolean) {
        val clean = rawPhone.trim()
        val masked = "*".repeat((clean.length - 4).coerceAtLeast(0)) + clean.takeLast(4)
        db.trustedContactDao().insertContact(
            TrustedContactEntity(
                name = name.trim(),
                relationshipLabel = relation.trim(),
                phoneEncrypted = KeystoreCryptoManager.encrypt(clean),
                phoneMasked = masked,
                notifySmsConsent = smsConsent,
            )
        )
    }

    suspend fun deleteTrustedContact(id: String) = db.trustedContactDao().deleteContact(id)

    suspend fun logContactShare(incidentId: String, contact: TrustedContactEntity, includeLocation: Boolean) {
        db.trustedContactDao().insertShareLog(
            ContactShareLogEntity(
                incidentId = incidentId,
                contactId = contact.id,
                contactName = contact.name,
                includeLocation = includeLocation,
            )
        )
        addTimelineObservation(incidentId, "Shared incident summary with ${contact.name} (consent confirmed)", "contact_shared")
    }

    suspend fun revokeContactShare(shareId: String) {
        db.trustedContactDao().revokeShareLog(shareId)
    }

    suspend fun askAssistant(query: String, language: String, activeIncident: IncidentEntity?): Unit {
        db.chatDao().insertMessage(ChatMessageEntity(role = "user", text = query))
        val prefs = prefsFlow.first()
        val checklist = checklistFlow.first()

        // Try live backend Groq/Gemini + Hybrid RAG first if authenticated
        if (prefs.accessToken.isNotBlank()) {
            try {
                val resp = api.aiChat(
                    "Bearer ${prefs.accessToken}",
                    AiChatReq(
                        query = query,
                        language = language,
                        incidentId = activeIncident?.serverId,
                        includeIncidentContext = activeIncident?.serverId != null,
                    ),
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val b = resp.body()!!
                    val citeText = b.citations.joinToString("\n") {
                        "• ${it.title} (${it.publisher}, Verified ${it.lastVerified}) — ${it.sourceUrl}"
                    }
                    db.chatDao().insertMessage(
                        ChatMessageEntity(
                            role = "assistant",
                            text = b.answer,
                            citationsSummary = citeText,
                            providerUsed = "${b.providerUsed} (${b.modelUsed})",
                            uncertaintyNotice = b.uncertaintyNotice,
                        )
                    )
                    return
                }
            } catch (_: Exception) {
                // Fall back seamlessly to local offline RAG engine
            }
        }

        val local = localEngine.answerOfflineQuery(query, language, activeIncident, checklist)
        val citeText = local.citations.joinToString("\n") {
            "• ${it.title} (${it.publisher}, Verified ${it.lastVerified}) — ${it.sourceUrl}"
        }
        db.chatDao().insertMessage(
            ChatMessageEntity(
                role = "assistant",
                text = local.answer,
                citationsSummary = citeText,
                providerUsed = if (local.grounded) "offline_rag" else "offline_guardrail",
                uncertaintyNotice = local.uncertaintyNotice,
            )
        )
    }

    suspend fun generateReportDraft(incident: IncidentEntity, language: String): String {
        val prefs = prefsFlow.first()
        val checklist = checklistFlow.first()
        if (prefs.accessToken.isNotBlank() && incident.serverId != null) {
            try {
                val resp = api.generateReportDraft(
                    "Bearer ${prefs.accessToken}",
                    ReportDraftReq(incidentId = incident.serverId, language = language),
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val txt = resp.body()!!.draftText
                    db.incidentDao().upsertIncident(incident.copy(reportDraftText = txt, updatedAtMillis = System.currentTimeMillis()))
                    return txt
                }
            } catch (_: Exception) {
                // Fallback locally
            }
        }
        val localDraft = localEngine.buildReportDraft(incident, checklist, language == "bn")
        db.incidentDao().upsertIncident(incident.copy(reportDraftText = localDraft, updatedAtMillis = System.currentTimeMillis()))
        return localDraft
    }

    suspend fun triggerManualSyncNow(): String {
        val pending = db.syncOutboxDao().getPendingItems()
        if (pending.isEmpty()) return "All local records are already synchronized."
        IncidentSyncWorker.enqueue(context)
        val prefs = prefsFlow.first()
        return if (prefs.accessToken.isBlank()) {
            "${pending.size} record(s) saved safely offline. Sign in to sync with backend."
        } else {
            "Queued ${pending.size} pending item(s) for background sync."
        }
    }

    suspend fun clearChatHistory() = db.chatDao().clearMessages()
}

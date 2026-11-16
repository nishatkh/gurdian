package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GuardianApplication
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChecklistItemEntity
import com.example.data.local.ContactShareLogEntity
import com.example.data.local.EmergencyResourceEntity
import com.example.data.local.IncidentEntity
import com.example.data.local.IncidentEventEntity
import com.example.data.local.IncidentEvidenceEntity
import com.example.data.local.SyncOutboxEntity
import com.example.data.local.TrustedContactEntity
import com.example.data.local.UserSessionPrefs
import com.example.domain.ExplainableAssessment
import com.example.domain.PdfReportExporter
import com.example.security.KeystoreCryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GuardianMainUiState(
    val prefs: UserSessionPrefs = UserSessionPrefs(),
    val incidents: List<IncidentEntity> = emptyList(),
    val recentEvents: List<IncidentEventEntity> = emptyList(),
    val checklist: List<ChecklistItemEntity> = emptyList(),
    val contacts: List<TrustedContactEntity> = emptyList(),
    val shareLogs: List<ContactShareLogEntity> = emptyList(),
    val resources: List<EmergencyResourceEntity> = emptyList(),
    val outbox: List<SyncOutboxEntity> = emptyList(),
    val chatMessages: List<ChatMessageEntity> = emptyList(),
) {
    val activeIncident: IncidentEntity?
        get() = incidents.firstOrNull { it.status in setOf("active", "draft", "additional_info_requested", "submitted_for_review") }
            ?: incidents.firstOrNull()

    val completedChecklistCount: Int
        get() = checklist.count { it.isCompleted }

    val pendingSyncCount: Int
        get() = outbox.count { it.status != "synced" }
}

class GuardianViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as GuardianApplication).repository

    val uiState: StateFlow<GuardianMainUiState> = combine(
        repo.prefsFlow,
        repo.incidentsFlow,
        repo.recentEventsFlow,
        repo.checklistFlow,
        repo.contactsFlow,
    ) { prefs, incidents, events, checklist, contacts ->
        GuardianMainUiState(
            prefs = prefs,
            incidents = incidents,
            recentEvents = events,
            checklist = checklist,
            contacts = contacts,
        )
    }.combine(
        combine(
            repo.shareLogsFlow,
            repo.resourcesFlow,
            repo.outboxFlow,
            repo.chatMessagesFlow,
        ) { shares, resources, outbox, chat ->
            Quadruple(shares, resources, outbox, chat)
        }
    ) { base, extra ->
        base.copy(
            shareLogs = extra.first,
            resources = extra.second,
            outbox = extra.third,
            chatMessages = extra.fourth,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GuardianMainUiState(),
    )

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showMessage(msg: String) {
        _statusBanner.value = msg
    }

    fun observeIncident(id: String): Flow<IncidentEntity?> = repo.observeIncident(id)
    fun observeEvents(id: String): Flow<List<IncidentEventEntity>> = repo.observeEvents(id)
    fun observeEvidence(id: String): Flow<List<IncidentEvidenceEntity>> = repo.observeEvidence(id)

    fun completeOnboarding() {
        viewModelScope.launch { repo.prefsRepo.completeOnboarding() }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.loginRemoteOrError(email, password)
            _isBusy.value = false
            res.onFailure { _statusBanner.value = it.message }
        }
    }

    fun register(name: String, email: String, password: String, phone: String?, lang: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.registerRemoteOrOffline(name, email, password, phone, lang)
            _isBusy.value = false
            res.onSuccess { _statusBanner.value = it }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun enterOfflineMode(name: String) {
        viewModelScope.launch {
            repo.activateOfflineWorkspace(name)
        }
    }

    fun toggleLanguage(lang: String) {
        viewModelScope.launch { repo.prefsRepo.setLanguage(lang) }
    }

    fun saveDeviceVault(brand: String, model: String, rawImei: String) {
        viewModelScope.launch {
            val clean = rawImei.filter { it.isDigit() }
            if (clean.isNotBlank() && !KeystoreCryptoManager.isValidLuhnImei(clean)) {
                _statusBanner.value = "Warning: IMEI did not pass 15-digit Luhn checksum — saved anyway, please double check."
            } else {
                _statusBanner.value = "Device & encrypted IMEI saved in local Keystore vault."
            }
            val enc = if (clean.isNotBlank()) KeystoreCryptoManager.encrypt(clean) else ""
            repo.prefsRepo.saveDeviceInfo(brand.trim(), model.trim(), enc, clean.takeLast(4))
        }
    }

    fun decryptImei(encrypted: String): String = KeystoreCryptoManager.decrypt(encrypted)

    fun createEmergencyIncident(
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
        onCreated: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val id = repo.createIncidentOfflineFirst(
                category = category,
                title = title,
                description = description,
                deviceBrand = deviceBrand,
                deviceModel = deviceModel,
                rawImei = rawImei,
                locationLabel = locationLabel,
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy,
                locationConsent = locationConsent,
            )
            _statusBanner.value = "Incident saved offline & queued for sync."
            onCreated(id)
        }
    }

    fun addObservation(incidentId: String, note: String) {
        if (note.isBlank()) return
        viewModelScope.launch {
            repo.addTimelineObservation(incidentId, note)
            _statusBanner.value = "Timeline observation recorded."
        }
    }

    fun transitionIncident(incidentId: String, nextStatus: String) {
        viewModelScope.launch {
            repo.transitionIncidentStatus(incidentId, nextStatus)
            _statusBanner.value = "Incident status updated to $nextStatus."
        }
    }

    fun updateIncident(
        incidentId: String,
        description: String,
        locationLabel: String,
        brand: String,
        model: String,
        imei: String?,
    ) {
        viewModelScope.launch {
            repo.updateIncidentDetails(incidentId, description, locationLabel, brand, model, imei)
            _statusBanner.value = "Incident updated and queued for sync."
        }
    }

    fun attachEvidence(incidentId: String, uriString: String, displayName: String) {
        viewModelScope.launch {
            repo.attachEvidenceUri(incidentId, uriString, displayName, "Attached via Photo Picker")
            _statusBanner.value = "Evidence attached to incident timeline."
        }
    }

    fun toggleChecklist(item: ChecklistItemEntity) {
        viewModelScope.launch { repo.toggleChecklistItem(item) }
    }

    fun updateChecklistNotes(item: ChecklistItemEntity, notes: String) {
        viewModelScope.launch { repo.updateChecklistNotes(item, notes) }
    }

    fun addTrustedContact(name: String, relation: String, phone: String, consent: Boolean) {
        if (name.isBlank() || phone.isBlank()) return
        viewModelScope.launch {
            repo.addTrustedContact(name, relation, phone, consent)
            _statusBanner.value = "Trusted contact saved securely."
        }
    }

    fun deleteTrustedContact(id: String) {
        viewModelScope.launch { repo.deleteTrustedContact(id) }
    }

    fun shareIncidentWithContact(incidentId: String, contact: TrustedContactEntity, includeLoc: Boolean) {
        viewModelScope.launch {
            repo.logContactShare(incidentId, contact, includeLoc)
            _statusBanner.value = "Consent-confirmed alert logged for ${contact.name}."
        }
    }

    fun revokeShare(shareId: String) {
        viewModelScope.launch {
            repo.revokeContactShare(shareId)
            _statusBanner.value = "Contact sharing access revoked."
        }
    }

    fun askAssistant(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isBusy.value = true
            val state = uiState.value
            repo.askAssistant(query.trim(), state.prefs.language, state.activeIncident)
            _isBusy.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch { repo.clearChatHistory() }
    }

    fun computeAssessment(incident: IncidentEntity, eventCount: Int): ExplainableAssessment {
        val s = uiState.value
        return repo.localEngine.assessIncident(
            incident = incident,
            checklist = s.checklist,
            eventCount = eventCount,
            bn = s.prefs.language == "bn",
        )
    }

    fun generateReportDraft(incident: IncidentEntity) {
        viewModelScope.launch {
            _isBusy.value = true
            repo.generateReportDraft(incident, uiState.value.prefs.language)
            _isBusy.value = false
            _statusBanner.value = "Report draft updated (verify before using for Online GD)."
        }
    }

    fun exportAndSharePdf(incident: IncidentEntity) {
        val ctx = getApplication<Application>()
        val draft = incident.reportDraftText ?: repo.localEngine.buildReportDraft(
            incident,
            uiState.value.checklist,
            uiState.value.prefs.language == "bn",
        )
        val file = PdfReportExporter.exportDraftPdf(ctx, incident.id, draft)
        PdfReportExporter.sharePdf(ctx, file)
    }

    fun triggerSyncNow() {
        viewModelScope.launch {
            _statusBanner.value = repo.triggerManualSyncNow()
        }
    }

    fun updatePrivacyPrefs(locationConsent: Boolean, aiConsent: Boolean, notifySync: Boolean) {
        viewModelScope.launch {
            repo.prefsRepo.updatePrivacy(locationConsent, aiConsent, notifySync)
        }
    }

    fun signOut() {
        viewModelScope.launch { repo.prefsRepo.signOut() }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

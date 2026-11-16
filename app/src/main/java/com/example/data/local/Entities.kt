package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val serverId: String? = null,
    val category: String, // lost_phone, suspected_theft, snatching, sim_compromise, account_compromise
    val status: String, // draft, active, submitted_for_review, under_review, additional_info_requested, approved, closed, cancelled
    val title: String,
    val description: String,
    val occurredAtMillis: Long,
    val deviceBrand: String = "",
    val deviceModel: String = "",
    val imeiEncrypted: String = "",
    val imeiLast4: String = "",
    val locationLabel: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracyMeters: Float? = null,
    val locationConsentGranted: Boolean = false,
    val aiSummary: String? = null,
    val missingFieldsCsv: String = "",
    val reportDraftText: String? = null,
    val reviewStatus: String = "not_submitted",
    val reviewerNotes: String? = null,
    val syncStatus: String = "pending", // pending, synced, failed
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "incident_events")
data class IncidentEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val incidentId: String,
    val eventType: String,
    val note: String,
    val actorRole: String = "user",
    val eventTimestampMillis: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending",
)

@Entity(tableName = "incident_evidence")
data class IncidentEvidenceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val incidentId: String,
    val uriString: String,
    val displayName: String,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "checklist_items")
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    val category: String,
    val sortOrder: Int,
    val titleEn: String,
    val titleBn: String,
    val whatToDoEn: String,
    val whatToDoBn: String,
    val whyItMattersEn: String,
    val whyItMattersBn: String,
    val officialUrl: String?,
    val officialPhone: String?,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val notes: String = "",
)

@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val relationshipLabel: String,
    val phoneEncrypted: String,
    val phoneMasked: String,
    val isVerified: Boolean = false,
    val notifySmsConsent: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "contact_share_logs")
data class ContactShareLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val incidentId: String,
    val contactId: String,
    val contactName: String,
    val includeLocation: Boolean,
    val isRevoked: Boolean = false,
    val sharedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "emergency_resources")
data class EmergencyResourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val address: String,
    val contactNumber: String,
    val sourceUrl: String,
    val verificationDate: String,
    val isVerified: Boolean = true,
)

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey val clientEventId: String = UUID.randomUUID().toString(),
    val entityType: String, // incident_upsert, incident_event, checklist_toggle
    val payloadJson: String,
    val originalTimestampMillis: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = "pending", // pending, synced, failed
    val lastError: String? = null,
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val role: String, // user, assistant
    val text: String,
    val citationsSummary: String = "",
    val providerUsed: String = "local_rag",
    val uncertaintyNotice: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

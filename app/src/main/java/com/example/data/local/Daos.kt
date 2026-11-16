package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Query("SELECT * FROM incidents ORDER BY updatedAtMillis DESC")
    fun observeAllIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE id = :id LIMIT 1")
    fun observeIncident(id: String): Flow<IncidentEntity?>

    @Query("SELECT * FROM incidents WHERE id = :id LIMIT 1")
    suspend fun getIncidentById(id: String): IncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncident(incident: IncidentEntity)

    @Query("SELECT * FROM incident_events WHERE incidentId = :incidentId ORDER BY eventTimestampMillis ASC")
    fun observeEventsForIncident(incidentId: String): Flow<List<IncidentEventEntity>>

    @Query("SELECT * FROM incident_events ORDER BY eventTimestampMillis DESC LIMIT 50")
    fun observeRecentEvents(): Flow<List<IncidentEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: IncidentEventEntity)

    @Query("SELECT * FROM incident_evidence WHERE incidentId = :incidentId ORDER BY createdAtMillis DESC")
    fun observeEvidenceForIncident(incidentId: String): Flow<List<IncidentEvidenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidence: IncidentEvidenceEntity)

    @Query("DELETE FROM incidents")
    suspend fun clearAllIncidents()
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_items ORDER BY sortOrder ASC")
    fun observeAllItems(): Flow<List<ChecklistItemEntity>>

    @Query("SELECT COUNT(*) FROM checklist_items")
    suspend fun countItems(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChecklistItemEntity>)

    @Update
    suspend fun updateItem(item: ChecklistItemEntity)
}

@Dao
interface TrustedContactDao {
    @Query("SELECT * FROM trusted_contacts ORDER BY createdAtMillis DESC")
    fun observeContacts(): Flow<List<TrustedContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: TrustedContactEntity)

    @Query("DELETE FROM trusted_contacts WHERE id = :id")
    suspend fun deleteContact(id: String)

    @Query("SELECT * FROM contact_share_logs ORDER BY sharedAtMillis DESC")
    fun observeShareLogs(): Flow<List<ContactShareLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShareLog(log: ContactShareLogEntity)

    @Query("UPDATE contact_share_logs SET isRevoked = 1 WHERE id = :id")
    suspend fun revokeShareLog(id: String)
}

@Dao
interface ResourceDao {
    @Query("SELECT * FROM emergency_resources ORDER BY category ASC, name ASC")
    fun observeResources(): Flow<List<EmergencyResourceEntity>>

    @Query("SELECT COUNT(*) FROM emergency_resources")
    suspend fun countResources(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<EmergencyResourceEntity>)
}

@Dao
interface SyncOutboxDao {
    @Query("SELECT * FROM sync_outbox ORDER BY originalTimestampMillis DESC")
    fun observeOutbox(): Flow<List<SyncOutboxEntity>>

    @Query("SELECT * FROM sync_outbox WHERE status != 'synced' ORDER BY originalTimestampMillis ASC")
    suspend fun getPendingItems(): List<SyncOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncOutboxEntity)

    @Query("UPDATE sync_outbox SET status = 'synced', lastError = NULL WHERE clientEventId IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("UPDATE sync_outbox SET status = 'failed', retryCount = retryCount + 1, lastError = :error WHERE clientEventId = :id")
    suspend fun markFailed(id: String, error: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY createdAtMillis ASC")
    fun observeMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()
}

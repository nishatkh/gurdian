package com.example.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.GuardianDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.GuardianApiService
import com.example.data.remote.SyncBatchReq
import com.example.data.remote.SyncOutboxItemDto
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class IncidentSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = GuardianDatabase.getInstance(applicationContext)
        val outboxDao = db.syncOutboxDao()
        val prefs = UserPreferencesRepository(applicationContext).prefsFlow.first()

        val pending = outboxDao.getPendingItems()
        if (pending.isEmpty()) return Result.success()
        if (prefs.accessToken.isBlank()) {
            // Offline-only session: preserve local records safely without discarding
            return Result.success()
        }

        val api = GuardianApiService.create()
        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val dtos = pending.map { item ->
            val obj = runCatching { JSONObject(item.payloadJson) }.getOrNull()
            val map = mutableMapOf<String, Any?>()
            if (obj != null) {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = obj.opt(k)
                }
            }
            SyncOutboxItemDto(
                clientEventId = item.clientEventId,
                entityType = item.entityType,
                payload = map,
                originalTimestamp = isoFmt.format(Date(item.originalTimestampMillis)),
            )
        }

        return try {
            val resp = api.syncOutbox("Bearer ${prefs.accessToken}", SyncBatchReq(dtos))
            if (resp.isSuccessful) {
                val body = resp.body()
                val doneIds = (body?.processedIds.orEmpty() + body?.deduplicatedIds.orEmpty()).distinct()
                if (doneIds.isNotEmpty()) {
                    outboxDao.markSynced(doneIds)
                }
                UserPreferencesRepository(applicationContext).setLastSync(System.currentTimeMillis())
                Result.success()
            } else {
                pending.forEach { outboxDao.markFailed(it.clientEventId, "HTTP ${resp.code()}") }
                Result.retry()
            }
        } catch (ex: Exception) {
            pending.forEach {
                outboxDao.markFailed(it.clientEventId, ex.message ?: "Network unavailable")
            }
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "guardian_outbox_sync"

        fun enqueue(context: Context) {
            runCatching {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val req = OneTimeWorkRequestBuilder<IncidentSyncWorker>()
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, req)
            }
        }
    }
}

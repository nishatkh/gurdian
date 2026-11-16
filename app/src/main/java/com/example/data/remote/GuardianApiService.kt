package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

@JsonClass(generateAdapter = true)
data class AuthLoginReq(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class AuthRegisterReq(
    val email: String,
    val password: String,
    @Json(name = "full_name") val fullName: String,
    val phone: String? = null,
    @Json(name = "preferred_language") val preferredLanguage: String = "en",
)

@JsonClass(generateAdapter = true)
data class AuthRes(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "user_id") val userId: String,
    val email: String,
    @Json(name = "full_name") val fullName: String,
    val role: String,
    @Json(name = "preferred_language") val preferredLanguage: String,
)

@JsonClass(generateAdapter = true)
data class SyncOutboxItemDto(
    @Json(name = "client_event_id") val clientEventId: String,
    @Json(name = "entity_type") val entityType: String,
    val payload: Map<String, Any?>,
    @Json(name = "original_timestamp") val originalTimestamp: String,
)

@JsonClass(generateAdapter = true)
data class SyncBatchReq(val items: List<SyncOutboxItemDto>)

@JsonClass(generateAdapter = true)
data class SyncBatchRes(
    @Json(name = "processed_ids") val processedIds: List<String>,
    @Json(name = "deduplicated_ids") val deduplicatedIds: List<String>,
)

@JsonClass(generateAdapter = true)
data class AiChatReq(
    val query: String,
    val language: String = "auto",
    @Json(name = "incident_id") val incidentId: String? = null,
    @Json(name = "include_incident_context") val includeIncidentContext: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class CitationDto(
    @Json(name = "doc_id") val docId: String,
    val title: String,
    val publisher: String,
    @Json(name = "source_url") val sourceUrl: String,
    @Json(name = "last_verified") val lastVerified: String,
    val score: Double,
    val excerpt: String,
)

@JsonClass(generateAdapter = true)
data class AiChatRes(
    val answer: String,
    val language: String,
    @Json(name = "provider_used") val providerUsed: String,
    @Json(name = "model_used") val modelUsed: String,
    @Json(name = "used_fallback") val usedFallback: Boolean,
    val grounded: Boolean,
    val citations: List<CitationDto>,
    @Json(name = "uncertainty_notice") val uncertaintyNotice: String? = null,
)

@JsonClass(generateAdapter = true)
data class ReportDraftReq(
    @Json(name = "incident_id") val incidentId: String,
    val language: String = "en",
)

@JsonClass(generateAdapter = true)
data class ReportDraftRes(
    val id: String,
    @Json(name = "incident_id") val incidentId: String,
    @Json(name = "is_official_gd") val isOfficialGd: Boolean,
    val disclaimer: String,
    @Json(name = "draft_text") val draftText: String,
    @Json(name = "missing_information") val missingInformation: List<String>,
    @Json(name = "provider_used") val providerUsed: String,
)

interface GuardianApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body req: AuthLoginReq): Response<AuthRes>

    @POST("api/v1/auth/register")
    suspend fun register(@Body req: AuthRegisterReq): Response<AuthRes>

    @POST("api/v1/sync/outbox")
    suspend fun syncOutbox(
        @Header("Authorization") bearer: String,
        @Body req: SyncBatchReq,
    ): Response<SyncBatchRes>

    @POST("api/v1/ai/chat")
    suspend fun aiChat(
        @Header("Authorization") bearer: String,
        @Body req: AiChatReq,
    ): Response<AiChatRes>

    @POST("api/v1/ai/summarise")
    suspend fun summariseIncident(
        @Header("Authorization") bearer: String,
        @Query("incident_id") incidentId: String,
    ): Response<Map<String, Any>>

    @POST("api/v1/ai/report-draft")
    suspend fun generateReportDraft(
        @Header("Authorization") bearer: String,
        @Body req: ReportDraftReq,
    ): Response<ReportDraftRes>

    @GET("api/v1/resources")
    suspend fun getResources(): Response<List<Map<String, Any>>>

    companion object {
        fun create(): GuardianApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()
            val moshi = com.squareup.moshi.Moshi.Builder()
                .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()
            val baseUrl = if (BuildConfig.BACKEND_BASE_URL.endsWith("/")) {
                BuildConfig.BACKEND_BASE_URL
            } else {
                "${BuildConfig.BACKEND_BASE_URL}/"
            }
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GuardianApiService::class.java)
        }
    }
}

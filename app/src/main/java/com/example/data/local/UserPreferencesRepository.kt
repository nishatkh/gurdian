package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "guardian_prefs")

data class UserSessionPrefs(
    val onboardingDone: Boolean = false,
    val isLoggedIn: Boolean = false,
    val offlineModeOnly: Boolean = false,
    val language: String = "en", // en | bn
    val fullName: String = "Guardian User",
    val email: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val deviceBrand: String = "",
    val deviceModel: String = "",
    val deviceImeiEncrypted: String = "",
    val deviceImeiLast4: String = "",
    val consentLocation: Boolean = false,
    val consentAi: Boolean = true,
    val notifySync: Boolean = true,
    val lastSyncMillis: Long = 0L,
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val OFFLINE_ONLY = booleanPreferencesKey("offline_mode_only")
        val LANGUAGE = stringPreferencesKey("language")
        val FULL_NAME = stringPreferencesKey("full_name")
        val EMAIL = stringPreferencesKey("email")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val DEVICE_BRAND = stringPreferencesKey("device_brand")
        val DEVICE_MODEL = stringPreferencesKey("device_model")
        val DEVICE_IMEI_ENC = stringPreferencesKey("device_imei_enc")
        val DEVICE_IMEI_LAST4 = stringPreferencesKey("device_imei_last4")
        val CONSENT_LOCATION = booleanPreferencesKey("consent_location")
        val CONSENT_AI = booleanPreferencesKey("consent_ai")
        val NOTIFY_SYNC = booleanPreferencesKey("notify_sync")
        val LAST_SYNC_MILLIS = longPreferencesKey("last_sync_millis")
    }

    val prefsFlow: Flow<UserSessionPrefs> = context.dataStore.data.map { p ->
        UserSessionPrefs(
            onboardingDone = p[Keys.ONBOARDING_DONE] ?: false,
            isLoggedIn = p[Keys.IS_LOGGED_IN] ?: false,
            offlineModeOnly = p[Keys.OFFLINE_ONLY] ?: false,
            language = p[Keys.LANGUAGE] ?: "en",
            fullName = p[Keys.FULL_NAME] ?: "Guardian User",
            email = p[Keys.EMAIL] ?: "",
            accessToken = p[Keys.ACCESS_TOKEN] ?: "",
            refreshToken = p[Keys.REFRESH_TOKEN] ?: "",
            deviceBrand = p[Keys.DEVICE_BRAND] ?: "",
            deviceModel = p[Keys.DEVICE_MODEL] ?: "",
            deviceImeiEncrypted = p[Keys.DEVICE_IMEI_ENC] ?: "",
            deviceImeiLast4 = p[Keys.DEVICE_IMEI_LAST4] ?: "",
            consentLocation = p[Keys.CONSENT_LOCATION] ?: false,
            consentAi = p[Keys.CONSENT_AI] ?: true,
            notifySync = p[Keys.NOTIFY_SYNC] ?: true,
            lastSyncMillis = p[Keys.LAST_SYNC_MILLIS] ?: 0L,
        )
    }

    suspend fun completeOnboarding() {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun setSession(
        fullName: String,
        email: String,
        accessToken: String,
        refreshToken: String,
        offlineOnly: Boolean,
    ) {
        context.dataStore.edit {
            it[Keys.ONBOARDING_DONE] = true
            it[Keys.IS_LOGGED_IN] = true
            it[Keys.OFFLINE_ONLY] = offlineOnly
            it[Keys.FULL_NAME] = fullName
            it[Keys.EMAIL] = email
            it[Keys.ACCESS_TOKEN] = accessToken
            it[Keys.REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    suspend fun saveDeviceInfo(
        brand: String,
        model: String,
        imeiEncrypted: String,
        imeiLast4: String,
    ) {
        context.dataStore.edit {
            it[Keys.DEVICE_BRAND] = brand
            it[Keys.DEVICE_MODEL] = model
            it[Keys.DEVICE_IMEI_ENC] = imeiEncrypted
            it[Keys.DEVICE_IMEI_LAST4] = imeiLast4
        }
    }

    suspend fun updatePrivacy(locationConsent: Boolean, aiConsent: Boolean, notifySync: Boolean) {
        context.dataStore.edit {
            it[Keys.CONSENT_LOCATION] = locationConsent
            it[Keys.CONSENT_AI] = aiConsent
            it[Keys.NOTIFY_SYNC] = notifySync
        }
    }

    suspend fun setLastSync(millis: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_MILLIS] = millis }
    }

    suspend fun signOut() {
        context.dataStore.edit {
            it[Keys.IS_LOGGED_IN] = false
            it[Keys.ACCESS_TOKEN] = ""
            it[Keys.REFRESH_TOKEN] = ""
        }
    }
}

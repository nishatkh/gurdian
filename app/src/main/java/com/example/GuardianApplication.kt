package com.example

import android.app.Application
import com.example.data.local.GuardianDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.GuardianRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GuardianApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: GuardianDatabase
        private set
    lateinit var preferencesRepository: UserPreferencesRepository
        private set
    lateinit var repository: GuardianRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = GuardianDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)
        repository = GuardianRepository(this, database, preferencesRepository)
        appScope.launch {
            repository.ensureSeeded()
        }
    }
}

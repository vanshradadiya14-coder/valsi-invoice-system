package com.valsi.invoicesystem.data.repository

import com.valsi.invoicesystem.data.dao.AppSettingsDao
import com.valsi.invoicesystem.data.entity.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val appSettingsDao: AppSettingsDao,
) {
    /** Always emits a value; falls back to defaults if the row hasn't been seeded yet. */
    fun observe(): Flow<AppSettings> = appSettingsDao.observe().map { it ?: AppSettings() }

    suspend fun getOnce(): AppSettings = appSettingsDao.getOnce() ?: AppSettings()

    /** Saves user-editable settings, preserving the singleton id and the invoice counter. */
    suspend fun save(settings: AppSettings) {
        appSettingsDao.upsert(settings.copy(id = AppSettings.SINGLETON_ID))
    }
}

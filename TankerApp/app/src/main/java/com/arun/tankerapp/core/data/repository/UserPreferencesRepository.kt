package com.arun.tankerapp.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val LAST_REPORT_DATE_KEY = longPreferencesKey("last_report_date")

    fun getLastReportDate(): Flow<LocalDate?> {
        return context.dataStore.data.map { preferences ->
            val timestamp = preferences[LAST_REPORT_DATE_KEY]
            if (timestamp != null) {
                LocalDate.ofEpochDay(timestamp)
            } else {
                null
            }
        }
    }

    suspend fun setLastReportDate(date: LocalDate) {
        context.dataStore.edit { preferences ->
            preferences[LAST_REPORT_DATE_KEY] = date.toEpochDay()
        }
    }
}

package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.entity.Apartment
import com.arun.tankerapp.core.data.database.entity.VacancyLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

// Moved ApartmentStatus here or keep? 
// If it's used elsewhere, keeping it here might break imports if they imported from Repository class.
// But usually imports are `com.arun.tankerapp.core.data.repository.ApartmentStatus`.
// So keeping it top-level is fine.

data class ApartmentStatus(
    val apartment: Apartment,
    val isVacant: Boolean
)

interface VacancyRepository {
    fun getApartmentStatuses(date: LocalDate): Flow<List<ApartmentStatus>>
    fun getVacanciesForMonth(yearMonth: YearMonth): Flow<List<VacancyLog>>
    suspend fun toggleVacancy(apartmentId: Long, date: LocalDate, isVacant: Boolean)
    
    // New methods for Billing
    fun getAllApartments(): Flow<List<Apartment>>
    fun getAllVacancies(): Flow<List<VacancyLog>>
}

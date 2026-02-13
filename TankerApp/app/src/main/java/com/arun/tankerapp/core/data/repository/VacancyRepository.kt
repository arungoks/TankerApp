package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.entity.VacancyLog
import com.arun.tankerapp.core.data.model.ApartmentStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface VacancyRepository {
    fun getApartmentStatuses(date: LocalDate): Flow<List<ApartmentStatus>>
    fun getVacanciesForMonth(yearMonth: YearMonth): Flow<List<VacancyLog>>
    suspend fun toggleVacancy(apartmentId: Long, date: LocalDate, isVacant: Boolean)
}

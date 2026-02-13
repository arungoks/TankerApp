package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.dao.ApartmentDao
import com.arun.tankerapp.core.data.database.dao.VacancyDao
import com.arun.tankerapp.core.data.database.entity.VacancyLog
import com.arun.tankerapp.core.data.model.ApartmentStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomVacancyRepository @Inject constructor(
    private val vacancyDao: VacancyDao,
    private val apartmentDao: ApartmentDao
) : VacancyRepository {
    /**
     * Returns a flow of all apartments with their vacancy status for a given date.
     */
    override fun getApartmentStatuses(date: LocalDate): Flow<List<ApartmentStatus>> {
        val dateStr = date.toString()
        return combine(
            apartmentDao.getAllApartments(),
            vacancyDao.getVacanciesForDate(dateStr)
        ) { apartments, vacancies ->
            val vacantIds = vacancies.map { it.apartmentId }.toSet()
            apartments.map { apt ->
                ApartmentStatus(
                    apartment = apt,
                    isVacant = vacantIds.contains(apt.id)
                )
            }.sortedBy { it.apartment.number.toIntOrNull() ?: Int.MAX_VALUE }
        }
    }

    /**
     * Returns all vacancies that overlap with the given month.
     */
    override fun getVacanciesForMonth(yearMonth: YearMonth): Flow<List<VacancyLog>> {
        val startDate = yearMonth.atDay(1).toString()
        val endDate = yearMonth.atEndOfMonth().toString()
        return vacancyDao.getVacanciesInRange(startDate, endDate)
    }

    /**
     * Toggles vacancy for a specific apartment on a specific date.
     * Note: Current implementation treats unchecking as deletion of the ENTIRE vacancy range covering that date.
     * Future improvement: Split ranges.
     */
    override suspend fun toggleVacancy(apartmentId: Long, date: LocalDate, isVacant: Boolean) {
        val dateStr = date.toString()
        val existing = vacancyDao.getVacancyForApartmentDate(apartmentId, dateStr)
        
        if (isVacant) {
            if (existing == null) {
                vacancyDao.insert(
                    VacancyLog(
                        apartmentId = apartmentId, 
                        startDate = dateStr, 
                        endDate = dateStr
                    )
                )
            }
        } else {
            if (existing != null) {
                vacancyDao.delete(existing)
            }
        }
    }
}

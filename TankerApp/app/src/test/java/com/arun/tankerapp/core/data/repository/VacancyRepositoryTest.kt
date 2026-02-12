package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.dao.ApartmentDao
import com.arun.tankerapp.core.data.database.dao.VacancyDao
import com.arun.tankerapp.core.data.database.entity.Apartment
import com.arun.tankerapp.core.data.database.entity.VacancyLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class VacancyRepositoryTest {

    private lateinit var repository: VacancyRepository
    private val vacancies = mutableListOf<VacancyLog>()
    private val apartments = listOf(
        Apartment(id = 101, number = "101"),
        Apartment(id = 102, number = "102")
    )

    private val fakeVacancyDao = object : VacancyDao {
        override suspend fun insert(vacancyLog: VacancyLog) {
            vacancies.add(vacancyLog)
        }

        override suspend fun delete(vacancyLog: VacancyLog) {
            vacancies.remove(vacancyLog)
        }

        override fun getVacanciesForDate(date: String): Flow<List<VacancyLog>> {
            return flowOf(vacancies.filter { it.startDate <= date && it.endDate >= date })
        }
        
        override fun getVacanciesByApartment(apartmentId: Long): Flow<List<VacancyLog>> = flowOf(emptyList()) // Unused
        override fun getAllVacancies(): Flow<List<VacancyLog>> = flowOf(emptyList()) // Unused
        override suspend fun getCount(): Int = vacancies.size

        override suspend fun getVacancyForApartmentDate(apartmentId: Long, date: String): VacancyLog? {
            return vacancies.firstOrNull { 
                it.apartmentId == apartmentId && it.startDate <= date && it.endDate >= date 
            }
        }

        override fun getVacanciesInRange(startDate: String, endDate: String): Flow<List<VacancyLog>> {
            return flowOf(vacancies.filter { it.startDate <= endDate && it.endDate >= startDate })
        }
    }

    private val fakeApartmentDao = object : ApartmentDao {
        override fun getAllApartments(): Flow<List<Apartment>> = flowOf(apartments)
        override suspend fun insertAll(apartments: List<Apartment>) {}
        override suspend fun getCount(): Int = apartments.size
    }

    @Before
    fun setUp() {
        repository = VacancyRepository(fakeVacancyDao, fakeApartmentDao)
    }

    @Test
    fun getApartmentStatuses_returns_correct_status() = runBlocking {
        // Given 101 is vacant
        vacancies.add(VacancyLog(apartmentId = 101, startDate = "2026-02-12", endDate = "2026-02-12"))
        
        val date = LocalDate.of(2026, 2, 12)
        val statuses = repository.getApartmentStatuses(date).first()
        
        val status101 = statuses.find { it.apartment.id == 101L }
        val status102 = statuses.find { it.apartment.id == 102L }
        
        assertTrue(status101!!.isVacant)
        assertEquals(false, status102!!.isVacant)
    }

    @Test
    fun toggleVacancy_adds_vacancy() = runBlocking {
        val date = LocalDate.of(2026, 2, 12)
        repository.toggleVacancy(102, date, true)
        
        val vacancy = vacancies.find { it.apartmentId == 102L }
        assertTrue(vacancy != null)
        assertEquals("2026-02-12", vacancy!!.startDate)
    }

    @Test
    fun toggleVacancy_removes_vacancy() = runBlocking {
        val date = LocalDate.of(2026, 2, 12)
        val existing = VacancyLog(apartmentId = 101, startDate = "2026-02-12", endDate = "2026-02-12")
        vacancies.add(existing)

        repository.toggleVacancy(101, date, false)
        
        val vacancy = vacancies.find { it.apartmentId == 101L }
        assertNull(vacancy)
    }
}

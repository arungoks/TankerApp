package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.dao.ApartmentDao
import com.arun.tankerapp.core.data.database.dao.TankerDao
import com.arun.tankerapp.core.data.database.dao.VacancyDao
import com.arun.tankerapp.core.data.database.entity.Apartment
import com.arun.tankerapp.core.data.database.dao.BillingCycleDao
import com.arun.tankerapp.core.data.database.entity.BillingCycle
import com.arun.tankerapp.core.data.database.entity.TankerLog
import com.arun.tankerapp.core.data.database.entity.VacancyLog
import com.arun.tankerapp.core.data.model.ApartmentBill
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class BillingRepositoryTest {

    private lateinit var repository: BillingRepository
    
    // Fakes
    private val apartments = mutableListOf<Apartment>()
    private val tankerLogs = mutableListOf<TankerLog>()
    private val vacancies = mutableListOf<VacancyLog>()

    private val fakeApartmentDao = object : ApartmentDao {
        override suspend fun insertAll(apts: List<Apartment>) {
            apartments.addAll(apts)
        }
        override fun getAllApartments(): Flow<List<Apartment>> = flowOf(apartments)
        override suspend fun getCount(): Int = apartments.size
    }

    private val fakeTankerDao = object : TankerDao {
        override suspend fun insert(tankerLog: TankerLog) { tankerLogs.add(tankerLog) }
        override suspend fun delete(tankerLog: TankerLog) {}
        override fun getAllTankerLogs(): Flow<List<TankerLog>> = flowOf(tankerLogs)
        override fun getTankerLogsByMonth(month: Int, year: Int): Flow<List<TankerLog>> = flowOf(emptyList())
        override suspend fun getCount(): Int = tankerLogs.size
        override suspend fun getTankerLogByDate(date: String): TankerLog? = null
        override suspend fun deleteByDate(date: String) {}
        override suspend fun updateTankerCount(date: String, count: Int) {}
    }

    private val fakeVacancyDao = object : VacancyDao {
        override suspend fun insert(vacancyLog: VacancyLog) { vacancies.add(vacancyLog) }
        override suspend fun delete(vacancyLog: VacancyLog) {}
        override fun getVacanciesByApartment(apartmentId: Long): Flow<List<VacancyLog>> = flowOf(emptyList())
        override fun getAllVacancies(): Flow<List<VacancyLog>> = flowOf(vacancies)
        override suspend fun getCount(): Int = vacancies.size
        override fun getVacanciesForDate(date: String): Flow<List<VacancyLog>> = flowOf(emptyList())
        override suspend fun getVacancyForApartmentDate(apartmentId: Long, date: String): VacancyLog? = null
        override fun getVacanciesInRange(startDate: String, endDate: String): Flow<List<VacancyLog>> = flowOf(emptyList())
    }

    @Before
    fun setUp() {
        repository = RoomBillingRepository(fakeApartmentDao, fakeTankerDao, fakeVacancyDao, fakeBillingCycleDao)
        apartments.clear()
        tankerLogs.clear()
        vacancies.clear()
    }

    @Test
    fun getBillingReport_calculates_correctly_without_vacancies() = runBlocking {
        // Given
        val apt1 = Apartment(1L, "101")
        val apt2 = Apartment(2L, "102")
        apartments.add(apt1)
        apartments.add(apt2)

        tankerLogs.add(TankerLog(date = "2026-02-10", month = 2, year = 2026, count = 2)) // 2 tankers
        tankerLogs.add(TankerLog(date = "2026-02-12", month = 2, year = 2026, count = 3)) // 3 tankers
        
        // When
        val report = repository.getBillingReport().first()

        // Then
        assertEquals(2, report.size)
        // Check apt1
        assertEquals(5, report[0].billableTankers) // 2 + 3
        assertEquals(5, report[0].totalTankersInCycle)
        // Check apt2
        assertEquals(5, report[1].billableTankers)
    }

    @Test
    fun getBillingReport_deducts_vacancies_correctly() = runBlocking {
        // Given
        val apt1 = Apartment(1L, "101")
        val apt2 = Apartment(2L, "102") // Has vacancy
        apartments.add(apt1)
        apartments.add(apt2)

        tankerLogs.add(TankerLog(date = "2026-02-10", month = 2, year = 2026, count = 2))
        tankerLogs.add(TankerLog(date = "2026-02-12", month = 2, year = 2026, count = 3))

        // Apt2 is vacant on Feb 10
        vacancies.add(VacancyLog(apartmentId = 2L, startDate = "2026-02-10", endDate = "2026-02-10"))

        // When
        val report = repository.getBillingReport().first()

        // Then
        // Apt1: no vacancy -> 5
        assertEquals(5, report[0].billableTankers)
        
        // Apt2: vacant on Feb 10 (2 tankers), present on Feb 12 (3 tankers) -> 3
        assertEquals(3, report[1].billableTankers) // 5 - 2 = 3
    }

    @Test
    fun getBillingReport_handles_vacancy_range() = runBlocking {
        // Given
        val apt1 = Apartment(1L, "101")
        apartments.add(apt1)

        tankerLogs.add(TankerLog(date = "2026-02-10", month = 2, year = 2026, count = 2))
        tankerLogs.add(TankerLog(date = "2026-02-12", month = 2, year = 2026, count = 3))

        // Vacant from Feb 9 to Feb 11 (covers Feb 10)
        vacancies.add(VacancyLog(apartmentId = 1L, startDate = "2026-02-09", endDate = "2026-02-11"))

        // When
        val report = repository.getBillingReport().first()

        // Then
        assertEquals(3, report[0].billableTankers)
    }
    
    @Test
    fun getBillingReport_handles_full_vacancy() = runBlocking {
        // Given
        val apt1 = Apartment(1L, "101")
        apartments.add(apt1)

        tankerLogs.add(TankerLog(date = "2026-02-10", month = 2, year = 2026, count = 1))

        vacancies.add(VacancyLog(apartmentId = 1L, startDate = "2026-02-01", endDate = "2026-02-28"))

        // When
        val report = repository.getBillingReport().first()

        // Then
        assertEquals(0, report[0].billableTankers)
    }

    private val billingCycles = mutableListOf<BillingCycle>()
    private val fakeBillingCycleDao = object : BillingCycleDao {
        override suspend fun insert(billingCycle: BillingCycle) {
            billingCycles.add(billingCycle)
        }

        override fun getAllCycles(): Flow<List<BillingCycle>> = flowOf(billingCycles)

        override suspend fun getCycleById(id: Int): BillingCycle? = billingCycles.find { it.id == id }
    }

    @Test
    fun archiveCurrentCycle_saves_correct_data() = runBlocking {
        // Given
        val startDate = LocalDate.of(2026, 2, 1)
        val endDate = LocalDate.of(2026, 2, 28)
        val totalTankers = 10

        // When
        repository.archiveCurrentCycle(startDate, endDate, totalTankers)

        // Then
        assertEquals(1, billingCycles.size)
        val savedCycle = billingCycles[0]
        assertEquals(startDate, savedCycle.startDate)
        assertEquals(endDate, savedCycle.endDate)
        assertEquals(totalTankers, savedCycle.totalTankers)
    }
}

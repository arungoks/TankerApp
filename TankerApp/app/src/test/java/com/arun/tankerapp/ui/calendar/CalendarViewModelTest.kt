package com.arun.tankerapp.ui.calendar

import com.arun.tankerapp.MainDispatcherRule
import com.arun.tankerapp.core.data.database.dao.ApartmentDao
import com.arun.tankerapp.core.data.database.dao.TankerDao
import com.arun.tankerapp.core.data.database.dao.VacancyDao
import com.arun.tankerapp.core.data.database.entity.Apartment
import com.arun.tankerapp.core.data.database.entity.TankerLog
import com.arun.tankerapp.core.data.database.entity.VacancyLog
import com.arun.tankerapp.core.data.repository.TankerRepository
import com.arun.tankerapp.core.data.repository.VacancyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: CalendarViewModel
    
    private val vacanciesFlow = MutableStateFlow<List<VacancyLog>>(emptyList())
    private val tankerLogsFlow = MutableStateFlow<List<TankerLog>>(emptyList())
    
    private val fakeVacancyDao = object : VacancyDao {
        override suspend fun insert(vacancyLog: VacancyLog) {
            val current = vacanciesFlow.value.toMutableList()
            current.add(vacancyLog)
            vacanciesFlow.value = current
        }
        override suspend fun delete(vacancyLog: VacancyLog) {
            val current = vacanciesFlow.value.toMutableList()
            current.remove(vacancyLog)
            vacanciesFlow.value = current
        }
        override fun getVacanciesForDate(date: String): Flow<List<VacancyLog>> = flowOf(emptyList())
        override fun getVacanciesByApartment(apartmentId: Long): Flow<List<VacancyLog>> = flowOf(emptyList())
        override fun getAllVacancies(): Flow<List<VacancyLog>> = flowOf(emptyList())
        override suspend fun getCount(): Int = 0
        override suspend fun getVacancyForApartmentDate(apartmentId: Long, date: String): VacancyLog? = null
        override fun getVacanciesInRange(startDate: String, endDate: String): Flow<List<VacancyLog>> = vacanciesFlow
    }

    private val fakeApartmentDao = object : ApartmentDao {
        override fun getAllApartments(): Flow<List<Apartment>> = flowOf(emptyList())
        override suspend fun insertAll(apartments: List<Apartment>) {}
        override suspend fun getCount(): Int = 0
    }

    private val fakeTankerDao = object : TankerDao {
        override suspend fun insert(tankerLog: TankerLog) {
            val current = tankerLogsFlow.value.toMutableList()
            current.add(tankerLog)
            tankerLogsFlow.value = current
        }
        override suspend fun delete(tankerLog: TankerLog) {
            val current = tankerLogsFlow.value.toMutableList()
            current.remove(tankerLog)
            tankerLogsFlow.value = current
        }
        override fun getAllTankerLogs(): Flow<List<TankerLog>> = tankerLogsFlow
        override fun getTankerLogsByMonth(month: Int, year: Int): Flow<List<TankerLog>> = tankerLogsFlow
        override suspend fun getCount(): Int = tankerLogsFlow.value.size
        override suspend fun getTankerLogByDate(date: String): TankerLog? = tankerLogsFlow.value.find { it.date == date }
        override suspend fun deleteByDate(date: String) {
            val current = tankerLogsFlow.value.toMutableList()
            current.removeIf { it.date == date }
            tankerLogsFlow.value = current
        }
        override suspend fun updateTankerCount(date: String, count: Int) {
            val current = tankerLogsFlow.value.toMutableList()
            val index = current.indexOfFirst { it.date == date }
            if (index != -1) {
                current[index] = current[index].copy(count = count)
                tankerLogsFlow.value = current
            }
        }
    }

    @Before
    fun setUp() {
        val vacancyRepository = VacancyRepository(fakeVacancyDao, fakeApartmentDao)
        val tankerRepository = TankerRepository(fakeTankerDao)
        val snackbarManager = com.arun.tankerapp.core.ui.SnackbarManager() // Mock or real instance
        viewModel = CalendarViewModel(vacancyRepository, tankerRepository, snackbarManager)
    }

    @Test
    fun initial_state_is_current_month() {
        // ... (rest of the test)
        val current = YearMonth.now()
        // Initialize UI State
        // ...
        assertEquals(current, viewModel.uiState.value.currentMonth)
    }

    @Test
    fun next_month_updates_state() {
        val current = YearMonth.now()
        viewModel.onNextMonth()
        assertEquals(current.plusMonths(1), viewModel.uiState.value.currentMonth)
    }

    @Test
    fun previous_month_updates_state() {
        val current = YearMonth.now()
        viewModel.onPreviousMonth()
        assertEquals(current.minusMonths(1), viewModel.uiState.value.currentMonth)
    }

    @Test
    fun onToday_resets_to_current_month() {
        // Move away first
        viewModel.onNextMonth()
        viewModel.onNextMonth()
        
        viewModel.onToday()
        
        assertEquals(YearMonth.now(), viewModel.uiState.value.currentMonth)
    }

    @Test
    fun vacanciesInMonth_updates_when_vacancy_added() = runBlocking {
        val today = LocalDate.now()
        val log = VacancyLog(apartmentId = 101, startDate = today.toString(), endDate = today.toString())
        
        // Add vacancy via Dao (simulating DB update)
        fakeVacancyDao.insert(log)
        
        // Collect first emission that is not empty
        val updated = viewModel.vacanciesInMonth.first { it.isNotEmpty() }
        
        assertEquals(1, updated.size)
        assertEquals(today, updated.first())
    }

    @Test
    fun tankerCount_updates_when_tanker_added() = runBlocking {
        val today = LocalDate.now()
        val log = TankerLog(date = today.toString(), month = today.monthValue, year = today.year, count = 3)

        // Add tanker via Dao
        fakeTankerDao.insert(log)

        // Collect first emission that is not zero
        val count = viewModel.tankerCount.first { it > 0 }

        assertEquals(3, count)
    }

    @Test
    fun tankerDatesInMonth_updates_when_tanker_added() = runBlocking {
        val today = LocalDate.now()
        val log = TankerLog(date = today.toString(), month = today.monthValue, year = today.year, count = 2)

        // Add tanker via Dao
        fakeTankerDao.insert(log)

        // Collect first emission that is not empty
        val updated = viewModel.tankerDatesInMonth.first { it.isNotEmpty() }

        assertEquals(1, updated.size)
        assertEquals(today, updated.first())
    }

    @Test
    fun currentCycleTankerCount_reflects_total_count() = runBlocking {
        val date1 = LocalDate.of(2026, 2, 10)
        val date2 = LocalDate.of(2026, 2, 15)
        
        // Add multiple tanker logs
        fakeTankerDao.insert(TankerLog(date = date1.toString(), month = 2, year = 2026, count = 3))
        fakeTankerDao.insert(TankerLog(date = date2.toString(), month = 2, year = 2026, count = 5))

        // Collect first emission that is not zero
        val totalCount = viewModel.currentCycleTankerCount.first { it > 0 }

        assertEquals(8, totalCount)
    }
}

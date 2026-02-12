package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.dao.TankerDao
import com.arun.tankerapp.core.data.database.entity.TankerLog
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

class TankerRepositoryTest {

    private lateinit var repository: TankerRepository
    private val tankerLogs = mutableListOf<TankerLog>()

    private val fakeTankerDao = object : TankerDao {
        override suspend fun insert(tankerLog: TankerLog) {
            tankerLogs.add(tankerLog)
        }

        override suspend fun delete(tankerLog: TankerLog) {
            tankerLogs.remove(tankerLog)
        }

        override fun getAllTankerLogs(): Flow<List<TankerLog>> = flowOf(tankerLogs)

        override fun getTankerLogsByMonth(month: Int, year: Int): Flow<List<TankerLog>> {
            return flowOf(tankerLogs.filter { it.month == month && it.year == year })
        }

        override suspend fun getCount(): Int = tankerLogs.size

        override suspend fun getTankerLogByDate(date: String): TankerLog? {
            return tankerLogs.find { it.date == date }
        }

        override suspend fun deleteByDate(date: String) {
            tankerLogs.removeIf { it.date == date }
        }

        override suspend fun updateTankerCount(date: String, count: Int) {
            val index = tankerLogs.indexOfFirst { it.date == date }
            if (index != -1) {
                tankerLogs[index] = tankerLogs[index].copy(count = count)
            }
        }
    }

    @Before
    fun setUp() {
        repository = TankerRepository(fakeTankerDao)
        tankerLogs.clear()
    }

    @Test
    fun incrementTankerCount_creates_new_log() = runBlocking {
        val date = LocalDate.of(2026, 2, 10)
        repository.incrementTankerCount(date)
        
        val log = tankerLogs.find { it.date == "2026-02-10" }
        assertTrue(log != null)
        assertEquals(1, log!!.count)
    }

    @Test
    fun incrementTankerCount_increases_existing_count() = runBlocking {
        val date = LocalDate.of(2026, 2, 10)
        tankerLogs.add(TankerLog(date = "2026-02-10", month = 2, year = 2026, count = 2))
        
        repository.incrementTankerCount(date)
        
        val log = tankerLogs.find { it.date == "2026-02-10" }
        assertEquals(3, log!!.count)
    }

    @Test
    fun decrementTankerCount_decreases_count() = runBlocking {
        val date = LocalDate.of(2026, 2, 10)
        tankerLogs.add(TankerLog(date = "2026-02-10", month = 2, year = 2026, count = 3))
        
        repository.decrementTankerCount(date)
        
        val log = tankerLogs.find { it.date == "2026-02-10" }
        assertEquals(2, log!!.count)
    }

    @Test
    fun decrementTankerCount_removes_log_when_zero() = runBlocking {
        val date = LocalDate.of(2026, 2, 10)
        tankerLogs.add(TankerLog(date = "2026-02-10", month = 2, year = 2026, count = 1))
        
        repository.decrementTankerCount(date)
        
        val log = tankerLogs.find { it.date == "2026-02-10" }
        assertNull(log)
    }

    @Test
    fun getTankerCount_returns_correct_count() = runBlocking {
        val date = LocalDate.of(2026, 2, 10)
        tankerLogs.add(TankerLog(date = "2026-02-10", month = 2, year = 2026, count = 5))
        
        val count = repository.getTankerCount(date).first()
        assertEquals(5, count)
    }

    @Test
    fun getTankerCount_returns_zero_if_missing() = runBlocking {
        val date = LocalDate.of(2026, 2, 10)
        val count = repository.getTankerCount(date).first()
        assertEquals(0, count)
    }

    @Test
    fun getCurrentCycleTankerCount_sums_all_tankers() = runBlocking {
        // Add multiple tanker logs with different counts
        tankerLogs.add(TankerLog(date = "2026-02-10", month = 2, year = 2026, count = 3))
        tankerLogs.add(TankerLog(date = "2026-02-15", month = 2, year = 2026, count = 2))
        tankerLogs.add(TankerLog(date = "2026-02-20", month = 2, year = 2026, count = 4))
        
        val totalCount = repository.getCurrentCycleTankerCount().first()
        assertEquals(9, totalCount)
    }

    @Test
    fun getCurrentCycleTankerCount_returns_zero_when_empty() = runBlocking {
        val totalCount = repository.getCurrentCycleTankerCount().first()
        assertEquals(0, totalCount)
    }
}

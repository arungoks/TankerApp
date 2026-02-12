package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.dao.TankerDao
import com.arun.tankerapp.core.data.database.entity.TankerLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TankerRepository @Inject constructor(
    private val tankerDao: TankerDao
) {
    /**
     * Returns a flow of tanker logs for the given month.
     */
    fun getTankersForMonth(yearMonth: YearMonth): Flow<List<TankerLog>> {
        return tankerDao.getTankerLogsByMonth(yearMonth.monthValue, yearMonth.year)
    }

    /**
     * Returns the tanker count for the given date as a Flow.
     */
    fun getTankerCount(date: LocalDate): Flow<Int> {
        val dateStr = date.toString()
        return tankerDao.getAllTankerLogs().map { logs ->
            logs.find { it.date == dateStr }?.count ?: 0
        }
    }

    /**
     * Sets the tanker count for the given date.
     * If count is 0, removes the log entry.
     */
    suspend fun setTankerCount(date: LocalDate, count: Int) {
        val dateStr = date.toString()
        val existing = tankerDao.getTankerLogByDate(dateStr)
        
        when {
            count <= 0 -> {
                // Remove entry if count is 0 or negative
                if (existing != null) {
                    tankerDao.deleteByDate(dateStr)
                }
            }
            existing == null -> {
                // Create new entry
                val log = TankerLog(
                    date = dateStr,
                    month = date.monthValue,
                    year = date.year,
                    count = count
                )
                tankerDao.insert(log)
            }
            else -> {
                // Update existing entry
                tankerDao.updateTankerCount(dateStr, count)
            }
        }
    }

    /**
     * Increments the tanker count for the given date by 1.
     */
    suspend fun incrementTankerCount(date: LocalDate) {
        val dateStr = date.toString()
        val existing = tankerDao.getTankerLogByDate(dateStr)
        val newCount = (existing?.count ?: 0) + 1
        setTankerCount(date, newCount)
    }

    /**
     * Decrements the tanker count for the given date by 1.
     * Will not go below 0.
     */
    suspend fun decrementTankerCount(date: LocalDate) {
        val dateStr = date.toString()
        val existing = tankerDao.getTankerLogByDate(dateStr)
        val currentCount = existing?.count ?: 0
        if (currentCount > 0) {
            setTankerCount(date, currentCount - 1)
        }
    }

    /**
     * Returns the total tanker count for the current billing cycle.
     * For now, this includes all tankers in the database.
     * TODO: Filter by cycle status when report generation is implemented.
     */
    fun getCurrentCycleTankerCount(): Flow<Int> {
        return tankerDao.getAllTankerLogs().map { logs ->
            logs.sumOf { it.count }
        }
    }
}

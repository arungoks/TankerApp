package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.entity.TankerLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface TankerRepository {
    fun getTankersForMonth(yearMonth: YearMonth): Flow<List<TankerLog>>
    fun getTankerCount(date: LocalDate): Flow<Int>
    suspend fun setTankerCount(date: LocalDate, count: Int)
    suspend fun incrementTankerCount(date: LocalDate)
    suspend fun decrementTankerCount(date: LocalDate)
    fun getCurrentCycleTankerCount(): Flow<Int>
}

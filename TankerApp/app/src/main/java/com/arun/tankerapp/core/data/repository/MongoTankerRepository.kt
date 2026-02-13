package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.entity.TankerLog
import com.arun.tankerapp.core.data.model.bson.TankerBson
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MongoTankerRepository @Inject constructor(
    private val database: MongoDatabase
) : TankerRepository {

    private val tankerCollection = database.getCollection<TankerBson>("tankers")
    private val ownerId = "user_1" // Hardcoded for MVP

    override fun getTankersForMonth(yearMonth: YearMonth): Flow<List<TankerLog>> = flow {
        // Construct date range queries
        val start = yearMonth.atDay(1).toString()
        val end = yearMonth.atEndOfMonth().toString()
        
        try {
            // Find tankers within date range (String comparison works for ISO dates: YYYY-MM-DD)
            val filter = Filters.and(
                Filters.gte("date", start),
                Filters.lte("date", end),
                Filters.eq("ownerId", ownerId)
            )
            
            val tankers = tankerCollection.find(filter).toList()
            
            val logs = tankers.map { bson ->
                val date = LocalDate.parse(bson.date)
                TankerLog(
                    date = bson.date,
                    month = date.monthValue,
                    year = date.year,
                    count = bson.count
                )
            }
            emit(logs)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override fun getTankerCount(date: LocalDate): Flow<Int> = flow {
        try {
            val filter = Filters.and(
                 Filters.eq("date", date.toString()),
                 Filters.eq("ownerId", ownerId)
            )
            val tanker = tankerCollection.find(filter).firstOrNull()
            emit(tanker?.count ?: 0)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(0)
        }
    }

    override suspend fun setTankerCount(date: LocalDate, count: Int) {
        val dateStr = date.toString()
        val filter = Filters.and(
            Filters.eq("date", dateStr),
            Filters.eq("ownerId", ownerId)
        )
        
        if (count <= 0) {
            tankerCollection.deleteMany(filter)
        } else {
            val bson = TankerBson(
                date = dateStr,
                count = count,
                ownerId = ownerId
            )
            // Upsert: Replace if exists, insert if not
            val options = ReplaceOptions().upsert(true)
            tankerCollection.replaceOne(filter, bson, options)
        }
    }

    override suspend fun incrementTankerCount(date: LocalDate) {
         val currentCount = getTankerCount(date).firstOrNull() ?: 0
         setTankerCount(date, currentCount + 1)
    }

    override suspend fun decrementTankerCount(date: LocalDate) {
        val currentCount = getTankerCount(date).firstOrNull() ?: 0
        if (currentCount > 0) {
            setTankerCount(date, currentCount - 1)
        }
    }

    override fun getCurrentCycleTankerCount(): Flow<Int> = flow {
        try {
            // Filter by ownerId only for now (same as Room "All Logs")
             val filter = Filters.eq("ownerId", ownerId)
             val tankers = tankerCollection.find(filter).toList()
             val total = tankers.sumOf { it.count }
             emit(total)
        } catch (e: Exception) {
             e.printStackTrace()
             emit(0)
        }
    }
}

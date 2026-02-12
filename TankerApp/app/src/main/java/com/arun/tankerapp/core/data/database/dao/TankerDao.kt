package com.arun.tankerapp.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arun.tankerapp.core.data.database.entity.TankerLog
import kotlinx.coroutines.flow.Flow

@Dao
interface TankerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tankerLog: TankerLog)

    @Delete
    suspend fun delete(tankerLog: TankerLog)

    @Query("SELECT * FROM tankers ORDER BY date DESC")
    fun getAllTankerLogs(): Flow<List<TankerLog>>

    @Query("SELECT * FROM tankers WHERE month = :month AND year = :year ORDER BY date ASC")
    fun getTankerLogsByMonth(month: Int, year: Int): Flow<List<TankerLog>>

    @Query("SELECT COUNT(*) FROM tankers")
    suspend fun getCount(): Int

    @Query("SELECT * FROM tankers WHERE date = :date LIMIT 1")
    suspend fun getTankerLogByDate(date: String): TankerLog?

    @Query("DELETE FROM tankers WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("UPDATE tankers SET count = :count WHERE date = :date")
    suspend fun updateTankerCount(date: String, count: Int)
}

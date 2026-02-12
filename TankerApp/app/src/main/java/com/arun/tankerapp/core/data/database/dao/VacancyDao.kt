package com.arun.tankerapp.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arun.tankerapp.core.data.database.entity.VacancyLog
import kotlinx.coroutines.flow.Flow

@Dao
interface VacancyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vacancyLog: VacancyLog)

    @Delete
    suspend fun delete(vacancyLog: VacancyLog)

    @Query("SELECT * FROM vacancies WHERE apartment_id = :apartmentId ORDER BY start_date DESC")
    fun getVacanciesByApartment(apartmentId: Long): Flow<List<VacancyLog>>

    @Query("SELECT * FROM vacancies ORDER BY start_date DESC")
    fun getAllVacancies(): Flow<List<VacancyLog>>

    @Query("SELECT COUNT(*) FROM vacancies")
    suspend fun getCount(): Int

    @Query("SELECT * FROM vacancies WHERE start_date <= :date AND end_date >= :date")
    fun getVacanciesForDate(date: String): Flow<List<VacancyLog>>

    @Query("SELECT * FROM vacancies WHERE apartment_id = :apartmentId AND start_date <= :date AND end_date >= :date LIMIT 1")
    suspend fun getVacancyForApartmentDate(apartmentId: Long, date: String): VacancyLog?

    @Query("SELECT * FROM vacancies WHERE start_date <= :endDate AND end_date >= :startDate")
    fun getVacanciesInRange(startDate: String, endDate: String): Flow<List<VacancyLog>>
}

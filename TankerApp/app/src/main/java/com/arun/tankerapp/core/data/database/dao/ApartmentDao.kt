package com.arun.tankerapp.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arun.tankerapp.core.data.database.entity.Apartment
import kotlinx.coroutines.flow.Flow

@Dao
interface ApartmentDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(apartments: List<Apartment>)

    @Query("SELECT * FROM apartments ORDER BY number ASC")
    fun getAllApartments(): Flow<List<Apartment>>

    @Query("SELECT COUNT(*) FROM apartments")
    suspend fun getCount(): Int
}

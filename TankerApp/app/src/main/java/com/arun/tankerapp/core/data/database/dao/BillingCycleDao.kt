package com.arun.tankerapp.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.arun.tankerapp.core.data.database.entity.BillingCycle
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface BillingCycleDao {
    @Insert
    suspend fun insert(billingCycle: BillingCycle)

    @Query("SELECT * FROM billing_cycles ORDER BY endDate DESC")
    fun getAllCycles(): Flow<List<BillingCycle>>

    @Query("SELECT * FROM billing_cycles WHERE id = :id")
    suspend fun getCycleById(id: Int): BillingCycle?
}

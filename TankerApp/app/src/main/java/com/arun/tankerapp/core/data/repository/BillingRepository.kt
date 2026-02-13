package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.entity.BillingCycle
import com.arun.tankerapp.core.data.model.ApartmentBill
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface BillingRepository {
    suspend fun archiveCurrentCycle(startDate: LocalDate, endDate: LocalDate, totalTankers: Int)
    fun getBillingHistory(): Flow<List<BillingCycle>>
    fun getBillingReport(fromDate: LocalDate? = null, toDate: LocalDate? = null): Flow<List<ApartmentBill>>
}

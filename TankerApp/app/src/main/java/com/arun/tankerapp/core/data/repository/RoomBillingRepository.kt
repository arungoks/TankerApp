package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.dao.ApartmentDao
import com.arun.tankerapp.core.data.database.dao.BillingCycleDao
import com.arun.tankerapp.core.data.database.dao.TankerDao
import com.arun.tankerapp.core.data.database.dao.VacancyDao
import com.arun.tankerapp.core.data.database.entity.BillingCycle
import com.arun.tankerapp.core.data.model.ApartmentBill
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomBillingRepository @Inject constructor(
    private val apartmentDao: ApartmentDao,
    private val tankerDao: TankerDao,
    private val vacancyDao: VacancyDao,
    private val billingCycleDao: BillingCycleDao
) : BillingRepository {

    override suspend fun archiveCurrentCycle(startDate: LocalDate, endDate: LocalDate, totalTankers: Int) {
        val cycle = BillingCycle(
            startDate = startDate,
            endDate = endDate,
            totalTankers = totalTankers
        )
        billingCycleDao.insert(cycle)
    }

    override fun getBillingHistory(): Flow<List<BillingCycle>> {
        return billingCycleDao.getAllCycles()
    }

    override fun getBillingReport(fromDate: LocalDate?, toDate: LocalDate?): Flow<List<ApartmentBill>> {
        return combine(
            apartmentDao.getAllApartments(),
            tankerDao.getAllTankerLogs(),
            vacancyDao.getAllVacancies()
        ) { apartments, tankers, vacancies ->
            
            // 1. Filter valid tankers (count > 0) AND in range
            val validTankers = tankers.filter { 
                val tDate = LocalDate.parse(it.date)
                it.count > 0 && 
                (fromDate == null || tDate.isAfter(fromDate)) &&
                (toDate == null || !tDate.isAfter(toDate))
            }
            val totalTankers = validTankers.sumOf { it.count }

            // 2. Pre-process vacancies for faster lookup
            val vacancyMap = vacancies.groupBy { it.apartmentId }

            // 3. Calculate bill for each apartment
            apartments.map { apartment ->
                val aptVacancies = vacancyMap[apartment.id] ?: emptyList()
                var billableCount = 0

                validTankers.forEach { tanker ->
                    val tankerDate = LocalDate.parse(tanker.date)
                    
                    // Check if occupied on this date
                    val isVacant = aptVacancies.any { vacancy ->
                        val start = LocalDate.parse(vacancy.startDate)
                        val end = LocalDate.parse(vacancy.endDate)
                        !tankerDate.isBefore(start) && !tankerDate.isAfter(end)
                    }

                    if (!isVacant) {
                        billableCount += tanker.count
                    }
                }

                ApartmentBill(
                    apartment = apartment,
                    billableTankers = billableCount,
                    totalTankersInCycle = totalTankers
                )
            }.sortedBy { it.apartment.id }
        }
    }
}

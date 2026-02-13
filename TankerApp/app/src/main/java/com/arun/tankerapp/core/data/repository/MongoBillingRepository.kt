package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.entity.Apartment
import com.arun.tankerapp.core.data.database.entity.BillingCycle
// Removed invalid import

import com.arun.tankerapp.core.data.model.ApartmentBill
import com.arun.tankerapp.core.data.model.bson.ApartmentBson
import com.arun.tankerapp.core.data.model.bson.BillingCycleBson
import com.arun.tankerapp.core.data.model.bson.TankerBson
import com.arun.tankerapp.core.data.model.bson.VacancyBson
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MongoBillingRepository @Inject constructor(
    private val database: MongoDatabase
) : BillingRepository {

    private val apartmentCollection = database.getCollection<ApartmentBson>("apartments")
    private val tankerCollection = database.getCollection<TankerBson>("tankers")
    private val vacancyCollection = database.getCollection<VacancyBson>("vacancies")
    private val billingCycleCollection = database.getCollection<BillingCycleBson>("billing_cycles")

    override suspend fun archiveCurrentCycle(startDate: LocalDate, endDate: LocalDate, totalTankers: Int) {
        val cycle = BillingCycleBson(
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            isActive = false, // Archived cycles are inactive
            ownerId = "hardcoded_user" // Placeholder
        )
        billingCycleCollection.insertOne(cycle)
    }

    override fun getBillingHistory(): Flow<List<BillingCycle>> = flow {
         try {
             val bsonList = billingCycleCollection.find<BillingCycleBson>().toList()
             val entities = bsonList.map { bson ->
                BillingCycle(
                    id = bson.id.hashCode(), // Temporary Int ID for Room compatibility
                    startDate = LocalDate.parse(bson.startDate),
                    endDate = LocalDate.parse(bson.endDate),
                    totalTankers = 0 // Needs to be calculated or stored
                )
            }
            emit(entities)
         } catch (e: Exception) {
             e.printStackTrace()
             emit(emptyList())
         }
    }

    /**
     * Generates billing report by combining Cloud Streams.
     */
    override fun getBillingReport(fromDate: LocalDate?, toDate: LocalDate?): Flow<List<ApartmentBill>> {
        return combine(
            getAllApartmentsFlow(),
            getAllTankersFlow(),
            getAllVacanciesFlow()
        ) { apartments, tankers, vacancies ->

            // 1. Filter valid tankers (count > 0 not in Bson yet, assume 1) AND in range
            val validTankers = tankers.filter {
                val tDate = LocalDate.parse(it.date)
                (fromDate == null || tDate.isAfter(fromDate)) &&
                (toDate == null || !tDate.isAfter(toDate))
            }
            val totalTankers = validTankers.size

            // 2. Pre-process vacancies
            val vacancyMap = vacancies.groupBy { it.apartmentId }

            // 3. Calculate bill for each apartment
            apartments.map { apartment ->
                val aptVacancies = vacancyMap[apartment.id.toInt()] ?: emptyList()
                var billableCount = 0

                validTankers.forEach { tanker ->
                    val tankerDate = LocalDate.parse(tanker.date)
                    
                    // Check if occupied
                    val isVacant = aptVacancies.any { vacancy ->
                        val start = LocalDate.parse(vacancy.startDate)
                        val end = vacancy.endDate?.let { LocalDate.parse(it) } ?: LocalDate.MAX
                        !tankerDate.isBefore(start) && !tankerDate.isAfter(end)
                    }

                    if (!isVacant) {
                        billableCount++
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

    // --- Helper Flows (One-Shot Wrappers) ---

    private fun getAllApartmentsFlow(): Flow<List<Apartment>> = flow {
        try {
            val bsonList = apartmentCollection.find<ApartmentBson>().toList()
            val entities = bsonList.map { bson ->
                Apartment(
                    id = (bson.number.toIntOrNull() ?: bson.hashCode()).toLong(),
                    number = bson.number
                ) 
                // Note: Residents are embedded in Bson but Apartment entity might need mapping if used elsewhere
            }
            emit(entities)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    private fun getAllTankersFlow(): Flow<List<TankerBson>> = flow {
         try {
             emit(tankerCollection.find<TankerBson>().toList())
         } catch (e: Exception) {
             e.printStackTrace()
             emit(emptyList())
         }
    }

    private fun getAllVacanciesFlow(): Flow<List<VacancyBson>> = flow {
        try {
            emit(vacancyCollection.find<VacancyBson>().toList())
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }
}

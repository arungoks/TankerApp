package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.entity.Apartment
import com.arun.tankerapp.core.data.database.entity.BillingCycle
import com.arun.tankerapp.core.data.model.firestore.BillingCycleDocument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class ApartmentBill(
    val apartment: Apartment,
    val billableTankers: Int,
    val totalTankersInCycle: Int,
    val dailyBreakdown: Map<LocalDate, Int> = emptyMap(),
    val dailyOccupancyBreakdown: Map<LocalDate, Int> = emptyMap()
)

@Singleton
class BillingRepository @Inject constructor(
    private val appFirestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val tankerRepository: TankerRepository,
    private val vacancyRepository: VacancyRepository
) {
    private val billingCyclesCollection = appFirestore.collection("billing_cycles")

    /**
     * Archives the current billing cycle.
     */
    suspend fun archiveCurrentCycle(startDate: LocalDate, endDate: LocalDate, totalTankers: Int) {
        val startStr = startDate.toString()
        val docId = startStr // Use Start Date as ID? Or generated ID? StartDate is unique enough.
        
        val cycleDoc = BillingCycleDocument(
            id = docId,
            startDate = startStr,
            endDate = endDate.toString(),
            totalTankers = totalTankers,
            ownerId = "Global"
        )
        billingCyclesCollection.document(docId).set(cycleDoc).await()
    }

    fun getBillingHistory(): Flow<List<BillingCycle>> = callbackFlow {
        val registration = billingCyclesCollection.addSnapshotListener { snapshot, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            
            val cycles = snapshot?.documents?.mapNotNull { doc ->
                val data = doc.toObject(BillingCycleDocument::class.java)
                data?.let {
                    BillingCycle(
                        id = it.id.hashCode(), // Use Int hash code
                        startDate = LocalDate.parse(it.startDate),
                        endDate = LocalDate.parse(it.endDate),
                        totalTankers = it.totalTankers
                    )
                }
            } ?: emptyList()
            trySend(cycles)
            trySend(cycles)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Generates a billing report for a given period.
     */
    fun getBillingReport(fromDate: LocalDate? = null, toDate: LocalDate? = null): Flow<List<ApartmentBill>> {
        return combine(
            vacancyRepository.getApartmentDocuments(),
            tankerRepository.getAllTankers(),
            vacancyRepository.getAllVacancies(),
            vacancyRepository.getAllDailyOccupancies()
        ) { aptDocs, tankers, vacancies, dailyOccupancies ->
            
            // 1. Filter valid tankers (count > 0) AND in range
            val validTankers = tankers.filter { 
                val tDate = LocalDate.parse(it.date)
                it.count > 0 && 
                (fromDate == null || tDate.isAfter(fromDate)) &&
                (toDate == null || !tDate.isAfter(toDate))
            }
            val totalTankers = validTankers.sumOf { it.count }

            // 2. Pre-process vacancies for faster lookup
            // VacancyLog uses Long IDs (hashCode of apartment number)
            val vacancyMap = vacancies.groupBy { it.apartmentId }
            
            // Daily Occupancy uses String IDs (apartment number)
            val dailyMap = dailyOccupancies.groupBy { it.apartmentId }

            // 3. Calculate bill for each apartment
            aptDocs.map { doc ->
                val aptNumber = doc.number
                val aptIdLong = aptNumber.hashCode().toLong()
                
                val aptVacancies = vacancyMap[aptIdLong] ?: emptyList()
                val aptDailyOccupancies = dailyMap[aptNumber] ?: emptyList()
                val defaultOcc = doc.defaultOccupancy
                
                val dailyBreakdown = mutableMapOf<LocalDate, Int>()
                val dailyOccupancyBreakdown = mutableMapOf<LocalDate, Int>()
                var billableCount = 0

                validTankers.forEach { tanker ->
                    val tankerDate = LocalDate.parse(tanker.date)
                    val dateStr = tanker.date
                    
                    // Check explicit vacancy
                    val isExplicitlyVacant = aptVacancies.any { vacancy ->
                        val start = LocalDate.parse(vacancy.startDate)
                        val end = if (vacancy.endDate.isNotEmpty()) LocalDate.parse(vacancy.endDate) else null
                        
                        !tankerDate.isBefore(start) && (end == null || !tankerDate.isAfter(end))
                    }
                    
                    // Check Effective Occupancy
                    val override = aptDailyOccupancies.find { it.date == dateStr }?.occupancy
                    val count = override ?: defaultOcc
                    val isEffectiveVacant = isExplicitlyVacant || (count == 0)
                    
                    // Record Occupancy (0 if vacant)
                    val effectiveOccupancy = if (isEffectiveVacant) 0 else count
                    dailyOccupancyBreakdown[tankerDate] = effectiveOccupancy

                    if (!isEffectiveVacant) {
                        dailyBreakdown[tankerDate] = (dailyBreakdown[tankerDate] ?: 0) + tanker.count
                        billableCount += tanker.count
                    }
                }

                ApartmentBill(
                    apartment = Apartment(id = aptIdLong, number = aptNumber),
                    billableTankers = billableCount,
                    totalTankersInCycle = totalTankers,
                    dailyBreakdown = dailyBreakdown,
                    dailyOccupancyBreakdown = dailyOccupancyBreakdown
                )
            }.sortedBy { it.apartment.id } // Or sort by number logic if needed
        }
    }
}

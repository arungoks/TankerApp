package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.MasterApartmentList
import com.arun.tankerapp.core.data.database.entity.Apartment
import com.arun.tankerapp.core.data.database.entity.VacancyLog

import com.arun.tankerapp.core.data.model.firestore.ApartmentDocument
import com.arun.tankerapp.core.data.model.firestore.VacancyDocument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreVacancyRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    auth: FirebaseAuth, // Injected but not used for queries (public/admin read)
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : VacancyRepository {

    private val apartmentsCollection = firestore.collection("apartments")
    private val vacanciesCollection = firestore.collection("vacancies")
    private val dailyOccupancyCollection = firestore.collection("daily_occupancy")

    init {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            seedApartments()
            seedDefaultOccupancy()
        }
    }

    private suspend fun seedApartments() {
        try {
            val snapshot = apartmentsCollection.limit(1).get().await()
            if (snapshot.isEmpty) {
                val batch = firestore.batch()
                MasterApartmentList.apartments.forEach { number ->
                    val docRef = apartmentsCollection.document(number)
                    val apartment = ApartmentDocument(
                        id = number, 
                        number = number, 
                        ownerId = "Global"
                    )
                    batch.set(docRef, apartment)
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun seedDefaultOccupancy() {
        try {
            val inputStream = context.assets.open("apartment-occupancy.csv")
            val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))
            reader.readLine() // Skip header
            
            val occupancyMap = mutableMapOf<String, Int>()
            var line = reader.readLine()
            while (line != null) {
                val parts = line.split(",")
                if (parts.size >= 2) {
                    val apt = parts[0].trim()
                    val count = parts[1].trim().toIntOrNull() ?: 0
                    occupancyMap[apt] = count
                }
                line = reader.readLine()
            }
            reader.close()

            val snapshot = apartmentsCollection.get().await()
            val docs = snapshot.documents
            
            val batch = firestore.batch()
            var hasUpdates = false

            occupancyMap.forEach { (aptNumber, defaultCount) ->
                val doc = docs.find { it.id == aptNumber }
                if (doc != null) {
                    val currentDefault = doc.getLong("defaultOccupancy")?.toInt() ?: 0
                    if (currentDefault == 0 && defaultCount > 0) {
                         batch.update(doc.reference, "defaultOccupancy", defaultCount)
                         hasUpdates = true
                    }
                }
            }
            
            if (hasUpdates) {
                batch.commit().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val apartmentsFlow: Flow<List<Apartment>> = callbackFlow {
        val registration = apartmentsCollection.addSnapshotListener { snapshot, _ ->
            val docs = snapshot?.toObjects(ApartmentDocument::class.java) ?: emptyList()
            trySend(docs.map { Apartment(id = it.number.hashCode().toLong(), number = it.number) })
        }
        awaitClose { registration.remove() }
    }
    
    // Internal flow including Document fields like defaultOccupancy
    private val apartmentDocsFlow: Flow<List<ApartmentDocument>> = callbackFlow {
        val registration = apartmentsCollection.addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.toObjects(ApartmentDocument::class.java) ?: emptyList())
        }
        awaitClose { registration.remove() }
    }

    private val allVacanciesFlow: Flow<List<VacancyDocument>> = callbackFlow {
        val registration = vacanciesCollection.addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.toObjects(VacancyDocument::class.java) ?: emptyList())
        }
        awaitClose { registration.remove() }
    }

    private val allDailyOccupanciesFlow: Flow<List<com.arun.tankerapp.core.data.model.firestore.DailyOccupancyDocument>> = callbackFlow {
        val registration = dailyOccupancyCollection.addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.toObjects(com.arun.tankerapp.core.data.model.firestore.DailyOccupancyDocument::class.java) ?: emptyList())
        }
        awaitClose { registration.remove() }
    }

    override fun getAllApartments(): Flow<List<Apartment>> = apartmentsFlow
    override fun getApartmentDocuments() = apartmentDocsFlow
    override fun getAllDailyOccupancies() = allDailyOccupanciesFlow

    override fun getAllVacancies(): Flow<List<VacancyLog>> = allVacanciesFlow.map { docs ->
        docs.map { vac ->
            VacancyLog(
                 id = vac.id.hashCode().toLong(),
                 apartmentId = vac.apartmentId.hashCode().toLong(),
                 startDate = vac.startDate,
                 endDate = vac.startDate
            )
        }
    }

    override fun getApartmentStatuses(date: LocalDate): Flow<List<ApartmentStatus>> {
        val dateStr = date.toString()
        
        // Flow for Daily Occupancy for THIS date
        val dailyFlow = callbackFlow {
            val registration = dailyOccupancyCollection.whereEqualTo("date", dateStr)
                .addSnapshotListener { snapshot, _ ->
                    trySend(snapshot?.toObjects(com.arun.tankerapp.core.data.model.firestore.DailyOccupancyDocument::class.java) ?: emptyList())
                }
            awaitClose { registration.remove() }
        }

        return combine(apartmentDocsFlow, allVacanciesFlow, dailyFlow) { aptDocs, vacancies, dailyDocs ->
            
            // Map Daily Overrides
            val overrides = dailyDocs.associate { it.apartmentId to it.occupancy }
            
            // Filter Vacancies for this date
            val vacantApartmentNumbers = vacancies.filter { 
                it.startDate == dateStr 
            }.map { it.apartmentId }.toSet()

            aptDocs.map { doc ->
                val aptNumber = doc.number
                val isExplicitlyVacant = vacantApartmentNumbers.contains(aptNumber)
                
                // Effective Count Logic:
                // 1. Get Base Count (Override or Default)
                val baseCount = overrides[aptNumber] ?: doc.defaultOccupancy
                
                // 2. Effective Vacancy = Explicitly Vacant OR Base Count is 0
                val isEffectiveVacant = isExplicitlyVacant || (baseCount == 0)
                
                // 3. Final Count = 0 if Vacant, else Base Count
                val count = if (isEffectiveVacant) 0 else baseCount

                ApartmentStatus(
                    apartment = Apartment(id = doc.number.hashCode().toLong(), number = doc.number),
                    isVacant = isEffectiveVacant,
                    occupancy = count
                )
            }.sortedBy { it.apartment.number.toIntOrNull() ?: Int.MAX_VALUE }
        }
    }

    override fun getVacanciesForMonth(yearMonth: YearMonth): Flow<List<VacancyLog>> {
        val startOfMonth = yearMonth.atDay(1).toString()
        val endOfMonth = yearMonth.atEndOfMonth().toString()

        return allVacanciesFlow.map { vacancies ->
             vacancies.filter { vac ->
                 vac.startDate >= startOfMonth && vac.startDate <= endOfMonth
             }.map { vac ->
                 VacancyLog(
                     id = vac.id.hashCode().toLong(),
                     apartmentId = vac.apartmentId.hashCode().toLong(),
                     startDate = vac.startDate,
                     endDate = vac.startDate
                 )
             }
        }
    }

    override suspend fun toggleVacancy(apartmentId: Long, date: LocalDate, isVacant: Boolean) {
        val aptSnapshot = apartmentsCollection.get().await()
        val apartments = aptSnapshot.toObjects(ApartmentDocument::class.java)
        val targetApt = apartments.find { it.number.hashCode().toLong() == apartmentId } ?: return

        val apartmentNumber = targetApt.number
        val dateStr = date.toString()

        val vacancySnapshot = vacanciesCollection
            .whereEqualTo("apartmentId", apartmentNumber)
            .whereEqualTo("startDate", dateStr)
            .get()
            .await()

        val batch = firestore.batch()

        if (isVacant) {
            if (vacancySnapshot.isEmpty) {
                val newDocRef = vacanciesCollection.document()
                val newVacancy = VacancyDocument(
                    id = newDocRef.id,
                    apartmentId = apartmentNumber,
                    startDate = dateStr,
                    endDate = dateStr,
                    ownerId = "Global"
                )
                batch.set(newDocRef, newVacancy)
                
                // Note: We don't need to explicitly zero daily_occupancy here because getApartmentStatuses logic handles it.
                // However, to keep store clean, we COULD delete any override for this date.
                // But preserving it allows restoring count if toggled back?
                // Logic says: "If Vacant, override daily occupancy to 0." -> In UI/Logic.
                // If I toggle Vacant off, count should restore to default (or override if exists).
                // So leaving daily_occupancy implies it persists across vacancy toggle?
                // Valid assumption.
            }
        } else {
            vacancySnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
        }
        
        batch.commit().await()
    }
    
    override suspend fun updateOccupancy(apartmentId: Long, date: LocalDate, count: Int) {
        val aptSnapshot = apartmentsCollection.get().await() // Inefficient lookup by ID hash
        val apartments = aptSnapshot.toObjects(ApartmentDocument::class.java)
        val targetApt = apartments.find { it.number.hashCode().toLong() == apartmentId } ?: return
        
        val apartmentNumber = targetApt.number
        val defaultOcc = targetApt.defaultOccupancy
        val dateStr = date.toString()
        val dailyDocId = "${apartmentNumber}_$dateStr"
        
        val batch = firestore.batch()

        // Check existing vacancy status
        val vacancySnapshot = vacanciesCollection
            .whereEqualTo("apartmentId", apartmentNumber)
            .whereEqualTo("startDate", dateStr)
            .get()
            .await()

        if (count == 0) {
            // 1. Ensure Vacancy Exists
            if (vacancySnapshot.isEmpty) {
                val newDocRef = vacanciesCollection.document()
                val newVacancy = VacancyDocument(
                    id = newDocRef.id,
                    apartmentId = apartmentNumber,
                    startDate = dateStr,
                    endDate = dateStr,
                    ownerId = "Global"
                )
                batch.set(newDocRef, newVacancy)
            }
            // 2. Remove any daily occupancy override (Vacancy implies 0)
            batch.delete(dailyOccupancyCollection.document(dailyDocId))
        } else {
            // 1. Ensure Vacancy Removed
            vacancySnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            // 2. Set/Update Daily Occupancy Override
            if (count == defaultOcc) {
                // If count matches default, we don't need an override
                batch.delete(dailyOccupancyCollection.document(dailyDocId))
            } else {
                val doc = com.arun.tankerapp.core.data.model.firestore.DailyOccupancyDocument(
                    id = dailyDocId,
                    apartmentId = apartmentNumber,
                    date = dateStr,
                    occupancy = count,
                    ownerId = "Global"
                )
                batch.set(dailyOccupancyCollection.document(dailyDocId), doc)
            }
        }
        
        batch.commit().await()
    }
}

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
    auth: FirebaseAuth // Injected but not used for queries (public/admin read)
) : VacancyRepository {

    private val apartmentsCollection = firestore.collection("apartments")
    private val vacanciesCollection = firestore.collection("vacancies")

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedApartments()
        }
    }

    private suspend fun seedApartments() {
        try {
            val snapshot = apartmentsCollection.limit(1).get().await()
            if (snapshot.isEmpty) {
                val batch = firestore.batch()
                MasterApartmentList.apartments.forEach { number ->
                    val docRef = apartmentsCollection.document(number)
                    // Use number as ID
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

    // Reactive flow for all apartments
    private val apartmentsFlow: Flow<List<Apartment>> = callbackFlow {
        val registration = apartmentsCollection.addSnapshotListener { snapshot, _ ->
            val docs = snapshot?.toObjects(ApartmentDocument::class.java) ?: emptyList()
            trySend(docs.map { Apartment(id = it.number.hashCode().toLong(), number = it.number) })
        }
        awaitClose { registration.remove() }
    }

    // Reactive flow for all vacancies (optimized to fetch once/listen)
    private val allVacanciesFlow: Flow<List<VacancyDocument>> = callbackFlow {
        val registration = vacanciesCollection.addSnapshotListener { snapshot, _ ->
            val docs = snapshot?.toObjects(VacancyDocument::class.java) ?: emptyList()
            trySend(docs)
        }
        awaitClose { registration.remove() }
    }

    override fun getAllApartments(): Flow<List<Apartment>> = apartmentsFlow

    override fun getAllVacancies(): Flow<List<VacancyLog>> = allVacanciesFlow.map { docs ->
        docs.map { vac ->
            VacancyLog(
                 id = vac.id.hashCode().toLong(),
                 apartmentId = vac.apartmentId.hashCode().toLong(),
                 startDate = vac.startDate,
                 endDate = vac.startDate // Ranges removed, effectively single day
            )
        }
    }

    override fun getApartmentStatuses(date: LocalDate): Flow<List<ApartmentStatus>> {
        return combine(apartmentsFlow, allVacanciesFlow) { apartments, vacancies ->
            val dateStr = date.toString()
            
            // Filter vacancies for EXACTLY this date (Single Day logic)
            // We ignore endDate. Only startDate matters.
            val activeVacancies = vacancies.filter { vac ->
                vac.startDate == dateStr
            }
            
            val vacantApartmentNumbers = activeVacancies.map { it.apartmentId }.toSet()

            apartments.map { apt ->
                ApartmentStatus(
                    apartment = apt,
                    isVacant = vacantApartmentNumbers.contains(apt.number)
                )
            }.sortedBy { it.apartment.number.toIntOrNull() ?: Int.MAX_VALUE }
        }
    }

    override fun getVacanciesForMonth(yearMonth: YearMonth): Flow<List<VacancyLog>> {
        val startOfMonth = yearMonth.atDay(1).toString()
        val endOfMonth = yearMonth.atEndOfMonth().toString()

        return allVacanciesFlow.map { vacancies ->
             vacancies.filter { vac ->
                 // Simple range check: Is startDate within this month?
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
        // We need to map Long ID back to String Number.
        // This is inefficient. Ideally Domain should pass String Number.
        // Hack: Fetch apartment by ID (fetch all and find).
        val aptSnapshot = apartmentsCollection.get().await()
        val apartments = aptSnapshot.toObjects(ApartmentDocument::class.java)
        val targetApt = apartments.find { it.number.hashCode().toLong() == apartmentId } ?: return // Apt not found

        val apartmentNumber = targetApt.number
        val dateStr = date.toString()

        // Find existing vacancies for this apartment on this date
        // Single Day Logic: Query vacancy for THIS apartment on THIS specific date
        // Note: We use startDate equality. 
        val vacancySnapshot = vacanciesCollection
            .whereEqualTo("apartmentId", apartmentNumber)
            .whereEqualTo("startDate", dateStr)
            .get()
            .await()

        val batch = firestore.batch()

        if (isVacant) {
            // If empty, create. If exists, do nothing (already vacant)
            if (vacancySnapshot.isEmpty) {
                val newDocRef = vacanciesCollection.document()
                val newVacancy = VacancyDocument(
                    id = newDocRef.id,
                    apartmentId = apartmentNumber,
                    startDate = dateStr,
                    endDate = dateStr, // Explicitly single day
                    ownerId = "Global"
                )
                batch.set(newDocRef, newVacancy)
            }
        } else {
            // Delete if exists
            vacancySnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
        }
        
        batch.commit().await()
    }
}

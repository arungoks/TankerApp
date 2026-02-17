package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.entity.TankerLog
import com.arun.tankerapp.core.data.model.firestore.TankerDocument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TankerRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    auth: FirebaseAuth
) {
    private val tankersCollection = firestore.collection("tankers")

    /**
     * Returns a flow of tanker logs for the given month.
     */
    fun getTankersForMonth(yearMonth: YearMonth): Flow<List<TankerLog>> = callbackFlow {
        // Query by month and year
        val registration = tankersCollection
            .whereEqualTo("month", yearMonth.monthValue)
            .whereEqualTo("year", yearMonth.year)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { close(e); return@addSnapshotListener }
                
                val docs = snapshot?.toObjects(TankerDocument::class.java) ?: emptyList()
                val logs = docs.map { 
                    TankerLog(
                        // Use date string hash or something unique. Firestore ID is date string.
                        id = it.id.hashCode().toLong(), 
                        date = it.date,
                        month = it.month,
                        year = it.year,
                        count = it.count
                    )
                }
                trySend(logs)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Returns the tanker count for the given date as a Flow.
     */
    fun getTankerCount(date: LocalDate): Flow<Int> = callbackFlow {
        val dateStr = date.toString()
        val docRef = tankersCollection.document(dateStr)
        
        val registration = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            
            if (snapshot != null && snapshot.exists()) {
                val doc = snapshot.toObject(TankerDocument::class.java)
                trySend(doc?.count ?: 0)
            } else {
                trySend(0)
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Sets the tanker count for the given date.
     * If count is 0, removes the log entry.
     */
    suspend fun setTankerCount(date: LocalDate, count: Int) {
        val dateStr = date.toString()
        val docRef = tankersCollection.document(dateStr)
        
        if (count <= 0) {
            docRef.delete().await()
        } else {
            val doc = TankerDocument(
                id = dateStr, // Use date as ID
                date = dateStr,
                month = date.monthValue,
                year = date.year,
                count = count,
                ownerId = "Global"
            )
            docRef.set(doc).await()
        }
    }

    /**
     * Increments the tanker count for the given date by 1.
     */
    suspend fun incrementTankerCount(date: LocalDate) {
        val dateStr = date.toString()
        val docRef = tankersCollection.document(dateStr)
        
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentCount = if (snapshot.exists()) {
                snapshot.toObject(TankerDocument::class.java)?.count ?: 0
            } else {
                0
            }
            val newCount = currentCount + 1
            
            val doc = TankerDocument(
                id = dateStr,
                date = dateStr,
                month = date.monthValue,
                year = date.year,
                count = newCount,
                ownerId = "Global"
            )
            transaction.set(docRef, doc)
        }.await()
    }

    /**
     * Decrements the tanker count for the given date by 1.
     * Will not go below 0.
     */
    suspend fun decrementTankerCount(date: LocalDate) {
         val dateStr = date.toString()
         val docRef = tankersCollection.document(dateStr)
         
         firestore.runTransaction { transaction ->
             val snapshot = transaction.get(docRef)
             if (!snapshot.exists()) return@runTransaction 
             
             val currentCount = snapshot.toObject(TankerDocument::class.java)?.count ?: 0
             if (currentCount > 0) {
                 val newCount = currentCount - 1
                 if (newCount == 0) {
                     transaction.delete(docRef)
                 } else {
                     transaction.update(docRef, "count", newCount)
                 }
             }
         }.await()
    }


    /**
     * Returns the total tanker count for the current billing cycle.
     * For now, this includes all tankers in the database.
     * TODO: Filter by cycle status when report generation is implemented.
     */
    fun getCurrentCycleTankerCount(): Flow<Int> = callbackFlow {
        // Fetch ALL tankers
        // Ideally: collection("tankers").whereEqualTo("billingCycleId", currentCycleId)
        // Since we don't have cycles yet, fetch ALL.
        val registration = tankersCollection.addSnapshotListener { snapshot, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            
            val total = snapshot?.documents?.sumOf { 
                it.toObject(TankerDocument::class.java)?.count ?: 0 
            } ?: 0
            trySend(total)
        }
        awaitClose { registration.remove() }
    }

    fun getAllTankers(): Flow<List<TankerLog>> = callbackFlow {
        val registration = tankersCollection.addSnapshotListener { snapshot, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            
            val logs = snapshot?.documents?.mapNotNull { doc ->
                val data = doc.toObject(TankerDocument::class.java)
                data?.let {
                    TankerLog(
                        id = it.id.hashCode().toLong(),
                        date = it.date,
                        month = it.month,
                        year = it.year,
                        count = it.count
                    )
                }
            } ?: emptyList()
            trySend(logs)
        }
        awaitClose { registration.remove() }
    }
}

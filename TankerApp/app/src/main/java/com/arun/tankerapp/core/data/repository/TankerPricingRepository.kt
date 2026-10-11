package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.model.TankerPrice
import com.arun.tankerapp.core.data.model.firestore.TankerPriceDocument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TankerPricingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val pricingCollection = firestore.collection("tanker_prices")

    /**
     * Observes all tanker pricing history sorted by effective date descending (most recent first).
     */
    fun getPricingHistory(): Flow<List<TankerPrice>> = callbackFlow {
        val registration = pricingCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val docs = snapshot?.documents ?: emptyList()
            val prices = docs.mapNotNull { doc ->
                val data = doc.toObject(TankerPriceDocument::class.java)
                data?.let {
                    try {
                        val effDate = LocalDate.parse(it.effectiveDate)
                        TankerPrice(
                            id = doc.id,
                            effectiveDate = effDate,
                            price = it.price
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }.sortedByDescending { it.effectiveDate }

            trySend(prices)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Sets or updates the tanker price for a given effective date.
     * Uses the date string as Document ID for idempotency.
     */
    suspend fun setTankerPrice(effectiveDate: LocalDate, price: Double) {
        val dateStr = effectiveDate.toString()
        val doc = TankerPriceDocument(
            id = dateStr,
            effectiveDate = dateStr,
            price = price,
            ownerId = auth.currentUser?.uid ?: "Global"
        )
        pricingCollection.document(dateStr).set(doc).await()
    }

    /**
     * Deletes a tanker price record by effective date.
     */
    suspend fun deleteTankerPrice(effectiveDate: LocalDate) {
        pricingCollection.document(effectiveDate.toString()).delete().await()
    }

    companion object {
        /**
         * Resolves the effective price for a given date from the price history.
         * Looks for the latest entry with effectiveDate <= date.
         * If the date precedes all recorded prices, falls back to the earliest recorded price.
         */
        fun getPriceForDate(date: LocalDate, priceList: List<TankerPrice>): Double {
            if (priceList.isEmpty()) return 0.0
            val effective = priceList
                .filter { !it.effectiveDate.isAfter(date) }
                .maxByOrNull { it.effectiveDate }

            return effective?.price ?: priceList.minByOrNull { it.effectiveDate }?.price ?: 0.0
        }
    }
}

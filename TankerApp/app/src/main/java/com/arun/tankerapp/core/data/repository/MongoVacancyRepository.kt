package com.arun.tankerapp.core.data.repository

import com.arun.tankerapp.core.data.database.entity.Apartment
import com.arun.tankerapp.core.data.database.entity.VacancyLog
import com.arun.tankerapp.core.data.model.ApartmentStatus
import com.arun.tankerapp.core.data.model.bson.ApartmentBson
import com.arun.tankerapp.core.data.model.bson.VacancyBson
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import java.time.LocalDate
import java.time.YearMonth
import com.arun.tankerapp.core.data.database.MasterApartmentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MongoVacancyRepository @Inject constructor(
    private val database: MongoDatabase
) : VacancyRepository {

    private val vacancyCollection = database.getCollection<VacancyBson>("vacancies")
    private val apartmentCollection = database.getCollection<ApartmentBson>("apartments")
    private val ownerId = "user_1"

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("MongoVacancyRepository", "Checking for data seeding...")
                val count = apartmentCollection.countDocuments()
                if (count == 0L) {
                    android.util.Log.d("MongoVacancyRepository", "Seeding apartments...")
                    val apartments = MasterApartmentList.apartments.map { number ->
                        ApartmentBson(
                            number = number,
                            ownerId = ownerId
                        )
                    }
                    apartmentCollection.insertMany(apartments)
                    android.util.Log.d("MongoVacancyRepository", "Seeding complete.")
                } else {
                    android.util.Log.d("MongoVacancyRepository", "Apartments already exist: $count")
                }
            } catch (e: Exception) {
                android.util.Log.e("MongoVacancyRepository", "Seeding failed", e)
                e.printStackTrace()
            }
        }
    }

    override fun getApartmentStatuses(date: LocalDate): Flow<List<ApartmentStatus>> = flow {
        val dateStr = date.toString()

        try {
            // 1. Fetch Apartments
            // Always try to fetch from cloud. If empty, the UI will just show empty list (unless we seed).
            val apartmentBsons = apartmentCollection.find(Filters.eq("ownerId", ownerId)).toList()
            
            val apartments = apartmentBsons.map { bson ->
                Apartment(
                    id = (bson.number.toIntOrNull() ?: bson.hashCode()).toLong(),
                    number = bson.number
                )
            }

            // 2. Fetch Vacancies for Date
            val dateFilter = Filters.and(
                 Filters.lte("startDate", dateStr),
                 Filters.or(
                     Filters.eq("endDate", null),
                     Filters.gte("endDate", dateStr)
                 ),
                 Filters.eq("ownerId", ownerId)
            )
            val vacancies = vacancyCollection.find(dateFilter).toList()
            val vacantIds = vacancies.map { it.apartmentId }.toSet()

            val statuses = apartments.map { apt ->
                ApartmentStatus(
                    apartment = apt,
                    isVacant = vacantIds.contains(apt.id.toInt()) // Cast Long id (from number hash) back to Int
                )
            }.sortedBy { it.apartment.number.toIntOrNull() ?: Int.MAX_VALUE }

            emit(statuses)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override fun getVacanciesForMonth(yearMonth: YearMonth): Flow<List<VacancyLog>> = flow {
         val startOfMonth = yearMonth.atDay(1).toString()
         val endOfMonth = yearMonth.atEndOfMonth().toString()
         
         try {
             val filter = Filters.and(
                 Filters.lte("startDate", endOfMonth),
                 Filters.or(
                     Filters.eq("endDate", null),
                     Filters.gte("endDate", startOfMonth)
                 ),
                 Filters.eq("ownerId", ownerId)
             )
             
             val vacancies = vacancyCollection.find(filter).toList()
             
             val logs = vacancies.map { bson ->
                 VacancyLog(
                     id = bson.id.hashCode().toLong(),
                     apartmentId = bson.apartmentId.toLong(),
                     startDate = bson.startDate,
                     endDate = bson.endDate ?: ""
                 )
             }
             emit(logs)
         } catch (e: Exception) {
             e.printStackTrace()
             emit(emptyList())
         }
    }

    override suspend fun toggleVacancy(apartmentId: Long, date: LocalDate, isVacant: Boolean) {
        val dateStr = date.toString()
        val aptIdInt = apartmentId.toInt()
        
        val filter = Filters.and(
             Filters.eq("apartmentId", aptIdInt),
             Filters.lte("startDate", dateStr),
             Filters.or(
                 Filters.eq("endDate", null),
                 Filters.gte("endDate", dateStr)
             ),
             Filters.eq("ownerId", ownerId)
        )
        
        val existing = vacancyCollection.find(filter).firstOrNull()

        if (isVacant) {
            if (existing == null) {
                vacancyCollection.insertOne(
                    VacancyBson(
                        apartmentId = aptIdInt,
                        startDate = dateStr,
                        endDate = dateStr,
                        ownerId = ownerId
                    )
                )
            }
        } else {
            if (existing != null) {
                 vacancyCollection.deleteOne(Filters.eq("_id", existing.id))
            }
        }
    }
}

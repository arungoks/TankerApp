package com.arun.tankerapp.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.arun.tankerapp.core.data.database.dao.ApartmentDao
import com.arun.tankerapp.core.data.database.dao.TankerDao
import com.arun.tankerapp.core.data.database.dao.VacancyDao
import com.arun.tankerapp.core.data.database.entity.Apartment
import com.arun.tankerapp.core.data.database.entity.TankerLog
import com.arun.tankerapp.core.data.database.entity.VacancyLog
import com.arun.tankerapp.core.data.database.dao.BillingCycleDao
import com.arun.tankerapp.core.data.database.entity.BillingCycle

@Database(
    entities = [Apartment::class, TankerLog::class, VacancyLog::class, BillingCycle::class],
    version = 3,
    exportSchema = false
)
@androidx.room.TypeConverters(TypeConverters::class)
abstract class TankerDatabase : RoomDatabase() {

    abstract fun apartmentDao(): ApartmentDao
    abstract fun tankerDao(): TankerDao
    abstract fun vacancyDao(): VacancyDao
    abstract fun billingCycleDao(): BillingCycleDao

    companion object {
        const val DATABASE_NAME = "tanker_database"
    }
}

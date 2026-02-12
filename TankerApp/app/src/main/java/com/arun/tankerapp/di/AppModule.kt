package com.arun.tankerapp.di

import android.content.Context
import androidx.room.Room
import com.arun.tankerapp.core.data.database.DatabaseCallback
import com.arun.tankerapp.core.data.database.TankerDatabase
import com.arun.tankerapp.core.data.database.dao.ApartmentDao
import com.arun.tankerapp.core.data.database.dao.TankerDao
import com.arun.tankerapp.core.data.database.dao.VacancyDao
import com.arun.tankerapp.core.data.database.dao.BillingCycleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTankerDatabase(
        @ApplicationContext context: Context,
        callback: DatabaseCallback // Hilt will inject this
    ): TankerDatabase {
        return Room.databaseBuilder(
            context,
            TankerDatabase::class.java,
            TankerDatabase.DATABASE_NAME
        )
            .addCallback(callback)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideApartmentDao(database: TankerDatabase): ApartmentDao {
        return database.apartmentDao()
    }

    @Provides
    @Singleton
    fun provideTankerDao(database: TankerDatabase): TankerDao {
        return database.tankerDao()
    }

    @Provides
    @Singleton
    fun provideVacancyDao(database: TankerDatabase): VacancyDao {
        return database.vacancyDao()
    }

    @Provides
    @Singleton
    fun provideBillingCycleDao(database: TankerDatabase): BillingCycleDao {
        return database.billingCycleDao()
    }
}

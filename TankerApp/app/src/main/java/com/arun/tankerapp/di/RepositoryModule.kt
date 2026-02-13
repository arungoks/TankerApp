package com.arun.tankerapp.di

import com.arun.tankerapp.core.data.repository.BillingRepository
import com.arun.tankerapp.core.data.repository.MongoBillingRepository
import com.arun.tankerapp.core.data.repository.RoomBillingRepository
import com.arun.tankerapp.core.data.repository.TankerRepository
import com.arun.tankerapp.core.data.repository.MongoTankerRepository
import com.arun.tankerapp.core.data.repository.RoomTankerRepository
import com.arun.tankerapp.core.data.repository.VacancyRepository
import com.arun.tankerapp.core.data.repository.MongoVacancyRepository
import com.arun.tankerapp.core.data.repository.RoomVacancyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // Uncomment this to switch back to Room
    // @Binds
    // @Singleton
    // abstract fun bindRoomBillingRepository(
    //     roomBillingRepository: RoomBillingRepository
    // ): BillingRepository

    // Current Configuration: MongoDB
    @Binds
    @Singleton
    abstract fun bindMongoBillingRepository(
        mongoBillingRepository: MongoBillingRepository
    ): BillingRepository

    @Binds
    @Singleton
    abstract fun bindMongoTankerRepository(
        mongoTankerRepository: MongoTankerRepository
    ): TankerRepository

    @Binds
    @Singleton
    abstract fun bindMongoVacancyRepository(
        mongoVacancyRepository: MongoVacancyRepository
    ): VacancyRepository
}

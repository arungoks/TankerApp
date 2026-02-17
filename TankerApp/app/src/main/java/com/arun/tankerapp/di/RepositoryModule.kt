package com.arun.tankerapp.di

import com.arun.tankerapp.core.data.repository.FirestoreVacancyRepository
import com.arun.tankerapp.core.data.repository.VacancyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindVacancyRepository(
        impl: FirestoreVacancyRepository
    ): VacancyRepository
}

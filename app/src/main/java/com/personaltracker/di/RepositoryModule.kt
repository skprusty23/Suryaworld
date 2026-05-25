package com.personaltracker.di

import com.personaltracker.data.repository.*
import com.personaltracker.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindDocumentRepository(impl: DocumentRepositoryImpl): DocumentRepository

    @Binds @Singleton
    abstract fun bindCredentialRepository(impl: CredentialRepositoryImpl): CredentialRepository

    @Binds @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds @Singleton
    abstract fun bindInvestmentRepository(impl: InvestmentRepositoryImpl): InvestmentRepository

    @Binds @Singleton
    abstract fun bindEmiRepository(impl: EmiRepositoryImpl): EmiRepository

    @Binds @Singleton
    abstract fun bindGoldRepository(impl: GoldRepositoryImpl): GoldRepository

    @Binds @Singleton
    abstract fun bindSchoolExpenseRepository(impl: SchoolExpenseRepositoryImpl): SchoolExpenseRepository

    @Binds @Singleton
    abstract fun bindTravelRepository(impl: TravelRepositoryImpl): TravelRepository

    @Binds @Singleton
    abstract fun bindGroupExpenseRepository(impl: GroupExpenseRepositoryImpl): GroupExpenseRepository
}

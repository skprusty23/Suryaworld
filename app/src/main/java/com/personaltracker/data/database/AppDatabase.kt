package com.personaltracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.personaltracker.data.database.converter.Converters
import com.personaltracker.data.database.dao.*
import com.personaltracker.data.database.entity.*

@Database(
    entities = [
        DocumentEntity::class,
        CredentialEntity::class,
        ExpenseEntity::class,
        InvestmentEntity::class,
        EmiEntity::class,
        EmiPaymentEntity::class,
        GoldInvestmentEntity::class,
        SchoolExpenseEntity::class,
        TripEntity::class,
        TravelExpenseEntity::class,
        ExpenseGroupEntity::class,
        GroupMemberEntity::class,
        GroupExpenseEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun credentialDao(): CredentialDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun emiDao(): EmiDao
    abstract fun goldInvestmentDao(): GoldInvestmentDao
    abstract fun schoolExpenseDao(): SchoolExpenseDao
    abstract fun travelDao(): TravelDao
    abstract fun groupExpenseDao(): GroupExpenseDao
}

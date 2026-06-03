package com.personaltracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        GroupExpenseEntity::class,
        NoteEntity::class,
        EventEntity::class,
        InsuranceEntity::class,
        MutualFundEntity::class,
        DepositEntity::class,
        StockEntity::class
    ],
    version = 4,
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
    abstract fun noteDao(): NoteDao
    abstract fun eventDao(): EventDao
    abstract fun insuranceDao(): InsuranceDao
    abstract fun mutualFundDao(): MutualFundDao
    abstract fun depositDao(): DepositDao
    abstract fun stockDao(): StockDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `category` TEXT NOT NULL DEFAULT 'General',
                        `isPinned` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` TEXT NOT NULL,
                        `modifiedAt` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `eventDate` TEXT NOT NULL,
                        `reminderDate` TEXT,
                        `category` TEXT NOT NULL DEFAULT 'General',
                        `priority` TEXT NOT NULL DEFAULT 'MEDIUM',
                        `repeatType` TEXT NOT NULL DEFAULT 'NONE',
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // New columns in credentials table
                database.execSQL("ALTER TABLE `credentials` ADD COLUMN `ifscCode` TEXT")
                database.execSQL("ALTER TABLE `credentials` ADD COLUMN `profilePassword` TEXT")

                // Insurance policies
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `insurance_policies` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `policyName` TEXT NOT NULL,
                        `insuranceType` TEXT NOT NULL,
                        `policyNumber` TEXT NOT NULL DEFAULT '',
                        `providerCompany` TEXT NOT NULL DEFAULT '',
                        `startDate` TEXT NOT NULL,
                        `maturityDate` TEXT NOT NULL,
                        `sumAssured` REAL NOT NULL,
                        `premiumAmount` REAL NOT NULL,
                        `premiumFrequency` TEXT NOT NULL DEFAULT 'YEARLY',
                        `policyTermYears` INTEGER NOT NULL DEFAULT 0,
                        `nomineeName` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` TEXT NOT NULL,
                        `updatedAt` TEXT NOT NULL
                    )
                """.trimIndent())

                // Mutual funds
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `mutual_funds` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `portfolioName` TEXT NOT NULL,
                        `amcName` TEXT NOT NULL DEFAULT '',
                        `fundName` TEXT NOT NULL DEFAULT '',
                        `folioNumber` TEXT NOT NULL DEFAULT '',
                        `investmentType` TEXT NOT NULL DEFAULT 'SIP',
                        `sipStartDate` TEXT,
                        `sipAmount` REAL,
                        `sipFrequency` TEXT NOT NULL DEFAULT 'MONTHLY',
                        `investmentDate` TEXT,
                        `investmentAmount` REAL,
                        `purpose` TEXT NOT NULL DEFAULT '',
                        `nominee` TEXT NOT NULL DEFAULT '',
                        `autoPay` INTEGER NOT NULL DEFAULT 0,
                        `linkedBankName` TEXT NOT NULL DEFAULT '',
                        `linkedAccountNumber` TEXT NOT NULL DEFAULT '',
                        `currentValue` REAL,
                        `returnsPercent` REAL,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` TEXT NOT NULL,
                        `updatedAt` TEXT NOT NULL
                    )
                """.trimIndent())

                // Deposits (RD / PPF / FD / NPS)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `deposits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `depositType` TEXT NOT NULL,
                        `accountNumber` TEXT NOT NULL DEFAULT '',
                        `institutionName` TEXT NOT NULL DEFAULT '',
                        `startDate` TEXT NOT NULL,
                        `maturityDate` TEXT,
                        `depositAmount` REAL NOT NULL,
                        `purpose` TEXT NOT NULL DEFAULT '',
                        `nominee` TEXT NOT NULL DEFAULT '',
                        `contributionType` TEXT NOT NULL DEFAULT 'MONTHLY',
                        `autoDebit` INTEGER NOT NULL DEFAULT 0,
                        `linkedBankName` TEXT NOT NULL DEFAULT '',
                        `linkedAccountNumber` TEXT NOT NULL DEFAULT '',
                        `currentValue` REAL,
                        `maturityValue` REAL,
                        `interestRate` REAL,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` TEXT NOT NULL,
                        `updatedAt` TEXT NOT NULL
                    )
                """.trimIndent())

                // Stocks
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stocks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `stockName` TEXT NOT NULL,
                        `stockSymbol` TEXT NOT NULL DEFAULT '',
                        `brokerName` TEXT NOT NULL DEFAULT '',
                        `dematAccountNumber` TEXT NOT NULL DEFAULT '',
                        `purchaseDate` TEXT NOT NULL,
                        `unitsPurchased` REAL NOT NULL,
                        `purchasePricePerUnit` REAL NOT NULL,
                        `totalPurchaseAmount` REAL NOT NULL,
                        `nominee` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `currentMarketPrice` REAL,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` TEXT NOT NULL,
                        `updatedAt` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}

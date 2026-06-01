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
        EventEntity::class
    ],
    version = 3,
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
    }
}

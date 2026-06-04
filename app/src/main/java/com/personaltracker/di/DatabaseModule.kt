package com.personaltracker.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.personaltracker.data.database.AppDatabase
import com.personaltracker.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "personal_tracker.db"
    private const val TAG = "DatabaseModule"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        securityManager: SecurityManager
    ): AppDatabase {
        val passphrase = securityManager.getOrCreateDatabaseKey()

        // Validate the encryption key against the existing DB file before Room opens it.
        // If SQLCipher reports "file is not a database", the key is stale (e.g. after
        // app data cleared on device but DB file survived, or KeyStore key was invalidated).
        // In that case, delete the corrupt/mismatched file so Room starts fresh.
        val dbFile = context.getDatabasePath(DB_NAME)
        if (dbFile.exists()) {
            try {
                SQLiteDatabase.loadLibs(context)
                val testDb = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    passphrase,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                testDb.close()
            } catch (e: Exception) {
                Log.w(TAG, "DB key mismatch or corruption detected — deleting and recreating: ${e.message}")
                // Delete main DB file plus WAL/SHM journal files
                dbFile.delete()
                context.getDatabasePath("$DB_NAME-wal").delete()
                context.getDatabasePath("$DB_NAME-shm").delete()
            }
        }

        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .openHelperFactory(factory)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()
    }

    @Provides fun provideDocumentDao(db: AppDatabase) = db.documentDao()
    @Provides fun provideCredentialDao(db: AppDatabase) = db.credentialDao()
    @Provides fun provideExpenseDao(db: AppDatabase) = db.expenseDao()
    @Provides fun provideInvestmentDao(db: AppDatabase) = db.investmentDao()
    @Provides fun provideEmiDao(db: AppDatabase) = db.emiDao()
    @Provides fun provideGoldDao(db: AppDatabase) = db.goldInvestmentDao()
    @Provides fun provideSchoolExpenseDao(db: AppDatabase) = db.schoolExpenseDao()
    @Provides fun provideTravelDao(db: AppDatabase) = db.travelDao()
    @Provides fun provideGroupExpenseDao(db: AppDatabase) = db.groupExpenseDao()
    @Provides fun provideNoteDao(db: AppDatabase) = db.noteDao()
    @Provides fun provideEventDao(db: AppDatabase) = db.eventDao()
    @Provides fun provideInsuranceDao(db: AppDatabase) = db.insuranceDao()
    @Provides fun provideMutualFundDao(db: AppDatabase) = db.mutualFundDao()
    @Provides fun provideDepositDao(db: AppDatabase) = db.depositDao()
    @Provides fun provideStockDao(db: AppDatabase) = db.stockDao()
}

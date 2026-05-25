package com.personaltracker.di

import android.content.Context
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

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        securityManager: SecurityManager
    ): AppDatabase {
        val passphrase = securityManager.getOrCreateDatabaseKey()
        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(context, AppDatabase::class.java, "personal_tracker.db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
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
}

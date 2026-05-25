package com.personaltracker.di

import com.personaltracker.backup.BackupManager
import com.personaltracker.backup.GoogleDriveManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {
    // BackupManager and GoogleDriveManager are @Singleton with @Inject constructor
    // Hilt auto-provides them — no explicit @Provides needed
}

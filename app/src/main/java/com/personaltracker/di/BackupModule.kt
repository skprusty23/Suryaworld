package com.personaltracker.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {
    // BackupManager is @Singleton with @Inject constructor — Hilt auto-provides it.
    // All backup is local-only; no cloud storage is used.
}

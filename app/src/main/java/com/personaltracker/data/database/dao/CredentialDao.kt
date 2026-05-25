package com.personaltracker.data.database.dao

import androidx.room.*
import com.personaltracker.data.database.entity.CredentialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDao {
    @Query("SELECT * FROM credentials ORDER BY name ASC")
    fun getAllCredentials(): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM credentials WHERE category = :category ORDER BY name ASC")
    fun getCredentialsByCategory(category: String): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM credentials WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteCredentials(): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM credentials WHERE id = :id")
    suspend fun getCredentialById(id: Long): CredentialEntity?

    @Query("SELECT * FROM credentials WHERE name LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%'")
    fun searchCredentials(query: String): Flow<List<CredentialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: CredentialEntity): Long

    @Update
    suspend fun updateCredential(credential: CredentialEntity)

    @Delete
    suspend fun deleteCredential(credential: CredentialEntity)

    @Query("SELECT DISTINCT category FROM credentials ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>
}

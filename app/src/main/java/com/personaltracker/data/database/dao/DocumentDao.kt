package com.personaltracker.data.database.dao

import androidx.room.*
import com.personaltracker.data.database.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE category = :category ORDER BY name ASC")
    fun getDocumentsByCategory(category: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE name LIKE '%' || :query || '%' OR documentType LIKE '%' || :query || '%'")
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE expiryDate IS NOT NULL AND expiryDate <= :date ORDER BY expiryDate ASC")
    fun getExpiringDocuments(date: LocalDate): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("SELECT DISTINCT category FROM documents ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>
}

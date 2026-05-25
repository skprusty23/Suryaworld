package com.personaltracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val documentType: String,
    val fileUri: String? = null,
    val thumbnailUri: String? = null,
    val expiryDate: LocalDate? = null,
    val notes: String? = null,
    val documentNumber: String? = null,
    val issuedBy: String? = null,
    val issuedDate: LocalDate? = null,
    val personName: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

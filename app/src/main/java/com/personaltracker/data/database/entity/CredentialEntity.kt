package com.personaltracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val username: String? = null,
    val passwordEncrypted: String? = null,
    val url: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val accountNumber: String? = null,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

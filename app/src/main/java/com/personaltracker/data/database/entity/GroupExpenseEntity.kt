package com.personaltracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "expense_groups")
data class ExpenseGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val isSettled: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity(tableName = "group_members")
data class GroupMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val phone: String? = null,
    val email: String? = null
)

@Entity(tableName = "group_expenses")
data class GroupExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val paidByMemberId: Long,
    val amount: Double,
    val description: String,
    val category: String = "General",
    val date: LocalDate,
    val splitType: String = "Equal",
    val notes: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

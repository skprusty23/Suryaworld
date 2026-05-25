package com.personaltracker.data.database.dao

import androidx.room.*
import com.personaltracker.data.database.entity.ExpenseGroupEntity
import com.personaltracker.data.database.entity.GroupExpenseEntity
import com.personaltracker.data.database.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupExpenseDao {
    @Query("SELECT * FROM expense_groups ORDER BY startDate DESC")
    fun getAllGroups(): Flow<List<ExpenseGroupEntity>>

    @Query("SELECT * FROM expense_groups WHERE isSettled = 0 ORDER BY startDate DESC")
    fun getActiveGroups(): Flow<List<ExpenseGroupEntity>>

    @Query("SELECT * FROM expense_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): ExpenseGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ExpenseGroupEntity): Long

    @Update
    suspend fun updateGroup(group: ExpenseGroupEntity)

    @Delete
    suspend fun deleteGroup(group: ExpenseGroupEntity)

    // Members
    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY name ASC")
    fun getMembersForGroup(groupId: Long): Flow<List<GroupMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMemberEntity): Long

    @Update
    suspend fun updateMember(member: GroupMemberEntity)

    @Delete
    suspend fun deleteMember(member: GroupMemberEntity)

    // Expenses
    @Query("SELECT * FROM group_expenses WHERE groupId = :groupId ORDER BY date DESC")
    fun getExpensesForGroup(groupId: Long): Flow<List<GroupExpenseEntity>>

    @Query("SELECT SUM(amount) FROM group_expenses WHERE groupId = :groupId")
    fun getTotalForGroup(groupId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM group_expenses WHERE groupId = :groupId AND paidByMemberId = :memberId")
    fun getPaidByMember(groupId: Long, memberId: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupExpense(expense: GroupExpenseEntity): Long

    @Update
    suspend fun updateGroupExpense(expense: GroupExpenseEntity)

    @Delete
    suspend fun deleteGroupExpense(expense: GroupExpenseEntity)
}

package com.personaltracker.domain.repository

import com.personaltracker.data.database.dao.CategoryTotal
import com.personaltracker.data.database.dao.TravelCategoryTotal
import com.personaltracker.data.database.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DocumentRepository {
    fun getAllDocuments(): Flow<List<DocumentEntity>>
    fun getDocumentsByCategory(category: String): Flow<List<DocumentEntity>>
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>
    fun getExpiringDocuments(date: LocalDate): Flow<List<DocumentEntity>>
    fun getAllCategories(): Flow<List<String>>
    suspend fun getDocumentById(id: Long): DocumentEntity?
    suspend fun insertDocument(document: DocumentEntity): Long
    suspend fun updateDocument(document: DocumentEntity)
    suspend fun deleteDocument(document: DocumentEntity)
}

interface CredentialRepository {
    fun getAllCredentials(): Flow<List<CredentialEntity>>
    fun getCredentialsByCategory(category: String): Flow<List<CredentialEntity>>
    fun getFavoriteCredentials(): Flow<List<CredentialEntity>>
    fun searchCredentials(query: String): Flow<List<CredentialEntity>>
    fun getAllCategories(): Flow<List<String>>
    suspend fun getCredentialById(id: Long): CredentialEntity?
    suspend fun insertCredential(credential: CredentialEntity): Long
    suspend fun updateCredential(credential: CredentialEntity)
    suspend fun deleteCredential(credential: CredentialEntity)
}

interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<ExpenseEntity>>
    fun getExpensesByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ExpenseEntity>>
    fun getExpensesByMonth(yearMonth: String): Flow<List<ExpenseEntity>>
    fun getExpensesByCategory(category: String): Flow<List<ExpenseEntity>>
    fun getTotalByMonth(yearMonth: String): Flow<Double?>
    fun getCategoryTotalsByMonth(yearMonth: String): Flow<List<CategoryTotal>>
    fun getRecentExpenses(limit: Int): Flow<List<ExpenseEntity>>
    fun getAllCategories(): Flow<List<String>>
    suspend fun insertExpense(expense: ExpenseEntity): Long
    suspend fun updateExpense(expense: ExpenseEntity)
    suspend fun deleteExpense(expense: ExpenseEntity)
}

interface InvestmentRepository {
    fun getAllActiveInvestments(): Flow<List<InvestmentEntity>>
    fun getAllInvestments(): Flow<List<InvestmentEntity>>
    fun getInvestmentsByType(type: String): Flow<List<InvestmentEntity>>
    fun getUpcomingMaturities(today: LocalDate, futureDate: LocalDate): Flow<List<InvestmentEntity>>
    fun getTotalInvestmentAmount(): Flow<Double?>
    fun getTotalCurrentValue(): Flow<Double?>
    fun getAllTypes(): Flow<List<String>>
    suspend fun getInvestmentById(id: Long): InvestmentEntity?
    suspend fun insertInvestment(investment: InvestmentEntity): Long
    suspend fun updateInvestment(investment: InvestmentEntity)
    suspend fun deleteInvestment(investment: InvestmentEntity)
}

interface EmiRepository {
    fun getAllActiveEmis(): Flow<List<EmiEntity>>
    fun getAllEmis(): Flow<List<EmiEntity>>
    fun getTotalMonthlyEmi(): Flow<Double?>
    fun getPaymentsForEmi(emiId: Long): Flow<List<EmiPaymentEntity>>
    fun getPaidCount(emiId: Long): Flow<Int>
    suspend fun getEmiById(id: Long): EmiEntity?
    suspend fun getPayment(emiId: Long, month: Int, year: Int): EmiPaymentEntity?
    suspend fun insertEmi(emi: EmiEntity): Long
    suspend fun updateEmi(emi: EmiEntity)
    suspend fun deleteEmi(emi: EmiEntity)
    suspend fun insertPayment(payment: EmiPaymentEntity): Long
    suspend fun updatePayment(payment: EmiPaymentEntity)
    suspend fun deletePayment(payment: EmiPaymentEntity)
}

interface GoldRepository {
    fun getAllGoldInvestments(): Flow<List<GoldInvestmentEntity>>
    fun getGoldByType(type: String): Flow<List<GoldInvestmentEntity>>
    fun getTotalGrams(): Flow<Double?>
    fun getTotalInvested(): Flow<Double?>
    suspend fun insertGoldInvestment(gold: GoldInvestmentEntity): Long
    suspend fun updateGoldInvestment(gold: GoldInvestmentEntity)
    suspend fun deleteGoldInvestment(gold: GoldInvestmentEntity)
}

interface SchoolExpenseRepository {
    fun getAllSchoolExpenses(): Flow<List<SchoolExpenseEntity>>
    fun getExpensesByChild(childName: String): Flow<List<SchoolExpenseEntity>>
    fun getExpensesByChildAndYear(childName: String, year: String): Flow<List<SchoolExpenseEntity>>
    fun getTotalByChildAndYear(childName: String, year: String): Flow<Double?>
    fun getAllChildren(): Flow<List<String>>
    fun getAllYears(): Flow<List<String>>
    suspend fun insertSchoolExpense(expense: SchoolExpenseEntity): Long
    suspend fun updateSchoolExpense(expense: SchoolExpenseEntity)
    suspend fun deleteSchoolExpense(expense: SchoolExpenseEntity)
}

interface TravelRepository {
    fun getAllTrips(): Flow<List<TripEntity>>
    fun getActiveTrips(): Flow<List<TripEntity>>
    fun getExpensesForTrip(tripId: Long): Flow<List<TravelExpenseEntity>>
    fun getTotalForTrip(tripId: Long): Flow<Double?>
    fun getCategoryTotalsForTrip(tripId: Long): Flow<List<TravelCategoryTotal>>
    suspend fun getTripById(id: Long): TripEntity?
    suspend fun insertTrip(trip: TripEntity): Long
    suspend fun updateTrip(trip: TripEntity)
    suspend fun deleteTrip(trip: TripEntity)
    suspend fun insertTravelExpense(expense: TravelExpenseEntity): Long
    suspend fun updateTravelExpense(expense: TravelExpenseEntity)
    suspend fun deleteTravelExpense(expense: TravelExpenseEntity)
}

interface GroupExpenseRepository {
    fun getAllGroups(): Flow<List<ExpenseGroupEntity>>
    fun getActiveGroups(): Flow<List<ExpenseGroupEntity>>
    fun getMembersForGroup(groupId: Long): Flow<List<GroupMemberEntity>>
    fun getExpensesForGroup(groupId: Long): Flow<List<GroupExpenseEntity>>
    fun getTotalForGroup(groupId: Long): Flow<Double?>
    fun getPaidByMember(groupId: Long, memberId: Long): Flow<Double?>
    suspend fun getGroupById(id: Long): ExpenseGroupEntity?
    suspend fun insertGroup(group: ExpenseGroupEntity): Long
    suspend fun updateGroup(group: ExpenseGroupEntity)
    suspend fun deleteGroup(group: ExpenseGroupEntity)
    suspend fun insertMember(member: GroupMemberEntity): Long
    suspend fun updateMember(member: GroupMemberEntity)
    suspend fun deleteMember(member: GroupMemberEntity)
    suspend fun insertGroupExpense(expense: GroupExpenseEntity): Long
    suspend fun updateGroupExpense(expense: GroupExpenseEntity)
    suspend fun deleteGroupExpense(expense: GroupExpenseEntity)
}

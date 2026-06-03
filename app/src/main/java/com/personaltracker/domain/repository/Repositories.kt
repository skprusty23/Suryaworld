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
    // One-shot queries for report export
    suspend fun getExpensesByMonthOnce(yearMonth: String): List<ExpenseEntity>
    suspend fun getCategoryTotalsByMonthOnce(yearMonth: String): List<CategoryTotal>
    suspend fun getAllExpensesOnce(): List<ExpenseEntity>
    suspend fun getExpensesByDateRangeOnce(startDate: LocalDate, endDate: LocalDate): List<ExpenseEntity>
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

interface InsuranceRepository {
    fun getAll(): Flow<List<com.personaltracker.data.database.entity.InsuranceEntity>>
    fun getAllActive(): Flow<List<com.personaltracker.data.database.entity.InsuranceEntity>>
    fun getByType(type: com.personaltracker.data.database.entity.InsuranceType): Flow<List<com.personaltracker.data.database.entity.InsuranceEntity>>
    fun getTotalCoverage(): Flow<Double>
    suspend fun getById(id: Long): com.personaltracker.data.database.entity.InsuranceEntity?
    suspend fun insert(entity: com.personaltracker.data.database.entity.InsuranceEntity): Long
    suspend fun update(entity: com.personaltracker.data.database.entity.InsuranceEntity)
    suspend fun delete(entity: com.personaltracker.data.database.entity.InsuranceEntity)
}

interface MutualFundRepository {
    fun getAll(): Flow<List<com.personaltracker.data.database.entity.MutualFundEntity>>
    fun getAllActive(): Flow<List<com.personaltracker.data.database.entity.MutualFundEntity>>
    fun getTotalSipAmount(): Flow<Double>
    fun getTotalLumpsumAmount(): Flow<Double>
    suspend fun getById(id: Long): com.personaltracker.data.database.entity.MutualFundEntity?
    suspend fun insert(entity: com.personaltracker.data.database.entity.MutualFundEntity): Long
    suspend fun update(entity: com.personaltracker.data.database.entity.MutualFundEntity)
    suspend fun delete(entity: com.personaltracker.data.database.entity.MutualFundEntity)
}

interface DepositRepository {
    fun getAll(): Flow<List<com.personaltracker.data.database.entity.DepositEntity>>
    fun getAllActive(): Flow<List<com.personaltracker.data.database.entity.DepositEntity>>
    fun getByType(type: com.personaltracker.data.database.entity.DepositType): Flow<List<com.personaltracker.data.database.entity.DepositEntity>>
    fun getTotalDeposited(): Flow<Double>
    suspend fun getById(id: Long): com.personaltracker.data.database.entity.DepositEntity?
    suspend fun insert(entity: com.personaltracker.data.database.entity.DepositEntity): Long
    suspend fun update(entity: com.personaltracker.data.database.entity.DepositEntity)
    suspend fun delete(entity: com.personaltracker.data.database.entity.DepositEntity)
}

interface StockRepository {
    fun getAll(): Flow<List<com.personaltracker.data.database.entity.StockEntity>>
    fun getAllActive(): Flow<List<com.personaltracker.data.database.entity.StockEntity>>
    fun getTotalInvested(): Flow<Double>
    suspend fun getById(id: Long): com.personaltracker.data.database.entity.StockEntity?
    suspend fun insert(entity: com.personaltracker.data.database.entity.StockEntity): Long
    suspend fun update(entity: com.personaltracker.data.database.entity.StockEntity)
    suspend fun delete(entity: com.personaltracker.data.database.entity.StockEntity)
}

interface NoteRepository {
    fun getAllNotes(): Flow<List<com.personaltracker.data.database.entity.NoteEntity>>
    fun searchNotes(query: String): Flow<List<com.personaltracker.data.database.entity.NoteEntity>>
    fun getNotesByCategory(category: String): Flow<List<com.personaltracker.data.database.entity.NoteEntity>>
    fun getPinnedNotes(): Flow<List<com.personaltracker.data.database.entity.NoteEntity>>
    fun getAllCategories(): Flow<List<String>>
    suspend fun getNoteById(id: Long): com.personaltracker.data.database.entity.NoteEntity?
    suspend fun insertNote(note: com.personaltracker.data.database.entity.NoteEntity): Long
    suspend fun updateNote(note: com.personaltracker.data.database.entity.NoteEntity)
    suspend fun deleteNote(note: com.personaltracker.data.database.entity.NoteEntity)
}

interface EventRepository {
    fun getAllActiveEvents(): Flow<List<com.personaltracker.data.database.entity.EventEntity>>
    fun getAllEvents(): Flow<List<com.personaltracker.data.database.entity.EventEntity>>
    fun getEventsByDate(date: LocalDate): Flow<List<com.personaltracker.data.database.entity.EventEntity>>
    fun getUpcomingEvents(today: LocalDate, future: LocalDate): Flow<List<com.personaltracker.data.database.entity.EventEntity>>
    fun getEventsByCategory(category: String): Flow<List<com.personaltracker.data.database.entity.EventEntity>>
    fun getAllCategories(): Flow<List<String>>
    suspend fun getEventsForToday(today: LocalDate): List<com.personaltracker.data.database.entity.EventEntity>
    suspend fun getEventById(id: Long): com.personaltracker.data.database.entity.EventEntity?
    suspend fun insertEvent(event: com.personaltracker.data.database.entity.EventEntity): Long
    suspend fun updateEvent(event: com.personaltracker.data.database.entity.EventEntity)
    suspend fun deleteEvent(event: com.personaltracker.data.database.entity.EventEntity)
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

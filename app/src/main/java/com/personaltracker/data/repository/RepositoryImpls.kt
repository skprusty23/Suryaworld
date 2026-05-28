package com.personaltracker.data.repository

import com.personaltracker.data.database.dao.*
import com.personaltracker.data.database.entity.*
import com.personaltracker.domain.repository.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(private val dao: DocumentDao) : DocumentRepository {
    override fun getAllDocuments() = dao.getAllDocuments()
    override fun getDocumentsByCategory(category: String) = dao.getDocumentsByCategory(category)
    override fun searchDocuments(query: String) = dao.searchDocuments(query)
    override fun getExpiringDocuments(date: LocalDate) = dao.getExpiringDocuments(date)
    override fun getAllCategories() = dao.getAllCategories()
    override suspend fun getDocumentById(id: Long) = dao.getDocumentById(id)
    override suspend fun insertDocument(document: DocumentEntity) = dao.insertDocument(document)
    override suspend fun updateDocument(document: DocumentEntity) = dao.updateDocument(document)
    override suspend fun deleteDocument(document: DocumentEntity) = dao.deleteDocument(document)
}

class CredentialRepositoryImpl @Inject constructor(private val dao: CredentialDao) : CredentialRepository {
    override fun getAllCredentials() = dao.getAllCredentials()
    override fun getCredentialsByCategory(category: String) = dao.getCredentialsByCategory(category)
    override fun getFavoriteCredentials() = dao.getFavoriteCredentials()
    override fun searchCredentials(query: String) = dao.searchCredentials(query)
    override fun getAllCategories() = dao.getAllCategories()
    override suspend fun getCredentialById(id: Long) = dao.getCredentialById(id)
    override suspend fun insertCredential(credential: CredentialEntity) = dao.insertCredential(credential)
    override suspend fun updateCredential(credential: CredentialEntity) = dao.updateCredential(credential)
    override suspend fun deleteCredential(credential: CredentialEntity) = dao.deleteCredential(credential)
}

class ExpenseRepositoryImpl @Inject constructor(private val dao: ExpenseDao) : ExpenseRepository {
    override fun getAllExpenses() = dao.getAllExpenses()
    override fun getExpensesByDateRange(startDate: LocalDate, endDate: LocalDate) = dao.getExpensesByDateRange(startDate, endDate)
    override fun getExpensesByMonth(yearMonth: String) = dao.getExpensesByMonth(yearMonth)
    override fun getExpensesByCategory(category: String) = dao.getExpensesByCategory(category)
    override fun getTotalByMonth(yearMonth: String) = dao.getTotalByMonth(yearMonth)
    override fun getCategoryTotalsByMonth(yearMonth: String) = dao.getCategoryTotalsByMonth(yearMonth)
    override fun getRecentExpenses(limit: Int) = dao.getRecentExpenses(limit)
    override fun getAllCategories() = dao.getAllCategories()
    override suspend fun insertExpense(expense: ExpenseEntity) = dao.insertExpense(expense)
    override suspend fun updateExpense(expense: ExpenseEntity) = dao.updateExpense(expense)
    override suspend fun deleteExpense(expense: ExpenseEntity) = dao.deleteExpense(expense)
    override suspend fun getExpensesByMonthOnce(yearMonth: String) = dao.getExpensesByMonthOnce(yearMonth)
    override suspend fun getCategoryTotalsByMonthOnce(yearMonth: String) = dao.getCategoryTotalsByMonthOnce(yearMonth)
    override suspend fun getAllExpensesOnce() = dao.getAllExpensesOnce()
    override suspend fun getExpensesByDateRangeOnce(startDate: LocalDate, endDate: LocalDate) = dao.getExpensesByDateRangeOnce(startDate, endDate)
}

class InvestmentRepositoryImpl @Inject constructor(private val dao: InvestmentDao) : InvestmentRepository {
    override fun getAllActiveInvestments() = dao.getAllActiveInvestments()
    override fun getAllInvestments() = dao.getAllInvestments()
    override fun getInvestmentsByType(type: String) = dao.getInvestmentsByType(type)
    override fun getUpcomingMaturities(today: LocalDate, futureDate: LocalDate) = dao.getUpcomingMaturities(today, futureDate)
    override fun getTotalInvestmentAmount() = dao.getTotalInvestmentAmount()
    override fun getTotalCurrentValue() = dao.getTotalCurrentValue()
    override fun getAllTypes() = dao.getAllTypes()
    override suspend fun getInvestmentById(id: Long) = dao.getInvestmentById(id)
    override suspend fun insertInvestment(investment: InvestmentEntity) = dao.insertInvestment(investment)
    override suspend fun updateInvestment(investment: InvestmentEntity) = dao.updateInvestment(investment)
    override suspend fun deleteInvestment(investment: InvestmentEntity) = dao.deleteInvestment(investment)
}

class EmiRepositoryImpl @Inject constructor(private val dao: EmiDao) : EmiRepository {
    override fun getAllActiveEmis() = dao.getAllActiveEmis()
    override fun getAllEmis() = dao.getAllEmis()
    override fun getTotalMonthlyEmi() = dao.getTotalMonthlyEmi()
    override fun getPaymentsForEmi(emiId: Long) = dao.getPaymentsForEmi(emiId)
    override fun getPaidCount(emiId: Long) = dao.getPaidCount(emiId)
    override suspend fun getEmiById(id: Long) = dao.getEmiById(id)
    override suspend fun getPayment(emiId: Long, month: Int, year: Int) = dao.getPayment(emiId, month, year)
    override suspend fun insertEmi(emi: EmiEntity) = dao.insertEmi(emi)
    override suspend fun updateEmi(emi: EmiEntity) = dao.updateEmi(emi)
    override suspend fun deleteEmi(emi: EmiEntity) = dao.deleteEmi(emi)
    override suspend fun insertPayment(payment: EmiPaymentEntity) = dao.insertPayment(payment)
    override suspend fun updatePayment(payment: EmiPaymentEntity) = dao.updatePayment(payment)
    override suspend fun deletePayment(payment: EmiPaymentEntity) = dao.deletePayment(payment)
}

class GoldRepositoryImpl @Inject constructor(private val dao: GoldInvestmentDao) : GoldRepository {
    override fun getAllGoldInvestments() = dao.getAllGoldInvestments()
    override fun getGoldByType(type: String) = dao.getGoldByType(type)
    override fun getTotalGrams() = dao.getTotalGrams()
    override fun getTotalInvested() = dao.getTotalInvested()
    override suspend fun insertGoldInvestment(gold: GoldInvestmentEntity) = dao.insertGoldInvestment(gold)
    override suspend fun updateGoldInvestment(gold: GoldInvestmentEntity) = dao.updateGoldInvestment(gold)
    override suspend fun deleteGoldInvestment(gold: GoldInvestmentEntity) = dao.deleteGoldInvestment(gold)
}

class SchoolExpenseRepositoryImpl @Inject constructor(private val dao: SchoolExpenseDao) : SchoolExpenseRepository {
    override fun getAllSchoolExpenses() = dao.getAllSchoolExpenses()
    override fun getExpensesByChild(childName: String) = dao.getExpensesByChild(childName)
    override fun getExpensesByChildAndYear(childName: String, year: String) = dao.getExpensesByChildAndYear(childName, year)
    override fun getTotalByChildAndYear(childName: String, year: String) = dao.getTotalByChildAndYear(childName, year)
    override fun getAllChildren() = dao.getAllChildren()
    override fun getAllYears() = dao.getAllYears()
    override suspend fun insertSchoolExpense(expense: SchoolExpenseEntity) = dao.insertSchoolExpense(expense)
    override suspend fun updateSchoolExpense(expense: SchoolExpenseEntity) = dao.updateSchoolExpense(expense)
    override suspend fun deleteSchoolExpense(expense: SchoolExpenseEntity) = dao.deleteSchoolExpense(expense)
}

class TravelRepositoryImpl @Inject constructor(private val dao: TravelDao) : TravelRepository {
    override fun getAllTrips() = dao.getAllTrips()
    override fun getActiveTrips() = dao.getActiveTrips()
    override fun getExpensesForTrip(tripId: Long) = dao.getExpensesForTrip(tripId)
    override fun getTotalForTrip(tripId: Long) = dao.getTotalForTrip(tripId)
    override fun getCategoryTotalsForTrip(tripId: Long) = dao.getCategoryTotalsForTrip(tripId)
    override suspend fun getTripById(id: Long) = dao.getTripById(id)
    override suspend fun insertTrip(trip: TripEntity) = dao.insertTrip(trip)
    override suspend fun updateTrip(trip: TripEntity) = dao.updateTrip(trip)
    override suspend fun deleteTrip(trip: TripEntity) = dao.deleteTrip(trip)
    override suspend fun insertTravelExpense(expense: TravelExpenseEntity) = dao.insertTravelExpense(expense)
    override suspend fun updateTravelExpense(expense: TravelExpenseEntity) = dao.updateTravelExpense(expense)
    override suspend fun deleteTravelExpense(expense: TravelExpenseEntity) = dao.deleteTravelExpense(expense)
}

class GroupExpenseRepositoryImpl @Inject constructor(private val dao: GroupExpenseDao) : GroupExpenseRepository {
    override fun getAllGroups() = dao.getAllGroups()
    override fun getActiveGroups() = dao.getActiveGroups()
    override fun getMembersForGroup(groupId: Long) = dao.getMembersForGroup(groupId)
    override fun getExpensesForGroup(groupId: Long) = dao.getExpensesForGroup(groupId)
    override fun getTotalForGroup(groupId: Long) = dao.getTotalForGroup(groupId)
    override fun getPaidByMember(groupId: Long, memberId: Long) = dao.getPaidByMember(groupId, memberId)
    override suspend fun getGroupById(id: Long) = dao.getGroupById(id)
    override suspend fun insertGroup(group: ExpenseGroupEntity) = dao.insertGroup(group)
    override suspend fun updateGroup(group: ExpenseGroupEntity) = dao.updateGroup(group)
    override suspend fun deleteGroup(group: ExpenseGroupEntity) = dao.deleteGroup(group)
    override suspend fun insertMember(member: GroupMemberEntity) = dao.insertMember(member)
    override suspend fun updateMember(member: GroupMemberEntity) = dao.updateMember(member)
    override suspend fun deleteMember(member: GroupMemberEntity) = dao.deleteMember(member)
    override suspend fun insertGroupExpense(expense: GroupExpenseEntity) = dao.insertGroupExpense(expense)
    override suspend fun updateGroupExpense(expense: GroupExpenseEntity) = dao.updateGroupExpense(expense)
    override suspend fun deleteGroupExpense(expense: GroupExpenseEntity) = dao.deleteGroupExpense(expense)
}

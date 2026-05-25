package com.personaltracker.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.ExpenseEntity
import com.personaltracker.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _saveResult = MutableStateFlow<Boolean?>(null)
    val saveResult: StateFlow<Boolean?> = _saveResult.asStateFlow()

    fun save(
        amount: Double,
        category: String,
        subCategory: String?,
        description: String?,
        date: LocalDate,
        paymentMethod: String,
        expenseType: String,
        notes: String?
    ) {
        viewModelScope.launch {
            val expense = ExpenseEntity(
                amount = amount,
                category = category,
                subCategory = subCategory,
                description = description,
                date = date,
                paymentMethod = paymentMethod,
                expenseType = expenseType,
                notes = notes
            )
            expenseRepository.insertExpense(expense)
            _saveResult.value = true
        }
    }
}

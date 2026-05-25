package com.personaltracker.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.ExpenseGroupEntity
import com.personaltracker.data.database.entity.GroupMemberEntity
import com.personaltracker.domain.repository.GroupExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class GroupListUiState(
    val groups: List<GroupWithSummary> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class GroupWithSummary(
    val group: ExpenseGroupEntity,
    val memberCount: Int,
    val totalAmount: Double
)

@HiltViewModel
class GroupExpensesViewModel @Inject constructor(
    private val groupExpenseRepository: GroupExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
    }

    private fun loadGroups() {
        viewModelScope.launch {
            groupExpenseRepository.getAllGroups()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { groups ->
                    val summaries = groups.map { group ->
                        val members = groupExpenseRepository.getMembersForGroup(group.id).firstOrNull() ?: emptyList()
                        val total = groupExpenseRepository.getTotalForGroup(group.id).firstOrNull() ?: 0.0
                        GroupWithSummary(
                            group = group,
                            memberCount = members.size,
                            totalAmount = total
                        )
                    }
                    _uiState.value = GroupListUiState(groups = summaries, isLoading = false)
                }
        }
    }

    fun createGroup(
        name: String,
        description: String?,
        startDate: LocalDate,
        members: List<Pair<String, String?>>
    ) {
        viewModelScope.launch {
            try {
                val group = ExpenseGroupEntity(
                    name = name,
                    description = description,
                    startDate = startDate
                )
                val groupId = groupExpenseRepository.insertGroup(group)
                members.forEach { (memberName, phone) ->
                    groupExpenseRepository.insertMember(
                        GroupMemberEntity(
                            groupId = groupId,
                            name = memberName,
                            phone = phone
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteGroup(group: ExpenseGroupEntity) {
        viewModelScope.launch {
            try {
                groupExpenseRepository.deleteGroup(group)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun settleGroup(group: ExpenseGroupEntity) {
        viewModelScope.launch {
            try {
                groupExpenseRepository.updateGroup(group.copy(isSettled = true))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

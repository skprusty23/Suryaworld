package com.personaltracker.ui.screens.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.DocumentEntity
import com.personaltracker.domain.repository.DocumentRepository
import com.personaltracker.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DocumentsUiState(
    val documents: List<DocumentEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DocumentsUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _isLoading,
        _errorMessage
    ) { query, category, loading, error ->
        Triple(query, category, Pair(loading, error))
    }.flatMapLatest { (query, category, extra) ->
        val (loading, error) = extra
        val source = when {
            query.isNotBlank() -> documentRepository.searchDocuments(query)
            category != "All" -> documentRepository.getDocumentsByCategory(category)
            else -> documentRepository.getAllDocuments()
        }
        source.map { docs ->
            DocumentsUiState(
                documents = docs,
                searchQuery = query,
                selectedCategory = category,
                isLoading = loading,
                errorMessage = error
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DocumentsUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            try {
                documentRepository.deleteDocument(document)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete document: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun isExpiringSoon(document: DocumentEntity): Boolean {
        val expiryDate = document.expiryDate ?: return false
        val today = LocalDate.now()
        return expiryDate.isAfter(today) && expiryDate.isBefore(today.plusDays(90))
    }

    fun isExpired(document: DocumentEntity): Boolean {
        val expiryDate = document.expiryDate ?: return false
        return expiryDate.isBefore(LocalDate.now())
    }
}

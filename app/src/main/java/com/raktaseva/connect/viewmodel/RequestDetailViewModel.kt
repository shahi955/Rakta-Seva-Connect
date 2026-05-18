package com.raktaseva.connect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.raktaseva.connect.di.AppGraph
import com.raktaseva.connect.model.firestore.BloodRequestDocument
import com.raktaseva.connect.repository.BloodRequestsRepository
import com.raktaseva.connect.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RequestDetailUiState(
    val isLoading: Boolean = true,
    val request: BloodRequestDocument? = null,
    val error: String? = null
)

class RequestDetailViewModel(
    private val requestId: String,
    private val repository: BloodRequestsRepository = AppGraph.bloodRequestsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestDetailUiState())
    val uiState: StateFlow<RequestDetailUiState> = _uiState.asStateFlow()

    init {
        if (requestId.isBlank()) {
            _uiState.value = RequestDetailUiState(
                isLoading = false,
                request = null,
                error = "Invalid request link."
            )
        } else {
            refresh()
        }
    }

    fun refresh() {
        if (requestId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.get(requestId)
            }.onSuccess { doc ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        request = doc,
                        error = if (doc == null) "This request no longer exists or you cannot access it." else null
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, request = null, error = e.toUserMessage())
                }
            }
        }
    }

    companion object {
        fun factory(requestId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(RequestDetailViewModel::class.java)) {
                    return RequestDetailViewModel(requestId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
            }
        }
    }
}

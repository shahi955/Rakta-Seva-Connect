package com.raktaseva.connect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.raktaseva.connect.di.AppGraph
import com.raktaseva.connect.model.firestore.BloodRequestDocument
import com.raktaseva.connect.model.firestore.DonationDocument
import com.raktaseva.connect.model.firestore.UserDocument
import com.raktaseva.connect.repository.BloodRequestsRepository
import com.raktaseva.connect.repository.DonationsRepository
import com.raktaseva.connect.repository.UsersRepository
import com.raktaseva.connect.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val isLoading: Boolean = false,
    val isBootstrapping: Boolean = true,
    val error: String? = null,
    val snackbar: String? = null,
    val users: List<UserDocument> = emptyList(),
    val requests: List<BloodRequestDocument> = emptyList(),
    val donations: List<DonationDocument> = emptyList()
)

class AdminViewModel(
    private val usersRepository: UsersRepository = AppGraph.usersRepository,
    private val bloodRequestsRepository: BloodRequestsRepository = AppGraph.bloodRequestsRepository,
    private val donationsRepository: DonationsRepository = AppGraph.donationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    snackbar = null
                )
            }
            runCatching {
                val users = usersRepository.listUsersForAdmin()
                val requests = bloodRequestsRepository.listRecentForAdmin()
                val donations = donationsRepository.listRecentForAdmin()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isBootstrapping = false,
                        users = users,
                        requests = requests,
                        donations = donations,
                        error = null
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isBootstrapping = false,
                        error = e.toUserMessage()
                    )
                }
            }
        }
    }

    fun deleteBloodRequest(requestId: String) {
        viewModelScope.launch {
            runCatching {
                bloodRequestsRepository.delete(requestId)
            }.onSuccess {
                _uiState.update { it.copy(snackbar = "Request removed.") }
                refresh()
            }.onFailure { e ->
                _uiState.update { it.copy(snackbar = e.toUserMessage()) }
            }
        }
    }

    fun setUserBlocked(targetUid: String, blocked: Boolean) {
        viewModelScope.launch {
            runCatching {
                usersRepository.setUserBlockedByAdmin(targetUid, blocked)
            }.onSuccess {
                _uiState.update { it.copy(snackbar = if (blocked) "User blocked." else "User unblocked.") }
                refresh()
            }.onFailure { e ->
                _uiState.update { it.copy(snackbar = e.toUserMessage()) }
            }
        }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbar = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
                    return AdminViewModel() as T
                }
                throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
            }
        }
    }
}

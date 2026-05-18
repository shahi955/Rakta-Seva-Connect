package com.raktaseva.connect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.raktaseva.connect.di.AppGraph
import com.raktaseva.connect.model.UserRoles
import com.raktaseva.connect.repository.AuthRepository
import com.raktaseva.connect.repository.UsersRepository
import com.raktaseva.connect.util.AuthValidators
import com.raktaseva.connect.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminLoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val navigateToDashboard: Boolean = false
)

class AdminLoginViewModel(
    private val authRepository: AuthRepository,
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminLoginUiState())
    val uiState: StateFlow<AdminLoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }
    }

    fun submit() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val eErr = AuthValidators.validateEmail(email)
        val pErr = AuthValidators.validatePasswordForSignIn(password)
        if (eErr != null || pErr != null) {
            _uiState.update { it.copy(emailError = eErr, passwordError = pErr) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    generalError = null,
                    emailError = null,
                    passwordError = null,
                    navigateToDashboard = false
                )
            }
            runCatching {
                authRepository.signInWithEmail(email, password)
                val uid = authRepository.currentUser?.uid
                    ?: throw IllegalStateException("Signed in but user id is missing.")
                val profile = usersRepository.getUser(uid)
                    ?: throw IllegalStateException("User profile not found in Firestore.")
                if (profile.role != UserRoles.ADMIN) {
                    authRepository.signOut()
                    throw SecurityException("This account is not an administrator.")
                }
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, navigateToDashboard = true) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generalError = e.toUserMessage()
                    )
                }
            }
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(navigateToDashboard = false) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AdminLoginViewModel::class.java)) {
                    return AdminLoginViewModel(
                        AppGraph.authRepository,
                        AppGraph.usersRepository
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
            }
        }
    }
}

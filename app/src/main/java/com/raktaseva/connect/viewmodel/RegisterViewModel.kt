package com.raktaseva.connect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.raktaseva.connect.di.AppGraph
import com.raktaseva.connect.model.BloodGroupOptions
import com.raktaseva.connect.model.UserRoles
import com.raktaseva.connect.model.firestore.DonorAvailability
import com.raktaseva.connect.repository.AuthRepository
import com.raktaseva.connect.repository.FirestoreRepository
import com.raktaseva.connect.util.AuthValidators
import com.raktaseva.connect.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val bloodGroup: String = "",
    val city: String = "",
    val taluk: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val phoneError: String? = null,
    val bloodGroupError: String? = null,
    val cityError: String? = null,
    val talukError: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val isFetchingLocation: Boolean = false,
    val navigateHome: Boolean = false
)

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository,
    private val locationHelper: com.raktaseva.connect.location.LocationHelper? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChange(value: String) =
        _uiState.update { it.copy(fullName = value, nameError = null, generalError = null) }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }

    fun onPhoneChange(value: String) =
        _uiState.update { it.copy(phone = value, phoneError = null, generalError = null) }

    fun onBloodGroupChange(value: String) =
        _uiState.update { it.copy(bloodGroup = value, bloodGroupError = null, generalError = null) }

    fun onCityChange(value: String) =
        _uiState.update { it.copy(city = value, cityError = null, generalError = null) }

    fun onTalukChange(value: String) =
        _uiState.update { it.copy(taluk = value, talukError = null, generalError = null) }

    fun fetchLocation(hasPermission: Boolean) {
        if (!hasPermission) {
            _uiState.update { it.copy(locationError = "Location permission is required.") }
            return
        }
        val helper = locationHelper ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLocation = true, locationError = null) }
            val loc = helper.getCurrentLocationOrNull()
            if (loc != null) {
                _uiState.update {
                    it.copy(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        isFetchingLocation = false,
                        locationError = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isFetchingLocation = false,
                        locationError = "Could not fetch location. Ensure GPS is on."
                    )
                }
            }
        }
    }

    fun submit() {
        val s = _uiState.value
        val nameErr = AuthValidators.validateName(s.fullName)
        val emailErr = AuthValidators.validateEmail(s.email.trim())
        val passErr = AuthValidators.validatePassword(s.password)
        val phoneErr = AuthValidators.validatePhone(s.phone)
        val bloodErr = AuthValidators.validateBloodGroup(s.bloodGroup)
        val cityErr = AuthValidators.validateCity(s.city)
        val talukErr = AuthValidators.validateTaluk(s.taluk)

        if (listOf(nameErr, emailErr, passErr, phoneErr, bloodErr, cityErr, talukErr).any { it != null }) {
            _uiState.update {
                it.copy(
                    nameError = nameErr,
                    emailError = emailErr,
                    passwordError = passErr,
                    phoneError = phoneErr,
                    bloodGroupError = bloodErr,
                    cityError = cityErr,
                    talukError = talukErr
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    generalError = null,
                    navigateHome = false
                )
            }

            val email = s.email.trim()
            val phoneDigits = s.phone.filter { it.isDigit() }

            runCatching {
                authRepository.registerWithEmail(email, s.password)
                authRepository.updateDisplayName(s.fullName.trim())
                val uid = authRepository.currentUser?.uid
                    ?: throw IllegalStateException("Account created but profile could not be linked. Try signing in.")
                val profile = mutableMapOf<String, Any?>(
                    "displayName" to s.fullName.trim(),
                    "email" to email,
                    "phone" to phoneDigits,
                    "bloodGroup" to s.bloodGroup,
                    "city" to s.city.trim(),
                    "taluk" to s.taluk.trim(),
                    "role" to UserRoles.USER,
                    "isDonor" to true,
                    "availabilityStatus" to DonorAvailability.AVAILABLE,
                    "createdAt" to Timestamp.now(),
                    "isBlocked" to false
                )
                if (s.latitude != null && s.longitude != null) {
                    profile["latitude"] = s.latitude
                    profile["longitude"] = s.longitude
                }
                firestoreRepository.setUserProfile(uid, profile, merge = true)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, navigateHome = true) }
            }.onFailure { e ->
                runCatching { authRepository.signOut() }
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
        _uiState.update { it.copy(navigateHome = false) }
    }

    companion object {
        val BloodGroups = BloodGroupOptions.ALL

        fun factory(application: android.app.Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
                    return RegisterViewModel(
                        AppGraph.authRepository,
                        AppGraph.firestoreRepository,
                        com.raktaseva.connect.location.LocationHelper(application)
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
            }
        }
    }
}

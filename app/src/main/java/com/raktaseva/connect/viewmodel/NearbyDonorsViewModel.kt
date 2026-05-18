package com.raktaseva.connect.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.raktaseva.connect.di.AppGraph
import com.raktaseva.connect.location.LocationHelper
import com.raktaseva.connect.repository.UsersRepository
import com.raktaseva.connect.util.DonorDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NearbyDonorsUiState(
    val bloodGroup: String = "O+",
    val donors: List<DonorDistance> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null
)

class NearbyDonorsViewModel(
    application: Application,
    private val usersRepository: UsersRepository = AppGraph.usersRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val locationHelper: LocationHelper = LocationHelper(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NearbyDonorsUiState())
    val uiState: StateFlow<NearbyDonorsUiState> = _uiState.asStateFlow()

    fun onBloodGroupSelected(group: String) {
        _uiState.update { it.copy(bloodGroup = group, error = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Fetches fused location, saves it to Firestore for the current user, then loads donors
     * matching [NearbyDonorsUiState.bloodGroup] within 10 km (and 90-day eligibility on server query path).
     *
     * @param hasLocationPermission false when the user denied runtime permission — shows a friendly error.
     */
    fun refreshSearch(hasLocationPermission: Boolean) {
        if (!hasLocationPermission) {
            _uiState.update {
                it.copy(error = "Location permission is required to find nearby donors.")
            }
            return
        }
        if (!locationHelper.isLocationEnabled()) {
            _uiState.update {
                it.copy(error = "Please turn on device location (GPS or network) and try again.")
            }
            return
        }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(error = "You must be signed in.") }
            return
        }

        val bloodGroup = _uiState.value.bloodGroup

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val location = locationHelper.getCurrentLocationOrNull()
                    ?: throw IllegalStateException(
                        "Could not read your location. Move near a window or outdoors and try again."
                    )

                withContext(Dispatchers.IO) {
                    usersRepository.updateUserGeoLocation(uid, location.latitude, location.longitude)
                }

                val matches = withContext(Dispatchers.IO) {
                    usersRepository.findEligibleNearbyDonors(
                        bloodGroupNeeded = bloodGroup,
                        requestLatitude = location.latitude,
                        requestLongitude = location.longitude
                    ).filter { it.user.id != uid }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        donors = matches,
                        lastLatitude = location.latitude,
                        lastLongitude = location.longitude,
                        error = null
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message?.takeIf { m -> m.isNotBlank() }
                            ?: "Something went wrong while searching donors."
                    )
                }
            }
        }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(NearbyDonorsViewModel::class.java)) {
                    return NearbyDonorsViewModel(app) as T
                }
                throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
            }
        }
    }
}

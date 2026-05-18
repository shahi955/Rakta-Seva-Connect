package com.raktaseva.connect.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.raktaseva.connect.di.AppGraph
import com.raktaseva.connect.location.LocationHelper
import com.raktaseva.connect.model.firestore.EmergencyLevel
import com.raktaseva.connect.repository.EmergencyRequestRepository
import com.raktaseva.connect.repository.EmergencySubmitResult
import com.raktaseva.connect.util.EmergencyRequestValidators
import com.raktaseva.connect.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmergencyRequestUiState(
    val patientName: String = "",
    val bloodGroup: String = "O+",
    val unitsText: String = "1",
    val hospitalName: String = "",
    val contactNumber: String = "",
    val emergencyLevel: String = EmergencyLevel.HIGH,
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val patientNameError: String? = null,
    val bloodGroupError: String? = null,
    val unitsError: String? = null,
    val hospitalError: String? = null,
    val contactError: String? = null,
    val emergencyLevelError: String? = null,
    val notesError: String? = null,
    val locationError: String? = null,
    val generalError: String? = null,
    val isSubmitting: Boolean = false,
    val isFetchingLocation: Boolean = false,
    val submitSuccess: EmergencySubmitResult? = null
)

class EmergencyRequestViewModel(
    application: Application,
    private val emergencyRepository: EmergencyRequestRepository = AppGraph.emergencyRequestRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val locationHelper: LocationHelper = LocationHelper(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EmergencyRequestUiState())
    val uiState: StateFlow<EmergencyRequestUiState> = _uiState.asStateFlow()

    fun onPatientNameChange(v: String) =
        _uiState.update { it.copy(patientName = v, patientNameError = null, generalError = null) }

    fun onBloodGroupChange(v: String) =
        _uiState.update { it.copy(bloodGroup = v, bloodGroupError = null, generalError = null) }

    fun onUnitsChange(v: String) =
        _uiState.update { it.copy(unitsText = v, unitsError = null, generalError = null) }

    fun onHospitalChange(v: String) =
        _uiState.update { it.copy(hospitalName = v, hospitalError = null, generalError = null) }

    fun onContactChange(v: String) =
        _uiState.update { it.copy(contactNumber = v, contactError = null, generalError = null) }

    fun onEmergencyLevelChange(v: String) =
        _uiState.update { it.copy(emergencyLevel = v, emergencyLevelError = null, generalError = null) }

    fun onNotesChange(v: String) =
        _uiState.update { it.copy(notes = v, notesError = null, generalError = null) }

    fun clearGeneralError() {
        _uiState.update { it.copy(generalError = null) }
    }

    fun consumeSubmitSuccess() {
        _uiState.update { it.copy(submitSuccess = null) }
    }

    /**
     * Fills latitude/longitude from fused location when [hasLocationPermission] is true.
     */
    fun fetchRequestLocation(hasLocationPermission: Boolean) {
        if (!hasLocationPermission) {
            _uiState.update {
                it.copy(locationError = "Location permission is required to pin the request on the map.")
            }
            return
        }
        if (!locationHelper.isLocationEnabled()) {
            _uiState.update {
                it.copy(locationError = "Turn on device location and try again.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLocation = true, locationError = null, generalError = null) }
            runCatching {
                val loc = locationHelper.getCurrentLocationOrNull()
                    ?: throw IllegalStateException("Unable to read GPS coordinates right now.")
                _uiState.update {
                    it.copy(
                        isFetchingLocation = false,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        locationError = null
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isFetchingLocation = false,
                        locationError = e.toUserMessage()
                    )
                }
            }
        }
    }

    fun submit() {
        val s = _uiState.value
        val pErr = EmergencyRequestValidators.patientName(s.patientName)
        val bErr = EmergencyRequestValidators.bloodGroup(s.bloodGroup)
        val uErr = EmergencyRequestValidators.units(s.unitsText)
        val hErr = EmergencyRequestValidators.hospital(s.hospitalName)
        val cErr = EmergencyRequestValidators.contactNumber(s.contactNumber)
        val eErr = EmergencyRequestValidators.emergencyLevel(s.emergencyLevel)
        val nErr = EmergencyRequestValidators.notes(s.notes)
        val lErr = EmergencyRequestValidators.location(s.latitude, s.longitude)

        if (listOf(pErr, bErr, uErr, hErr, cErr, eErr, nErr, lErr).any { it != null }) {
            _uiState.update {
                it.copy(
                    patientNameError = pErr,
                    bloodGroupError = bErr,
                    unitsError = uErr,
                    hospitalError = hErr,
                    contactError = cErr,
                    emergencyLevelError = eErr,
                    notesError = nErr,
                    locationError = lErr
                )
            }
            return
        }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(generalError = "You must be signed in.") }
            return
        }

        val units = s.unitsText.toLongOrNull() ?: 1L
        val lat = s.latitude!!
        val lng = s.longitude!!

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, generalError = null) }
            runCatching {
                emergencyRepository.submitEmergencyRequest(
                    creatorUid = uid,
                    patientName = s.patientName,
                    bloodGroupNeeded = s.bloodGroup,
                    unitsRequired = units,
                    hospitalName = s.hospitalName,
                    latitude = lat,
                    longitude = lng,
                    contactNumber = s.contactNumber,
                    emergencyLevel = s.emergencyLevel,
                    notes = s.notes
                )
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(isSubmitting = false, submitSuccess = result, generalError = null)
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        generalError = e.toUserMessage()
                    )
                }
            }
        }
    }

    companion object {
        val EmergencyLevels = listOf(
            EmergencyLevel.LOW,
            EmergencyLevel.MEDIUM,
            EmergencyLevel.HIGH,
            EmergencyLevel.CRITICAL
        )

        fun factory(app: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(EmergencyRequestViewModel::class.java)) {
                    return EmergencyRequestViewModel(app) as T
                }
                throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
            }
        }
    }
}

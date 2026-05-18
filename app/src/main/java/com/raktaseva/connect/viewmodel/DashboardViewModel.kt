package com.raktaseva.connect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.raktaseva.connect.di.AppGraph
import com.raktaseva.connect.model.firestore.BloodRequestDocument
import com.raktaseva.connect.model.firestore.DonationDocument
import com.raktaseva.connect.model.firestore.DonorAvailability
import com.raktaseva.connect.model.firestore.NotificationDocument
import com.raktaseva.connect.model.firestore.NotificationTypes
import com.raktaseva.connect.model.firestore.RequestStatus
import com.raktaseva.connect.model.firestore.UserDocument
import com.raktaseva.connect.repository.BloodRequestsRepository
import com.raktaseva.connect.repository.DonationsRepository
import com.raktaseva.connect.repository.NotificationsRepository
import com.raktaseva.connect.repository.UsersRepository
import com.raktaseva.connect.util.DonorFilter
import com.raktaseva.connect.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Snapshot UI state for the signed-in donor/requester home tab.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val user: UserDocument? = null,
    val activeRequests: List<BloodRequestDocument> = emptyList(),
    val emergencyAlerts: List<NotificationDocument> = emptyList(),
    val recentDonations: List<DonationDocument> = emptyList(),
    val availabilitySaving: Boolean = false
)

/**
 * Loads profile, active blood requests, notifications, and donations for the current Firebase user.
 */
class DashboardViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val usersRepository: UsersRepository = AppGraph.usersRepository,
    private val bloodRequestsRepository: BloodRequestsRepository = AppGraph.bloodRequestsRepository,
    private val notificationsRepository: NotificationsRepository = AppGraph.notificationsRepository,
    private val donationsRepository: DonationsRepository = AppGraph.donationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val user = usersRepository.getUser(uid)
                val allRequests = bloodRequestsRepository.listByCreator(uid, limit = 40)
                val active = allRequests.filter { it.status == RequestStatus.OPEN }
                val alerts = notificationsRepository.listForUser(uid, limit = 30)
                    .filter { it.type == NotificationTypes.BLOOD_REQUEST }
                    .take(12)
                val donations = donationsRepository.listByDonor(uid, limit = 8)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        activeRequests = active,
                        emergencyAlerts = alerts,
                        recentDonations = donations,
                        error = null
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, error = e.toUserMessage())
                }
            }
        }
    }

    fun setAvailability(available: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        if (_uiState.value.user?.isDonor != true) return
        val status = if (available) DonorAvailability.AVAILABLE else DonorAvailability.UNAVAILABLE
        viewModelScope.launch {
            _uiState.update { it.copy(availabilitySaving = true) }
            runCatching {
                usersRepository.updateUser(
                    uid,
                    mapOf(
                        "availabilityStatus" to status,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val user = usersRepository.getUser(uid)
                _uiState.update { it.copy(user = user, availabilitySaving = false) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(availabilitySaving = false, error = e.toUserMessage())
                }
            }
        }
    }

    fun donationStatusText(): String {
        val last = _uiState.value.user?.lastDonationDate
        val eligible = DonorFilter.isEligibleAfterLastDonation(last)
        return if (last == null) {
            "No last donation date on file. If you are a donor, you are treated as eligible to respond."
        } else {
            val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val dateStr = fmt.format(Date(last.toDate().time))
            if (eligible) {
                "Last donation: $dateStr. You meet the 90-day gap and can donate again."
            } else {
                "Last donation: $dateStr. Complete 90 days since last donation before the next whole-blood donation."
            }
        }
    }

    fun formatTimestamp(ts: Timestamp?): String {
        if (ts == null) return "—"
        val fmt = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
        return fmt.format(Date(ts.toDate().time))
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                    return DashboardViewModel() as T
                }
                throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
            }
        }
    }
}

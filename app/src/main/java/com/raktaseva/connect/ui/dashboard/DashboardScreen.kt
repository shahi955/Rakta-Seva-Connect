@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.raktaseva.connect.ui.dashboard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.raktaseva.connect.R
import com.raktaseva.connect.model.firestore.BloodRequestDocument
import com.raktaseva.connect.model.firestore.DonationDocument
import com.raktaseva.connect.model.firestore.DonorAvailability
import com.raktaseva.connect.model.firestore.NotificationDocument
import com.raktaseva.connect.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    onOpenRequestDetail: (String) -> Unit,
    onOpenDonorsTab: () -> Unit,
    onOpenEmergencyTab: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val horizontalPadding = when {
        configuration.screenWidthDp >= 840 -> 32.dp
        configuration.screenWidthDp >= 600 -> 24.dp
        else -> 16.dp
    }

    if (state.isLoading && state.user == null && state.activeRequests.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroHeader(
                displayName = state.user?.displayName?.takeIf { it.isNotBlank() },
                emailFallback = state.user?.email.orEmpty(),
                bloodGroup = state.user?.bloodGroup
            )
        }

        state.error?.let { err ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = err,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        item {
            SectionHeader(
                icon = { Icon(Icons.Default.LocalHospital, contentDescription = null) },
                title = stringResource(R.string.dashboard_section_active_requests),
                subtitle = stringResource(R.string.dashboard_section_active_requests_sub)
            )
        }
        if (state.activeRequests.isEmpty()) {
            item { EmptyHint(stringResource(R.string.dashboard_no_active_requests)) }
        } else {
            items(
                items = state.activeRequests,
                key = { it.id } ){ req ->
                ActiveRequestCard(request = req, onClick = { onOpenRequestDetail(req.id) })
            }
        }

        item {
            SectionHeader(
                icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                title = stringResource(R.string.dashboard_section_nearby_donors),
                subtitle = stringResource(R.string.dashboard_section_nearby_donors_sub)
            )
        }
        item {
            NearbyDonorsPromoCard(onOpenDonorsTab = onOpenDonorsTab)
        }

        item {
            SectionHeader(
                icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                title = stringResource(R.string.dashboard_section_emergency_alerts),
                subtitle = stringResource(R.string.dashboard_section_emergency_alerts_sub)
            )
        }
        if (state.emergencyAlerts.isEmpty()) {
            item { EmptyHint(stringResource(R.string.dashboard_no_alerts)) }
        } else {
            items(
                items = state.emergencyAlerts,
                key = { it.id }
            ) { alert ->
                AlertCard(
                    notification = alert,
                    formatTime = { it.toString() }
                )
            }
        }

        item {
            SectionHeader(
                icon = { Icon(Icons.Default.VolunteerActivism, contentDescription = null) },
                title = stringResource(R.string.dashboard_section_donation_status),
                subtitle = stringResource(R.string.dashboard_section_donation_sub)
            )
        }
        item {
            DonationStatusCard(
                statusText = viewModel.donationStatusText(),
                donations = state.recentDonations,
                formatTime = viewModel::formatTimestamp
            )
        }

        item {
            SectionHeader(
                icon = { Icon(Icons.Default.ToggleOn, contentDescription = null) },
                title = stringResource(R.string.dashboard_section_availability),
                subtitle = stringResource(R.string.dashboard_section_availability_sub)
            )
        }
        item {
            AvailabilityCard(
                isDonor = state.user?.isDonor == true,
                available = state.user?.availabilityStatus == DonorAvailability.AVAILABLE,
                saving = state.availabilitySaving,
                onToggle = { on -> viewModel.setAvailability(on) }
            )
        }

        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dashboard_quick_emergency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.dashboard_quick_emergency_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                    Button(onClick = onOpenEmergencyTab) {
                        Text(stringResource(R.string.emergency_request_nav))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun HeroHeader(displayName: String?, emailFallback: String, bloodGroup: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.dashboard_hero_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = displayName ?: emailFallback.ifBlank { stringResource(R.string.dashboard_guest) },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
            bloodGroup?.let { bg ->
                if (bg.isNotBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.dashboard_your_blood, bg)) },
                        leadingIcon = { Icon(Icons.Default.Bloodtype, contentDescription = null) },
                        modifier = Modifier.padding(top = 10.dp),
                        enabled = false,
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Box(modifier = Modifier.padding(10.dp)) {
                icon()
            }
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun ActiveRequestCard(request: BloodRequestDocument, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Patient: ${request.patientName.ifBlank { "—" }}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text(request.emergencyLevel) },
                    enabled = false
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.dashboard_request_blood_units, request.bloodGroupNeeded, request.unitsRequired),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = request.hospitalName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun NearbyDonorsPromoCard(onOpenDonorsTab: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dashboard_nearby_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.dashboard_nearby_card_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            TextButton(onClick = onOpenDonorsTab) {
                Text(stringResource(R.string.dashboard_open_donors))
            }
        }
    }
}

@Composable
private fun AlertCard(notification: NotificationDocument, formatTime: (Timestamp?) -> String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.read) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(notification.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    formatTime(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun DonationStatusCard(
    statusText: String,
    donations: List<DonationDocument>,
    formatTime: (Timestamp?) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(statusText, style = MaterialTheme.typography.bodyLarge)
            if (donations.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                Text(
                    stringResource(R.string.dashboard_recent_donations),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                donations.take(4).forEach { d ->
                    Text(
                        "· ${formatTime(d.donationDate)} — ${d.units} unit(s)" +
                            (d.hospitalName?.let { " @ $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailabilityCard(
    isDonor: Boolean,
    available: Boolean,
    saving: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!isDonor) {
                Text(
                    stringResource(R.string.dashboard_availability_non_donor),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.dashboard_available_label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.dashboard_available_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.width(28.dp).height(28.dp))
                    } else {
                        Switch(
                            checked = available,
                            onCheckedChange = onToggle
                        )
                    }
                }
            }
        }
    }
}

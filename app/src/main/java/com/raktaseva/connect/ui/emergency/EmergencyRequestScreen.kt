package com.raktaseva.connect.ui.emergency

import android.Manifest
import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raktaseva.connect.R
import com.raktaseva.connect.location.LocationHelper
import com.raktaseva.connect.ui.auth.LoadingOverlay
import com.raktaseva.connect.viewmodel.EmergencyRequestViewModel
import com.raktaseva.connect.viewmodel.RegisterViewModel

private val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyRequestScreen(
    onBack: () -> Unit = {},
    embedded: Boolean = false,
    onSuccessDismiss: () -> Unit = {},
    viewModel: EmergencyRequestViewModel = viewModel(
        factory = EmergencyRequestViewModel.factory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        viewModel.fetchRequestLocation(hasLocationPermission = granted)
    }

    fun dismissSuccessDialog() {
        viewModel.consumeSubmitSuccess()
        if (embedded) onSuccessDismiss() else onBack()
    }

    state.submitSuccess?.let { success ->
        AlertDialog(
            onDismissRequest = { dismissSuccessDialog() },
            title = { Text(stringResource(R.string.emergency_success_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.emergency_success_message,
                        success.notificationsSent,
                        success.requestId
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { dismissSuccessDialog() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.emergency_request_title)) },
                navigationIcon = {
                    if (!embedded) {
                        IconButton(onClick = onBack, enabled = !state.isSubmitting) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.emergency_request_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = state.patientName,
                            onValueChange = viewModel::onPatientNameChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.field_patient_name)) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            isError = state.patientNameError != null,
                            supportingText = {
                                state.patientNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        var bloodExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = bloodExpanded,
                            onExpandedChange = { bloodExpanded = !bloodExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = state.bloodGroup,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.field_blood_group)) },
                                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                isError = state.bloodGroupError != null,
                                supportingText = {
                                    state.bloodGroupError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = bloodExpanded,
                                onDismissRequest = { bloodExpanded = false }
                            ) {
                                RegisterViewModel.BloodGroups.forEach { group ->
                                    DropdownMenuItem(
                                        text = { Text(group) },
                                        onClick = {
                                            viewModel.onBloodGroupChange(group)
                                            bloodExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = state.unitsText,
                            onValueChange = viewModel::onUnitsChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.field_units_needed)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = state.unitsError != null,
                            supportingText = {
                                state.unitsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = state.hospitalName,
                            onValueChange = viewModel::onHospitalChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.field_hospital)) },
                            leadingIcon = { Icon(Icons.Default.LocalHospital, contentDescription = null) },
                            singleLine = true,
                            isError = state.hospitalError != null,
                            supportingText = {
                                state.hospitalError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                if (LocationHelper.hasLocationPermission(context)) {
                                    viewModel.fetchRequestLocation(true)
                                } else {
                                    permissionLauncher.launch(LocationPermissions)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isFetchingLocation && !state.isSubmitting
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (state.latitude != null) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (state.latitude != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (state.latitude != null) "Location Set ✅" else stringResource(R.string.use_location_for_request)
                                )
                            }
                        }

                        if (state.isFetchingLocation) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.getting_location))
                            }
                        }
                        
                        state.locationError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = state.contactNumber,
                            onValueChange = viewModel::onContactChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.field_contact_number)) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = state.contactError != null,
                            supportingText = {
                                state.contactError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        var levelExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = levelExpanded,
                            onExpandedChange = { levelExpanded = !levelExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = state.emergencyLevel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.field_emergency_level)) },
                                leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                isError = state.emergencyLevelError != null,
                                supportingText = {
                                    state.emergencyLevelError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = levelExpanded,
                                onDismissRequest = { levelExpanded = false }
                            ) {
                                EmergencyRequestViewModel.EmergencyLevels.forEach { level ->
                                    DropdownMenuItem(
                                        text = { Text(level) },
                                        onClick = {
                                            viewModel.onEmergencyLevelChange(level)
                                            levelExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = viewModel::onNotesChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.field_notes)) },
                            minLines = 3,
                            isError = state.notesError != null,
                            supportingText = {
                                state.notesError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        )

                        state.generalError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = viewModel::submit,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isSubmitting && !state.isFetchingLocation
                        ) {
                            Text(stringResource(R.string.submit_emergency_request))
                        }
                    }
                }
            }
            LoadingOverlay(visible = state.isSubmitting)
        }
    }
}

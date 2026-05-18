package com.raktaseva.connect.ui.auth

import android.Manifest
import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raktaseva.connect.R
import com.raktaseva.connect.viewmodel.RegisterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateLogin: () -> Unit,
    onRegistered: () -> Unit,
    viewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModel.factory(LocalContext.current.applicationContext as Application)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        viewModel.fetchLocation(granted)
    }

    LaunchedEffect(state.navigateHome) {
        if (state.navigateHome) {
            onRegistered()
            viewModel.consumeNavigation()
        }
    }

    LaunchedEffect(state.generalError) {
        state.generalError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier.height(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.register_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.register_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        OutlinedTextField(
                            value = state.fullName,
                            onValueChange = viewModel::onFullNameChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.full_name)) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            isError = state.nameError != null,
                            supportingText = {
                                state.nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.email,
                            onValueChange = viewModel::onEmailChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.email)) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            isError = state.emailError != null,
                            supportingText = {
                                state.emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.password)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            isError = state.passwordError != null,
                            supportingText = {
                                state.passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.phone,
                            onValueChange = viewModel::onPhoneChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.phone)) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = state.phoneError != null,
                            supportingText = {
                                state.phoneError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = state.bloodGroup,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.blood_group)) },
                                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                isError = state.bloodGroupError != null,
                                supportingText = {
                                    state.bloodGroupError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                RegisterViewModel.BloodGroups.forEach { group ->
                                    DropdownMenuItem(
                                        text = { Text(group) },
                                        onClick = {
                                            viewModel.onBloodGroupChange(group)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.city,
                            onValueChange = viewModel::onCityChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.city)) },
                            leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                            singleLine = true,
                            isError = state.cityError != null,
                            supportingText = {
                                state.cityError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.taluk,
                            onValueChange = viewModel::onTalukChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.taluk)) },
                            leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                            singleLine = true,
                            isError = state.talukError != null,
                            supportingText = {
                                state.talukError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedButton(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isFetchingLocation
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (state.latitude != null) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (state.latitude != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (state.latitude != null) "Location Linked ✅" else "Link My Location")
                            }
                        }
                        
                        state.locationError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = viewModel::submit,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading
                        ) {
                            Text(stringResource(R.string.create_account))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.have_account),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    TextButton(onClick = onNavigateLogin, enabled = !state.isLoading) {
                        Text(stringResource(R.string.sign_in))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            LoadingOverlay(visible = state.isLoading)
        }
    }
}

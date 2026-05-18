package com.raktaseva.connect.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.raktaseva.connect.R
import com.raktaseva.connect.di.AppGraph
import com.raktaseva.connect.model.UserRoles
import com.raktaseva.connect.model.firestore.UserDocument
import kotlinx.coroutines.launch

@Composable
fun ProfileTabScreen(
    onSignedOut: () -> Unit,
    onOpenAdminPanel: () -> Unit = {}
) {
    val authUser = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()
    var firestoreUser by remember { mutableStateOf<UserDocument?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(authUser?.uid) {
        val uid = authUser?.uid ?: return@LaunchedEffect
        isLoading = true
        firestoreUser = runCatching { AppGraph.usersRepository.getUser(uid) }.getOrNull()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (firestoreUser != null) {
            val user = firestoreUser!!
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileDetailRow(Icons.Default.Person, stringResource(R.string.full_name), user.displayName)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    ProfileDetailRow(Icons.Default.Email, stringResource(R.string.email), user.email)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    ProfileDetailRow(Icons.Default.Phone, stringResource(R.string.phone), user.phone)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    ProfileDetailRow(Icons.Default.Favorite, stringResource(R.string.blood_group), user.bloodGroup ?: "—")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    ProfileDetailRow(Icons.Default.LocationCity, stringResource(R.string.city), user.city ?: "—")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    ProfileDetailRow(Icons.Default.Map, stringResource(R.string.taluk), user.taluk ?: "—")
                }
            }

            if (user.role == UserRoles.ADMIN) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onOpenAdminPanel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_open_admin))
                }
            }
        } else {
            // Fallback for just Auth user if Firestore profile failed to load
            Text(text = authUser?.email ?: "Guest", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedButton(
            onClick = {
                scope.launch {
                    AppGraph.authRepository.signOut()
                    onSignedOut()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.sign_out))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

package com.raktaseva.connect.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.raktaseva.connect.R
import com.raktaseva.connect.RaktaApplication
import com.raktaseva.connect.fcm.FcmTokenManager
import com.raktaseva.connect.navigation.AuthRoutes
import com.raktaseva.connect.ui.dashboard.DashboardScreen
import com.raktaseva.connect.ui.donors.NearbyDonorsScreen
import com.raktaseva.connect.ui.emergency.EmergencyRequestScreen
import com.raktaseva.connect.ui.profile.ProfileTabScreen
import com.raktaseva.connect.viewmodel.DashboardViewModel

/**
 * Primary authenticated experience: bottom navigation hosting dashboard, donors, emergency flow,
 * and profile. Requests notification permission on Android 13+ so FCM alerts can post.
 */
@Composable
fun MainShell(
    navController: NavHostController,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val dashboardVm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* denied: emergency notifications may be silent until user enables in settings */ }

    LaunchedEffect(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(perm)
            }
        }
        if (FirebaseAuth.getInstance().currentUser != null) {
            val app = context.applicationContext as? RaktaApplication ?: return@LaunchedEffect
            FcmTokenManager.syncCurrentUserTokenToFirestore(app)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            Icons.Default.Dashboard,
                            contentDescription = stringResource(R.string.nav_dashboard)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_dashboard)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = stringResource(R.string.nav_donors)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_donors)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = stringResource(R.string.nav_emergency)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_emergency)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = stringResource(R.string.nav_profile)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_profile)) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    viewModel = dashboardVm,
                    onOpenRequestDetail = { id ->
                        navController.navigate(AuthRoutes.requestDetailRoute(id)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenDonorsTab = { selectedTab = 1 },
                    onOpenEmergencyTab = { selectedTab = 2 }
                )
                1 -> NearbyDonorsScreen(onBack = {}, embedded = true)
                2 -> EmergencyRequestScreen(
                    onBack = {},
                    embedded = true,
                    onSuccessDismiss = { dashboardVm.refresh() }
                )
                3 -> ProfileTabScreen(
                    onSignedOut = onSignedOut,
                    onOpenAdminPanel = {
                        navController.navigate(AuthRoutes.ADMIN_HOME)
                    }
                )
            }
        }
    }
}

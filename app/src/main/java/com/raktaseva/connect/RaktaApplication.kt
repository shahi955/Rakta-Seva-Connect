package com.raktaseva.connect

import android.app.Application
import com.raktaseva.connect.data.firebase.FirebaseBootstrap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Register in AndroidManifest: android:name=".RaktaApplication"
 *
 * Holds a process-wide coroutine scope for short background tasks (FCM token sync).
 * Cancels outstanding work in [onTerminate] (best-effort; not guaranteed on all devices).
 */
class RaktaApplication : Application() {

    lateinit var applicationScope: CoroutineScope
        private set

    private val _pendingOpenRequestId = MutableStateFlow<String?>(null)
    val pendingOpenRequestId: StateFlow<String?> = _pendingOpenRequestId.asStateFlow()

    fun enqueueOpenRequestFromNotification(requestId: String) {
        _pendingOpenRequestId.value = requestId
    }

    fun consumePendingOpenRequest() {
        _pendingOpenRequestId.value = null
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        FirebaseBootstrap.ensureInitialized(this)
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }
}

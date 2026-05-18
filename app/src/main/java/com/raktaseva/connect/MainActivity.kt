package com.raktaseva.connect

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.raktaseva.connect.navigation.RaktaNavHost
import com.raktaseva.connect.notification.RaktaNotificationHelper
import com.raktaseva.connect.ui.theme.RaktaSevaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ingestNotificationLaunchIntent(intent)

        setContent {
            RaktaSevaTheme {
                val pendingOpenRequestId = " "

                Surface(modifier = Modifier.fillMaxSize()) {
                    RaktaNavHost(
                        pendingOpenRequestId = pendingOpenRequestId,
                        onConsumePendingOpenRequest = { }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ingestNotificationLaunchIntent(intent)
    }

    private fun ingestNotificationLaunchIntent(intent: Intent?) {
        val id = RaktaNotificationHelper.readRequestIdFromExtras(intent?.extras)
        if (!id.isNullOrBlank()) {
            (applicationContext as RaktaApplication)
                .enqueueOpenRequestFromNotification(id)
        }
    }
}
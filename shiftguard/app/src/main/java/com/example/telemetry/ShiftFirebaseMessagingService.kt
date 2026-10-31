package com.example.telemetry

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ShiftFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "ShiftFCM"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Registration Token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains data payload
        val data = remoteMessage.data
        Log.d(TAG, "Message data payload: $data")

        val action = data["action"] ?: data["command"] ?: ""
        val isForceSync = action.equals("SYNC_LOCATION", ignoreCase = true) ||
                action.equals("FORCE_TELEMETRY", ignoreCase = true) ||
                action.equals("PING", ignoreCase = true) ||
                remoteMessage.priority == RemoteMessage.PRIORITY_HIGH

        if (isForceSync) {
            Log.i(TAG, "Received high-priority server sync trigger! Forcing immediate background telemetry push...")
            LocationTelemetryWorker.enqueueImmediateSync(applicationContext, source = "FCM_PUSH")
        }
    }
}

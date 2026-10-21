package com.inputoverlay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.inputoverlay.service.OverlayService

class OverlayReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "Received broadcast action: $action")

        val serviceIntent = Intent(context, OverlayService::class.java).apply {
            this.action = action
            intent.extras?.let { putExtras(it) }
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start OverlayService from BroadcastReceiver", e)
        }
    }

    companion object {
        private const val TAG = "OverlayReceiver"
    }
}

package com.tkrynski.background_location_handler
import android.content.Intent
import android.content.Context
import android.content.BroadcastReceiver


class ShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            //stop the service when phone is shotdown
//            LocationsUpdateRequest.getPendingIntent(context).cancel()
            //reset flag in preference
        } catch (e: Exception) {
        }
    }
}
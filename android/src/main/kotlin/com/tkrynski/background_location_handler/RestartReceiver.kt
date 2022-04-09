package com.tkrynski.background_location_handler
import android.content.Intent
import android.content.Context
import android.content.BroadcastReceiver
import android.util.Log
import java.util.concurrent.TimeUnit
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import android.app.PendingIntent


private const val TAG = "RestartReceiver"
class RestartReceiver : BroadcastReceiver() {

    // Stores parameters for requests to the FusedLocationProviderApi.
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        // Sets the desired interval for active location updates. This interval is inexact. You
        // may not receive updates at all if no location sources are available, or you may
        // receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        //
        // IMPORTANT NOTE: Apps running on "O" devices (regardless of targetSdkVersion) may
        // receive updates less frequently than this interval when the app is no longer in the
        // foreground.
        interval = TimeUnit.SECONDS.toMillis(5)

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        fastestInterval = TimeUnit.SECONDS.toMillis(1)

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        maxWaitTime = TimeUnit.SECONDS.toMillis(10)

        // sets the minimum distance travelled in meters before getting a location update
        smallestDisplacement = 0.0f

        // sets the priority for accuracy vs power consumption
        // we want high accuracy (for now)
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "tkrynski onReceive new!")
        try {
            val locationUpdatePendingIntent: PendingIntent by lazy {
                val intent = Intent(context, LocationUpdatesBroadcastReceiver::class.java)
                intent.action = LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            }

            Log.i(TAG, "tkrynski requesting location updates!")
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.requestLocationUpdates(locationRequest, locationUpdatePendingIntent)
            Log.i(TAG, "tkrynski requested location updates!")
        } catch (e: Exception) {
//            LogUtils.crashlytics(e)
        }
    }
}

package com.tkrynski.background_location_handler

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import android.content.Intent
import android.app.PendingIntent
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient

private const val TAG = "BackgroundWorker"


class BackgroundWorker(appContext: Context, workerParams: WorkerParameters):
        Worker(appContext, workerParams) {

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
        interval = TimeUnit.MINUTES.toMillis(5)

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        fastestInterval = TimeUnit.MINUTES.toMillis(1)

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        maxWaitTime = TimeUnit.MINUTES.toMillis(10)

        // sets the minimum distance travelled in meters before getting a location update
        smallestDisplacement = 10.0f

        // sets the priority for accuracy vs power consumption
        // we want high accuracy (for now)
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val locationUpdatePendingIntent: PendingIntent by lazy {
        val intent = Intent(appContext, LocationUpdatesBroadcastReceiver::class.java)
        intent.action = LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES
        PendingIntent.getBroadcast(appContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

    override fun doWork(): Result {
        Log.e(TAG, "doWork!")

        // set the object variables so that we can be shut down later
        if (mFusedLocationClient == null) {
            mFusedLocationClient = fusedLocationClient
        }
        if (mLocationUpdatePendingIntent == null) {
            mLocationUpdatePendingIntent = locationUpdatePendingIntent
        }

        // Check for location
        mFusedLocationClient!!.removeLocationUpdates(mLocationUpdatePendingIntent!!)
        mFusedLocationClient!!.requestLocationUpdates(locationRequest, mLocationUpdatePendingIntent!!)

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }

    companion object {
        private var mFusedLocationClient: FusedLocationProviderClient? = null
        private var mLocationUpdatePendingIntent: PendingIntent? = null

        public fun removeLocationUpdates() {
            if (mFusedLocationClient != null && mLocationUpdatePendingIntent != null) {
                mFusedLocationClient!!.removeLocationUpdates(mLocationUpdatePendingIntent!!)
            }
        }
    }
}

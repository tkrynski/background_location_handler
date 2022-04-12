package com.tkrynski.background_location_handler

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import android.content.Intent
import android.app.PendingIntent
import android.util.Log
import io.flutter.plugin.common.MethodChannel

import android.os.Handler
import android.os.Looper
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.util.concurrent.ListenableFuture
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.plugins.shim.ShimPluginRegistry
import io.flutter.plugin.common.MethodCall
import io.flutter.view.FlutterCallbackInformation
import io.flutter.view.FlutterMain
import java.util.*

private const val TAG = "BackgroundWorker"
private const val PKG = "com.tkrynski.background_location_handler";

class BackgroundWorker(private val context: Context,
                       private val workerParams: WorkerParameters):
        ListenableWorker(context, workerParams) {

    companion object {
        const val TAG = "BackgroundWorker"

        private var mFusedLocationClient: FusedLocationProviderClient? = null
        private var mLocationUpdatePendingIntent: PendingIntent? = null

        public fun removeLocationUpdates() {
            if (mFusedLocationClient != null && mLocationUpdatePendingIntent != null) {
                Log.i(TAG, "Removing location updates")
                mFusedLocationClient!!.removeLocationUpdates(mLocationUpdatePendingIntent!!)
            }
        }
    }

    private val randomThreadIdentifier = Random().nextInt()
    private lateinit var engine: FlutterEngine

    private var destroying = false
    private var startTime: Long = 0
    private val resolvableFuture = ResolvableFuture.create<Result>()

    override fun onStopped() {
        stopEngine(null)
    }

    private fun stopEngine(result: Result?) {
        Log.i(TAG, "Stopping Engine")
        val fetchDuration = System.currentTimeMillis() - startTime

        // No result indicates we were signalled to stop by WorkManager.  The result is already
        // STOPPED, so no need to resolve another one.
        if (result != null) {
            resolvableFuture.set(result)
        }

        // If stopEngine is called from `onStopped`, it may not be from the main thread.
        Handler(Looper.getMainLooper()).post {
            if (!destroying) {
                if (this::engine.isInitialized)
                    engine.destroy()
                destroying = true
            }
        }
    }

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

    private val locationUpdatePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, LocationUpdatesBroadcastReceiver::class.java)
        intent.action = LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)


    override fun startWork(): ListenableFuture<Result> {

        Log.e(TAG, "startWork!")

        // set the object variables so that we can be shut down later
        if (mFusedLocationClient == null) {
            mFusedLocationClient = fusedLocationClient
        }
        if (mLocationUpdatePendingIntent == null) {
            mLocationUpdatePendingIntent = locationUpdatePendingIntent
        }

        // Check for location
        mFusedLocationClient!!.requestLocationUpdates(locationRequest, mLocationUpdatePendingIntent!!)
        Log.i(TAG, "requested location updates")

        // Indicate whether the work finished successfully with the Result
        return resolvableFuture
    }

/*
 * Copyright (C) 2020 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tkrynski.background_location_handler

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationResult
import java.util.Date
import java.util.concurrent.Executors
import io.flutter.plugin.common.MethodChannel

private const val TAG = "LUBroadcastReceiver"


/**
 * Receiver for handling location updates.
 *
 * For apps targeting API level O and above
 * {@link android.app.PendingIntent#getBroadcast(Context, int, Intent, int)} should be used when
 * requesting location updates in the background. Due to limits on background services,
 * {@link android.app.PendingIntent#getService(Context, int, Intent, int)} should NOT be used.
 *
 *  Note: Apps running on "O" devices (regardless of targetSdkVersion) may receive updates
 *  less frequently than the interval specified in the
 *  {@link com.google.android.gms.location.LocationRequest} when the app is no longer in the
 *  foreground.
 */
class LocationUpdatesBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.e(TAG, "onReceive() context:$context, intent:$intent, channel: $channel")
        if (intent.action == ACTION_PROCESS_UPDATES) {
            Log.e(TAG, "ACTION_PROCESS_UPDATES")

            // Checks for location availability changes.
            LocationAvailability.extractLocationAvailability(intent)?.let { locationAvailability ->
                Log.e(TAG, "Has LocationAvailability")
                if (!locationAvailability.isLocationAvailable) {
                    Log.e(TAG, "Location services are no longer available!")
                }
            }

            LocationResult.extractResult(intent)?.let { locationResult ->
                Log.e(TAG, "Has LocationResult")
                val location = locationResult.getLastLocation()
                if (location != null) {
                    Log.e(TAG, "GOT location!! $location")
                    val locationMap = HashMap<String, Any>()
                    locationMap["latitude"] = location.latitude
                    locationMap["longitude"] = location.longitude
                    locationMap["altitude"] = location.altitude
                    locationMap["accuracy"] = location.accuracy.toDouble()
                    locationMap["bearing"] = location.bearing.toDouble()
                    locationMap["speed"] = location.speed.toDouble()
                    locationMap["time"] = location.time.toDouble()
                    locationMap["is_mock"] = location.isFromMockProvider
                    channel!!.invokeMethod("location", locationMap, null)
                    Log.d(TAG, "Invoked location method on channel: $channel, locationMap $locationMap")
                }
            }
        }
    }

    companion object {
        public var channel: MethodChannel? = null

        const val ACTION_PROCESS_UPDATES = "com.tkrynski.background_location_handler.PROCESS_UPDATES"
    }
}
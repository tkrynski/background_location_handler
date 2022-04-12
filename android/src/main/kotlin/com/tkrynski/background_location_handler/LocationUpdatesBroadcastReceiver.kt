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

private const val TAG = "LUBroadcastReceiver"
private const val PKG = "com.tkrynski.background_location_handler";
private const val BACKGROUND_CHANNEL_NAME = "$PKG/background_channel_work_manager"
private const val CALLBACK_DISPATCHER_HANDLE_KEY = "$PKG.CALLBACK_DISPATCHER_HANDLE_KEY"
private const val CALLBACK_EXTRAS_KEY = "com.tkrynski.background_location_handler.CALLBACK_EXTRAS"


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
    private lateinit var engine: FlutterEngine

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

                    // all this code is just to set up the callback handler
                    engine = FlutterEngine(context)
                    FlutterMain.ensureInitializationComplete(context, null)

                    val callbackHandle = context.getSharedPreferences("flutter_workmanager_plugin", Context.MODE_PRIVATE).getLong(CALLBACK_DISPATCHER_HANDLE_KEY, -1L)
                    val extras = context.getSharedPreferences("flutter_workmanager_plugin", Context.MODE_PRIVATE).getString(CALLBACK_EXTRAS_KEY, "")
                    locationMap["extras"] = extras!!;

                    val callbackInfo = FlutterCallbackInformation.lookupCallbackInformation(callbackHandle)
                    val dartBundlePath = FlutterMain.findAppBundlePath()

                    //Backwards compatibility with v1. We register all the user's plugins.
                    BackgroundLocationHandlerPlugin.pluginRegistryCallback?.registerWith(ShimPluginRegistry(engine))

                    // execute the callback handler.  This sets up the background method channel handler
                    // so that it can handle the location update to follow
                    engine.dartExecutor.executeDartCallback(DartExecutor.DartCallback(context.assets, dartBundlePath, callbackInfo))

                    // Now, finally invoke the location update in the method channel
                    val backgroundChannel = MethodChannel(engine.dartExecutor, BACKGROUND_CHANNEL_NAME)
                    backgroundChannel!!.invokeMethod("location", locationMap, null)
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
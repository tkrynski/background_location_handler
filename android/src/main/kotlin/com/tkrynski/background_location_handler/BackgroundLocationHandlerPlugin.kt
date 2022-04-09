package com.tkrynski.background_location_handler

import android.content.Context
import androidx.annotation.NonNull
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.util.concurrent.TimeUnit
import android.util.Log
import android.content.Intent
import android.app.PendingIntent
import com.google.android.gms.location.FusedLocationProviderClient

import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.PluginRegistry
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest


private const val TAG = "BLHandlerPlugin"
private const val SHARED_PREFS_FILE_NAME = "flutter_workmanager_plugin"
private const val PKG = "com.tkrynski.background_location_handler"
private const val CALLBACK_DISPATCHER_HANDLE_KEY = "$PKG.CALLBACK_DISPATCHER_HANDLE_KEY"
private const val CALLBACK_EXTRAS_KEY = "$PKG.CALLBACK_EXTRAS"
private const val WORK_REQUEST_TAG = "$PKG.BACKGROUND_LOCATION_WORK_REQUEST"

/** BackgroundLocationHandlerPlugin */
class BackgroundLocationHandlerPlugin: FlutterPlugin, MethodCallHandler {

  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel

  /**
   * Context that is set once attached to a FlutterEngine.
   * Context should no longer be referenced when detached.
   */
  private lateinit var context: Context
  private lateinit var mBackgroundWorkRequest : WorkRequest
  private lateinit var mFusedLocationClient: FusedLocationProviderClient

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    this.onAttachedToEngine(flutterPluginBinding.applicationContext, flutterPluginBinding.binaryMessenger)

//    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "background_location_handler")
//    channel.setMethodCallHandler(this)

  }


  private fun onAttachedToEngine(ctx: Context, messenger: BinaryMessenger) {
    Log.e(TAG, "onAttachedToEngine!")
    context = ctx
    channel = MethodChannel(messenger, "$PKG/foreground_channel_work_manager")
    channel.setMethodCallHandler(this)

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // create the background work request
    mBackgroundWorkRequest = PeriodicWorkRequestBuilder<BackgroundWorker>(15, TimeUnit.MINUTES)
            .addTag(WORK_REQUEST_TAG).build()
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

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    Log.e(TAG, "onMethodCall ${call.method}")
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else if (call.method == "start_location_monitoring") {
      // create work request
//      Log.e(TAG,"Enqueueing WorkManager request")
//      WorkManager.getInstance(context).enqueue(mBackgroundWorkRequest)
//      Log.e(TAG,"Done Enqueueing WorkManager request")
      Log.e(TAG,"Reuesting location updates")
      mFusedLocationClient.requestLocationUpdates(locationRequest, locationUpdatePendingIntent)
      result.success("")
    } else if (call.method == "stop_location_monitoring") {
      mFusedLocationClient.removeLocationUpdates(locationUpdatePendingIntent)
//      BackgroundWorker.removeLocationUpdates()
//      WorkManager.getInstance(context).cancelAllWorkByTag(WORK_REQUEST_TAG)
      result.success("")
    } else if (call.method == "start_visit_monitoring") {
      // do nothing
      result.success("")
    } else if (call.method == "stop_visit_monitoring") {
      // do nothing
      result.success("")
    } else if (call.method == "initialize") {
      saveCallbackDispatcherHandleKey(context, call.argument<Long>("callbackHandle")!!)
      saveCallbackExtrasKey(context, call.argument<String>("extras")!!)
      result.success("")
    } else {
      result.notImplemented()
    }
  }

  fun saveCallbackExtrasKey(ctx: Context, extras: String) {
    ctx.getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(CALLBACK_EXTRAS_KEY, extras)
            .apply()
  }
  fun saveCallbackDispatcherHandleKey(ctx: Context, callbackHandle: Long) {
    ctx.getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(CALLBACK_DISPATCHER_HANDLE_KEY, callbackHandle)
            .apply()
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    onDetachedFromEngine()
  }

  private fun onDetachedFromEngine() {
    channel.setMethodCallHandler(null)
  }


  companion object {
    var pluginRegistryCallback: PluginRegistry.PluginRegistrantCallback? = null

    @JvmStatic
    fun registerWith(registrar: PluginRegistry.Registrar) {
      val plugin = BackgroundLocationHandlerPlugin()
      plugin.onAttachedToEngine(registrar.context(), registrar.messenger())
      registrar.addViewDestroyListener {
        plugin.onDetachedFromEngine()
        false
      }
    }

    @Deprecated(message = "Use the Android v2 embedding method.")
    @JvmStatic
    fun setPluginRegistrantCallback(pluginRegistryCallback: PluginRegistry.PluginRegistrantCallback) {
      BackgroundLocationHandlerPlugin.pluginRegistryCallback = pluginRegistryCallback
    }
  }
}

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

private const val TAG = "BLHandlerPlugin"

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
  private var context: Context? = null

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    Log.e(TAG, "onAttachedToEngine!")

    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "background_location_handler")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }


  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    Log.e(TAG, "onMethodCall ${call.method}")
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else if (call.method == "start_location_monitoring") {
      // create work request
      val uploadWorkRequest: WorkRequest = PeriodicWorkRequestBuilder<BackgroundWorker>(15, TimeUnit.MINUTES).build()
      WorkManager.getInstance(context!!).enqueue(uploadWorkRequest)
      result.success("")
    } else if (call.method == "stop_location_monitoring") {
      BackgroundWorker.removeLocationUpdates()
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}

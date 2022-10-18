
import 'dart:async';
import 'dart:convert';
import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter/material.dart';
import './location_authorization_status.dart';

typedef BackgroundTaskHandler = Future<bool> Function(Location location, String inputData);

class BackgroundLocationHandler {
  static const pkg = "com.tkrynski.background_location_handler";
  factory BackgroundLocationHandler() => _instance;

  BackgroundLocationHandler._internal(
      MethodChannel backgroundChannel, MethodChannel foregroundChannel)
      : _backgroundChannel = backgroundChannel,
        _foregroundChannel = foregroundChannel;

  static final BackgroundLocationHandler _instance = BackgroundLocationHandler._internal(
      const MethodChannel(
          "$pkg/background_channel_work_manager"),
      const MethodChannel(
          "$pkg/foreground_channel_work_manager"));

  /// Use this constant inside your callbackDispatcher to identify when an iOS Background Fetch occurred.
  ///
  /// ```
  /// void callbackDispatcher() {
  ///   BackgroundLocationHandler().executeTask((taskName, inputData) {
  ///      switch (taskName) {
  ///        case BackgroundLocationHandler.iOSBackgroundTask:
  ///          stderr.writeln("The iOS background fetch was triggered");
  ///          break;
  ///      }
  ///
  ///      return Future.value(true);
  ///  });
  /// }
  /// ```
  MethodChannel _backgroundChannel = const MethodChannel(
      "$pkg/background_channel_work_manager");
  MethodChannel _foregroundChannel = const MethodChannel(
      "$pkg/foreground_channel_work_manager");

  // this is called on Android only
  void executeTask(final BackgroundTaskHandler backgroundTask) {
    WidgetsFlutterBinding.ensureInitialized();
    _backgroundChannel.setMethodCallHandler((call) async {
      if (call.method == 'location') {
        final extras = call.arguments["extras"];

        // we don't know the location yet -- maybe this will be fixed in a later release
        var defaultLocation = Location(
            latitude: 0.0,
            longitude: 0.0,
            altitude: 0.0,
            accuracy: 0.0,
            bearing: 0.0,
            speed: 0.0,
            time: 0.0,
            isMock: false);

        return backgroundTask(
            defaultLocation,
            extras
        );
      }
    });
    // _backgroundChannel.invokeMethod("backgroundChannelInitialized");
  }

  BackgroundTaskHandler? onLocationUpdate = null;

  Future<void> initialize(callbackDispatcher, String extras) async {
    final callback = PluginUtilities.getCallbackHandle(callbackDispatcher);
    assert(callback != null,
    "The callbackDispatcher needs to be either a static function or a top level function to be accessible as a Flutter entry point.");
    if (callback != null) {
      final int handle = callback.toRawHandle();
      await _foregroundChannel.invokeMethod<void>(
          'initialize',
          {
            "extras": extras,
            "callbackHandle": handle,
          }
      );
    }
  }

  static Future<String?> get platformVersion async {
    final String? version = await _instance._foregroundChannel.invokeMethod('getPlatformVersion');
    return version;
  }

  static startMonitoring(BackgroundTaskHandler locationCallback, Function(Visit)? visitCallback, Function(String)? statusCallback, String extraData) async {
    // add a handler on the channel to receive updates from the native classes
    _instance._foregroundChannel.setMethodCallHandler((MethodCall methodCall) async {
      print("MethodCall: ${methodCall.method}");
      if ((methodCall.method == 'location') && (locationCallback != null)) {
        var locationData = Map.from(methodCall.arguments);
        locationCallback(
            Location(
                latitude: locationData['latitude'],
                longitude: locationData['longitude'],
                altitude: locationData['altitude'],
                accuracy: locationData['accuracy'],
                bearing: locationData['bearing'],
                speed: locationData['speed'],
                time: locationData['time'],
                isMock: locationData['is_mock']),
            extraData
        );
      }
      if ((methodCall.method == 'visit') && (visitCallback != null)) {
        var visitData = Map.from(methodCall.arguments);
        visitCallback(
            Visit(
                latitude: visitData['latitude'],
                longitude: visitData['longitude'],
                accuracy: visitData['accuracy'],
                arrivalTime: visitData['arrivalTime'],
                departureTime: visitData['departureTime'],
                isMock: visitData['is_mock'])
        );
      }
      if ((methodCall.method == 'status') && (statusCallback != null)) {
        statusCallback(methodCall.arguments);
      }
    });

    // start monitoring
    if (locationCallback != null) {
      final result = await _instance._foregroundChannel.invokeMethod('start_location_monitoring');
      print("start_location_monitoring result $result");
    }
    if (visitCallback != null) {
      final result = await _instance._foregroundChannel.invokeMethod('start_visit_monitoring');
      print("start_visit_monitoring result $result");
    }
  }

  static stopMonitoring() async {
    // don't await
    _instance._foregroundChannel.invokeMethod('stop_visit_monitoring');
    _instance._foregroundChannel.invokeMethod('stop_location_monitoring');
    return;
  }

  static Future<String> getAuthorizationStatusString() async {
    dynamic status = await _instance._foregroundChannel.invokeMethod('get_authorization_status');
    return status.toString();
  }

  static Future<LocationAuthorizationStatus> getAuthorizationStatus() async {
    dynamic status = await _instance._foregroundChannel.invokeMethod('get_authorization_status');
    switch (status) {
      case "notDetermined":
        return LocationAuthorizationStatus.notDetermined;
      case "authorizedWhenInUse":
        return LocationAuthorizationStatus.whenInUse;
      case "authorizedAlways":
        return LocationAuthorizationStatus.always;
      case "restricted":
        return LocationAuthorizationStatus.restricted;
      case "limited":
        return LocationAuthorizationStatus.limited;
      case "unknown":
        return LocationAuthorizationStatus.unknown;
    }
    return LocationAuthorizationStatus.unknown;
  }

  static Future<String> getSettingsUrl() async {
    dynamic url = await _instance._foregroundChannel.invokeMethod('get_settings_url');
    return url.toString();
  }
}

class Location {
  double? latitude;
  double? longitude;
  double? altitude;
  double? bearing;
  double? accuracy;
  double? speed;
  double? time;
  bool? isMock;

  Location(
      {@required this.longitude,
        @required this.latitude,
        @required this.altitude,
        @required this.accuracy,
        @required this.bearing,
        @required this.speed,
        @required this.time,
        @required this.isMock});

  toMap() {
    var obj = {
      'latitude': latitude,
      'longitude': longitude,
      'altitude': altitude,
      'bearing': bearing,
      'accuracy': accuracy,
      'speed': speed,
      'time': time,
      'is_mock': isMock
    };
    return obj;
  }
}

class Visit {
  double? latitude;
  double? longitude;
  double? accuracy;
  double? arrivalTime;
  double? departureTime;
  bool? isMock;

  Visit(
      {@required this.longitude,
        @required this.latitude,
        @required this.accuracy,
        @required this.arrivalTime,
        @required this.departureTime,
        @required this.isMock});

  toMap() {
    var obj = {
      'latitude': latitude,
      'longitude': longitude,
      'accuracy': accuracy,
      'arrivalTime': arrivalTime,
      'departureTime': departureTime,
      'is_mock': isMock
    };
    return obj;
  }
}



import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter/material.dart';

class BackgroundLocationHandler {
  static const MethodChannel _channel = MethodChannel('background_location_handler');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static startMonitoring(Function(Location)? locationCallback, Function(Visit)? visitCallback, Function(String)? statusCallback) async {
    // add a handler on the channel to receive updates from the native classes
    _channel.setMethodCallHandler((MethodCall methodCall) async {
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
              isMock: visitData['is_mock']),
        );
      }
      if ((methodCall.method == 'status') && (statusCallback != null)) {
        statusCallback(methodCall.arguments);
      }
    });


    // start monitoring
    if (locationCallback != null) {
      await _channel.invokeMethod('start_location_monitoring');
    }
    if (visitCallback != null) {
      await _channel.invokeMethod('start_visit_monitoring');
    }
  }
  static stopMonitoring() async {
    // don't await
    _channel.invokeMethod('stop_visit_monitoring');
    _channel.invokeMethod('stop_location_monitoring');
    return;
  }

  static Future<String> getAuthorizationStatus() async {
    dynamic status = await _channel.invokeMethod('get_authorization_status');
    return status.toString();
  }
  static Future<String> getSettingsUrl() async {
    dynamic url = await _channel.invokeMethod('get_settings_url');
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

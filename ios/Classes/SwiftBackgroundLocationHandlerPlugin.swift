import Flutter
import UIKit
import CoreLocation

public class SwiftBackgroundLocationHandlerPlugin: NSObject, FlutterPlugin, CLLocationManagerDelegate {
  static var locationManager: CLLocationManager?
  static var channel: FlutterMethodChannel?

  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "com.tkrynski.background_location_handler/foreground_channel_work_manager", binaryMessenger: registrar.messenger())
    let instance = SwiftBackgroundLocationHandlerPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
    SwiftBackgroundLocationHandlerPlugin.channel = channel

    // not sure if this is needed
    channel.setMethodCallHandler(instance.handle)
  }

  private func authorize(_ locationManager: CLLocationManager) {
    locationManager.requestAlwaysAuthorization()
  }

  private func authorizationStatusToString(_ authorizationStatus: CLAuthorizationStatus) -> String {
    switch authorizationStatus {
    case .notDetermined:
        return "notDetermined"
    case .authorizedAlways:
        return "authorizedAlways"
    case .authorizedWhenInUse:
        return "authorizedWhenInUse"
    case .restricted:
        return "restricted"
    case .denied:
        return "denied"
    @unknown default:
        return "unknown"
    }
  }

  private func getAuthorizationStatus(_ locationManager: CLLocationManager) -> String {
    let authorizationStatus: CLAuthorizationStatus
    if #available(iOS 14.0, *) {
      authorizationStatus = locationManager.authorizationStatus
    } else {
      authorizationStatus = CLLocationManager.authorizationStatus()
    }
    return authorizationStatusToString(authorizationStatus)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    let locationManager = CLLocationManager()
    SwiftBackgroundLocationHandlerPlugin.locationManager = locationManager

    locationManager.delegate = self
    locationManager.allowsBackgroundLocationUpdates = true
    locationManager.pausesLocationUpdatesAutomatically = false
    locationManager.activityType = CLActivityType.other
    if #available(iOS 11.0, *) {
      locationManager.showsBackgroundLocationIndicator = true
    }

    if (call.method == "start_visit_monitoring") {
      locationManager.requestAlwaysAuthorization()
      self.authorize(locationManager)
      locationManager.startMonitoringVisits()
      result(true)
    } else if (call.method == "start_location_monitoring") {
      if (CLLocationManager.significantLocationChangeMonitoringAvailable()) {
        locationManager.requestAlwaysAuthorization()
        self.authorize(locationManager)
        locationManager.startMonitoringSignificantLocationChanges()
        result(true)
      } else {
        result(false)
      }
    } else if (call.method == "stop_visit_monitoring") {
      locationManager.stopMonitoringVisits()
      result(true)
    } else if (call.method == "stop_location_monitoring") {
      locationManager.stopMonitoringSignificantLocationChanges()
      result(true)
    } else if (call.method == "get_authorization_status") {
      result(self.getAuthorizationStatus(locationManager))
    } else if (call.method == "get_settings_url") {
      result(UIApplication.openSettingsURLString)
    } else {
      result(true)
    }
  }

  // Handle authorizationStatus changes that happen outside of the app
  public func locationManager(_ manager: CLLocationManager,
                       didChangeAuthorization authorizationStatus: CLAuthorizationStatus) {
      let status = authorizationStatusToString(authorizationStatus)
      SwiftBackgroundLocationHandlerPlugin.channel?.invokeMethod("status", arguments: status)
  }

  public func locationManager(_ manager: CLLocationManager, didVisit visit: CLVisit) {
    let visit = [
        "latitude": visit.coordinate.latitude,
        "longitude": visit.coordinate.longitude,
        "accuracy": visit.horizontalAccuracy,
        "arrivalTime": visit.arrivalDate.timeIntervalSince1970 * 1000,
        "departureTime": visit.departureDate.timeIntervalSince1970 * 1000,
        "is_mock": false
    ] as [String : Any]

    SwiftBackgroundLocationHandlerPlugin.channel?.invokeMethod("visit", arguments: visit)
  }
  public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
    if let lastLocation = locations.last {
      let location = [
          "speed": lastLocation.speed,
          "altitude": lastLocation.altitude,
          "latitude": lastLocation.coordinate.latitude,
          "longitude": lastLocation.coordinate.longitude,
          "accuracy": lastLocation.horizontalAccuracy,
          "bearing": lastLocation.course,
          "time": lastLocation.timestamp.timeIntervalSince1970 * 1000,
          "is_mock": false
      ] as [String : Any]

      SwiftBackgroundLocationHandlerPlugin.channel?.invokeMethod("location", arguments: location)
    }
  }
}

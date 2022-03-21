
/// Defines the state of authorization for location access
enum LocationAuthorizationStatus {
  /// The user has not yet made their choice
  notDetermined,

  /// The user denied access to the location
  denied,

  /// The user granted access to the location when in use
  whenInUse,

  /// The user granted access to the location always (in the background)
  always,

  /// The OS denied access to the requested feature. The user cannot change
  /// this app's status, possibly due to active restrictions such as parental
  /// controls being in place.
  /// *Only supported on iOS.*
  restricted,

  ///User has authorized this application for limited access.
  /// *Only supported on iOS (iOS14+).*
  limited,

  /// Permission to the requested feature is permanently denied, the permission
  /// dialog will not be shown when requesting this permission. The user may
  /// still change the permission status in the settings.
  /// *Only supported on Android.*
  permanentlyDenied,

  // something went wrong
  unknown,
}

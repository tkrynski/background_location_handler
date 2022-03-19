#import "BackgroundLocationHandlerPlugin.h"
#if __has_include(<background_location_handler/background_location_handler-Swift.h>)
#import <background_location_handler/background_location_handler-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "background_location_handler-Swift.h"
#endif

@implementation BackgroundLocationHandlerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftBackgroundLocationHandlerPlugin registerWithRegistrar:registrar];
}
@end

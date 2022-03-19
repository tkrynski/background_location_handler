import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:background_location_handler/background_location_handler.dart';

void main() {
  const MethodChannel channel = MethodChannel('background_location_handler');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await BackgroundLocationHandler.platformVersion, '42');
  });
}

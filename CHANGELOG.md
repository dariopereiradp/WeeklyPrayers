# Changelog

All notable changes to this project will be documented in this file.

## [v1.0] - 07/03/2021
First final version

### Known bugs:
- Security problem: if I receive a notification with the app open but do not open the notification; if then I close the app and open the notification, it won't go to the login page, but it will open the content without requiring password... This problem is caused because I create the notification intent in the moment that I receive the notification. I check if the app is open or not. If it is open, it doesn't need login. I didn't find a solution for that. Given the nature of the app, this is not so important, so maybe if I have some extra time I will search more and realese a future update to fix this security issue.
- On Android 8, 9 and maybe 10, it's not possible to authenticate with device pin/pattern. When I click this option, Android opens a new activity to enter the pin and closes my login activity, causing my app to close. When I enter the correct authentication or click on cancel, it goes to the Home Launcher of Android. However, it works on Android 11. On other versions, it still works using biometrics. I didn't test on Android 10. Note: With devices with no biometric authentication, it's possible to enter without any security. This is not a bug, but it's a decision. Maybe later, if I have some time, I will develop an authentication system for the app, independent of the device/system.


[v1.0]: https://github.com/dariopereiradp/WeeklyPrayers/releases/tag/v1.0

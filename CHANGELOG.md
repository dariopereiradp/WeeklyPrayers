# Changelog

All notable changes to this project will be documented in this file.

## v1.0.0.2 - 05/04/2021

### Improvements
- Back button closes search view if it is open (new method closeSearchView() on MainActivity)



## v1.0.0.1 - 04/04/2021

### Fixed
- Bug on navigation item uncheck in some cases. A separated function called uncheckNavigationItems(int index) was created and is called not only on backPressed but also onNavigationItemSelected.

### Added
- Information about ECB encryption mode on HTML files and more information about data protection



## [v1.0] - 03/03/2021
First final version

### Fixed
- Instagram link bug fixed
- Deprecation solved in Splash Screen
- FileNotFoundException error on Glide load picture

### Improvements
- Source code is commented with Javadoc
- Descendant order on Diary (new notes appear on top)
-  Better app colors and font size
- Improvements on share
- Improvements on Google Login
- Better class names
- Migration to LocalDateTime instead of Calendar in many classes/methods
- Removed some unnecessary method

### New
- Customize colors in Prayer Cards and Diary
- Search feature for Diary
- Share on Prayer Cards


## [v1.0-beta1] - 31/01/2021
Initial beta version
Known bugs:
- Security problem: if I receive a notification with the app open but do not open the notification; if then I close the app and open the notification, it won't go to the login page, but it will open the content without requiring password... This problem is caused because I create the notification intent in the moment that I receive the notification. I check if the app is open or not. If it is open, it doesn't need login. I didn't find a solution for that. Given the nature of the app, this is not so important, so maybe if I have some extra time I will search more and realese a future update to fix this security issue.
- On Android 8, 9 and maybe 10, it's not possible to authenticate with device pin/pattern. When I click this option, Android opens a new activity to enter the pin and closes my login activity, causing my app to close. When I enter the correct authentication or click on cancel, it goes to the Home Launcher of Android. However, it works on Android 11. On other versions, it still works using biometrics. I didn't test on Android 10. Note: With devices with no biometric authentication, it's possible to enter without any security. This is not a bug, but it's a decision. Maybe later, if I have some time, I will develop an authentication system for the app, independent of the device/system.


[v1.0]: https://github.com/dariopereiradp/PrayForHer/compare/v1.0-beta.1...v1.0
[v1.0-beta1]: https://github.com/dariopereiradp/PrayForHer/releases/tag/v1.0-beta.1

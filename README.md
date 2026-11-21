# AppLocker 🔐

A native Android application locker built with **Kotlin**. AppLocker helps protect selected apps using biometric authentication, PIN, password, or pattern-based authentication.

## ✨ Features

* **App selection:** Choose which installed applications you want to protect.
* **Multiple authentication methods:**
  * Fingerprint / biometric authentication (where supported)
  * Numeric PIN
  * Password
  * Pattern lock
* **Automatic relocking:** Configure how long an app can remain unlocked after leaving it.
* **Native authentication screen:** A dedicated Android Activity for authentication.
* **Accessibility-based app detection:** Detects when a protected app is opened or left.
* **Screen-lock handling:** Supports relocking when the device screen turns off.
* **Local operation:** Designed to work without a cloud account.

## 🛠️ Built With

| Technology | Purpose |
|----|----|
| Kotlin | Main programming language |
| Android SDK | Native Android development |
| Gradle | Build automation |
| Android AccessibilityService | Foreground app detection |
| AndroidX Biometric | Biometric authentication |
| AndroidX AppCompat | Android compatibility |

## 📱 Compatibility

* Minimum Android SDK: 23 (Android 6.0)
* Compile SDK: 35
* Target SDK: 35

## 🚀 Building the Project

### Prerequisites

* JDK 17
* Android SDK
* Android SDK Platform 35
* Android Build Tools 35.0.0
* Gradle 8.7

### Build

Clone the repository:

```bash
git clone https://github.com/shivam-tiwari-dev/AppLocker.git
```

Navigate to the project directory:

```bash
cd AppLocker
```

Build the debug APK:

```bash
gradle assembleDebug
```

The generated APK will be available at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected Android device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## ⚙️ Initial Setup


1. Install the generated APK.
2. Open AppLocker.
3. Configure your preferred authentication method.
4. Select the applications you want to protect.
5. Enable the AppLock accessibility service when prompted.
6. Configure your automatic relock preference.

## 🔒 Privacy and Permissions

AppLocker uses Android's AccessibilityService to detect application transitions and determine when a protected app needs authentication.

The accessibility service is configured with `canRetrieveWindowContent=false`, so it does not retrieve the contents of other apps' windows.

Accessibility permission must be explicitly enabled by the device user.

## 📸 Screenshots

Screenshots of the application interface will be added here.

## ⚠️ Limitations

* AppLocker is a personal, sideloaded Android project.
* Its protection depends on the accessibility service remaining enabled.
* It cannot guarantee protection if the app is force-stopped, uninstalled, or its required permissions are disabled.
* Biometric authentication depends on device hardware and Android support.

## 🛣️ Future Improvements

* Improve foreground application detection.
* Further refine automatic relocking.
* Improve the user interface and settings experience.
* Add additional privacy and security controls.

## 👨‍💻 Author

**Shivam Tiwari**

GitHub: [@shivam-tiwari-dev](https://github.com/shivam-tiwari-dev)


---

*Built as a hands-on Android development project while learning Kotlin and native application development.*
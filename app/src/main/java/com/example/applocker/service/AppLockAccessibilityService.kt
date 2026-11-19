
package com.example.applocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

class AppLockAccessibilityService : AccessibilityService() {

    private val preferences by lazy {
        getSharedPreferences("app_locker_settings", Context.MODE_PRIVATE)
    }

    private lateinit var overlayManager: LockOverlayManager
    private lateinit var relockController: RelockController

    private var unlockedPackage: String? = null
    private var currentPackage: String? = null

    private var screenReceiver: ScreenStateReceiver? = null
    private var biometricReceiver: BroadcastReceiver? = null
    private var quickLockReceiver: BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
android.util.Log.d("AppLockerDebug", "ACCESSIBILITY SERVICE CONNECTED")
        overlayManager = LockOverlayManager(this)

        relockController = RelockController {
            unlockedPackage = null
        }

        // Screen state receiver
        screenReceiver = ScreenStateReceiver(
            onScreenOff = {
                unlockedPackage = null
                relockController.cancel()
                overlayManager.hide()
            },
            onScreenOn = {
                val lockedApps = preferences.getStringSet(
                    "locked_apps",
                    emptySet()
                ) ?: emptySet()

                val packageName = currentPackage

                if (packageName != null && packageName in lockedApps) {
                    overlayManager.show {
                        unlockedPackage = packageName
                    }
                }
            }
        )

        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Biometric success receiver
        biometricReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (intent?.action ==
                    "com.example.applocker.BIOMETRIC_SUCCESS"
                ) {
                    overlayManager.biometricSucceeded()
                }
            }
        }

        ContextCompat.registerReceiver(
            this,
            biometricReceiver,
            IntentFilter("com.example.applocker.BIOMETRIC_SUCCESS"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Quick Lock receiver
        quickLockReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                unlockedPackage = null
                relockController.cancel()
                overlayManager.hide()
            }
        }

        ContextCompat.registerReceiver(
            this,
            quickLockReceiver,
            IntentFilter("com.example.applocker.QUICK_LOCK"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (event == null) return
android.util.Log.d(
    "AppLockerDebug",
    "Event=${event.eventType}, package=${event.packageName}"
)
        if (event.eventType !=
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val packageName = event.packageName?.toString() ?: return

        if (packageName == this.packageName) return

        val previousPackage = currentPackage
        currentPackage = packageName

        val lockedApps = preferences.getStringSet(
            "locked_apps",
            emptySet()
        ) ?: emptySet()

        android.util.Log.d(
    "AppLockerDebug",
    "PACKAGE=$packageName LOCKED=${packageName in lockedApps} UNLOCKED=$unlockedPackage"
)

        // Moved to an unprotected app
        if (packageName !in lockedApps) {

            if (previousPackage in lockedApps) {

                if (unlockedPackage == previousPackage) {

                    val timeout = preferences.getInt(
                        "auto_lock_timeout_seconds",
                        0
                    )

                    relockController.start(timeout)

                } else {
                    relockController.cancel()
                    overlayManager.hide()
                }
            }

            return
        }

        // Already authenticated
        if (unlockedPackage == packageName) {
            relockController.cancel()
            return
        }

        // Protected app requires authentication
        relockController.cancel()
        unlockedPackage = null
        overlayManager.hide()

      android.util.Log.d("AppLockerDebug", "SHOWING LOCK OVERLAY FOR $packageName")

overlayManager.show {
    android.util.Log.d("AppLockerDebug", "UNLOCK CALLBACK FOR $packageName")
}
    unlockedPackage = packageName

    }

    override fun onInterrupt() {
        if (::overlayManager.isInitialized) {
            overlayManager.hide()
        }
    }

    override fun onDestroy() {

        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {}
        }

        biometricReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {}
        }

        quickLockReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {}
        }

        if (::overlayManager.isInitialized) {
            overlayManager.hide()
        }

        if (::relockController.isInitialized) {
            relockController.cancel()
        }

        super.onDestroy()
    }
}

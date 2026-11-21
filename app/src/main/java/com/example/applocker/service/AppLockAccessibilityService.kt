
package com.example.applocker.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.example.applocker.auth.UnlockActivity

class AppLockAccessibilityService : AccessibilityService() {

    private val preferences by lazy {
        getSharedPreferences("app_locker_settings", Context.MODE_PRIVATE)
    }

    private lateinit var relockController: RelockController

    private var unlockedPackage: String? = null
    private var currentPackage: String? = null
    private var pendingAuthPackage: String? = null

    private var screenReceiver: ScreenStateReceiver? = null
    private var authReceiver: BroadcastReceiver? = null
    private var quickLockReceiver: BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        android.util.Log.d("AppLockerDebug", "ACCESSIBILITY SERVICE CONNECTED")

        relockController = RelockController {
            android.util.Log.d(
                "AppLockerDebug",
                "RELOCK TIMER EXPIRED"
            )
            unlockedPackage = null
        }

        screenReceiver = ScreenStateReceiver(
            onScreenOff = {
                unlockedPackage = null
                relockController.cancel()
            },
            onScreenOn = {
                val lockedApps = preferences.getStringSet(
                    "locked_apps",
                    emptySet()
                ) ?: emptySet()

                val packageName = currentPackage

                if (
                    packageName != null &&
                    packageName in lockedApps &&
                    unlockedPackage != packageName &&
                    pendingAuthPackage == null
                ) {
                    launchUnlockActivity(packageName)
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

        authReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                val target = intent?.getStringExtra(
                    UnlockActivity.EXTRA_TARGET_PACKAGE
                )

                when (intent?.action) {
                    UnlockActivity.ACTION_UNLOCK_SUCCESS -> {
                        if (target != null && target == pendingAuthPackage) {
                            unlockedPackage = target
                            pendingAuthPackage = null
                            relockController.cancel()

                            android.util.Log.d(
                                "AppLockerDebug",
                                "AUTHENTICATION SUCCESS: $target"
                            )
                        }
                    }

                    UnlockActivity.ACTION_UNLOCK_CANCELLED -> {
                        if (target == pendingAuthPackage) {
                            pendingAuthPackage = null

                            android.util.Log.d(
                                "AppLockerDebug",
                                "AUTHENTICATION CANCELLED: $target"
                            )
                        }
                    }
                }
            }
        }

        ContextCompat.registerReceiver(
            this,
            authReceiver,
            IntentFilter().apply {
                addAction(UnlockActivity.ACTION_UNLOCK_SUCCESS)
                addAction(UnlockActivity.ACTION_UNLOCK_CANCELLED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        quickLockReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                unlockedPackage = null
                relockController.cancel()
            }
        }

        ContextCompat.registerReceiver(
            this,
            quickLockReceiver,
            IntentFilter("com.example.applocker.QUICK_LOCK"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun launchUnlockActivity(packageName: String) {
        if (pendingAuthPackage == packageName) return

        pendingAuthPackage = packageName

        try {
            val intent = Intent(
                this,
                UnlockActivity::class.java
            ).apply {
                putExtra(
                    UnlockActivity.EXTRA_TARGET_PACKAGE,
                    packageName
                )

                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }

            startActivity(intent)

            android.util.Log.d(
                "AppLockerDebug",
                "LAUNCHING AUTHENTICATION FOR: $packageName"
            )

        } catch (e: Exception) {
            pendingAuthPackage = null

            android.util.Log.e(
                "AppLockerDebug",
                "Unable to launch authentication Activity",
                e
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (
            event.eventType !=
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val packageName = event.packageName?.toString() ?: return

        // Ignore App Locker's own Activities.
        if (packageName == this.packageName) return

        // Ignore temporary Android System UI events.
        // Do NOT change currentPackage for these events.
        if (packageName == "com.android.systemui") {
            android.util.Log.d(
                "AppLockerDebug",
                "IGNORING TEMPORARY SYSTEM UI EVENT"
            )
            return
        }

        val previousPackage = currentPackage
        currentPackage = packageName

        val lockedApps = preferences.getStringSet(
            "locked_apps",
            emptySet()
        ) ?: emptySet()

        android.util.Log.d(
            "AppLockerDebug",
            "PACKAGE=$packageName LOCKED=${packageName in lockedApps} UNLOCKED=$unlockedPackage PENDING=$pendingAuthPackage"
        )

        // User genuinely moved to an unprotected app.
        if (packageName !in lockedApps) {

            if (previousPackage in lockedApps) {

                if (unlockedPackage == previousPackage) {
                    val timeout = preferences.getInt(
                        "auto_lock_timeout_seconds",
                        30
                    )

                    android.util.Log.d(
                        "AppLockerDebug",
                        "STARTING RELOCK TIMER: $timeout seconds"
                    )

                    relockController.start(timeout)
                } else {
                    relockController.cancel()
                }
            }

            return
        }

        // Protected app is already authenticated.
        if (unlockedPackage == packageName) {
            relockController.cancel()
            return
        }

        // Authentication Activity is already displayed.
        if (pendingAuthPackage == packageName) {
            return
        }

        relockController.cancel()
        unlockedPackage = null

        launchUnlockActivity(packageName)
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {

        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }

        authReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }

        quickLockReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }

        if (::relockController.isInitialized) {
            relockController.cancel()
        }

        super.onDestroy()
    }
}


package com.example.applocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.example.applocker.auth.AuthenticationManager
import com.example.applocker.auth.BiometricUnlockActivity
import com.example.applocker.auth.PatternLockView

class LockOverlayManager(
    private val service: AccessibilityService
) {

    private val windowManager =
        service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val authManager = AuthenticationManager(service)
    private val handler = Handler(Looper.getMainLooper())

    private var overlayView: LinearLayout? = null
    private var unlockCallback: (() -> Unit)? = null
    private var cooldownRunnable: Runnable? = null

    fun show(onUnlocked: () -> Unit) {

        if (overlayView != null) {
            unlockCallback = onUnlocked
            return
        }

        unlockCallback = onUnlocked

        val root = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(35, 35, 35, 35)
            setBackgroundColor(Color.rgb(244, 245, 250))
        }

        val title = TextView(service).apply {
            text = "APP LOCKER"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(32, 36, 58))
        }
        root.addView(title)

        val message = TextView(service).apply {
            text = "Authenticate to open this app"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(0, 20, 0, 20)
        }
        root.addView(message)

        val input = EditText(service).apply {
            hint = "Enter PIN or password"
            gravity = Gravity.CENTER
        }

        val patternView = PatternLockView(service)

        val status = TextView(service).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.RED)
            setPadding(0, 15, 0, 15)
        }

        val unlock = Button(service).apply {
            text = "UNLOCK"
        }

        root.addView(input)

        root.addView(
            patternView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                350
            )
        )

        root.addView(status)
        root.addView(unlock)

        fun completeUnlock() {
            authManager.resetFailedAttempts()
            val callback = unlockCallback
            hide()
            unlockCallback = null
            callback?.invoke()
        }

        fun updateCooldown() {
            cooldownRunnable?.let {
                handler.removeCallbacks(it)
            }
            cooldownRunnable = null

            val remaining = authManager.getCooldownRemainingSeconds()

            if (remaining > 0L) {
                status.text =
                    "Too many attempts. Try again in ${remaining}s."

                patternView.isEnabled = false
                input.isEnabled = false
                unlock.isEnabled = false

                cooldownRunnable = Runnable {
                    updateCooldown()
                }

                handler.postDelayed(cooldownRunnable!!, 1000L)

            } else {
                patternView.isEnabled = true
                input.isEnabled = true
                unlock.isEnabled = true
                status.text = "You can try again now."
            }
        }

        val authType = authManager.getPreferredMethod()

        when (authType) {

            AuthenticationManager.AuthType.PIN -> {

                patternView.visibility = View.GONE

                input.inputType =
                    InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD

                unlock.setOnClickListener {

                    if (authManager.getCooldownRemainingSeconds() > 0L) {
                        updateCooldown()
                        return@setOnClickListener
                    }

                    if (authManager.verifyPin(input.text.toString())) {
                        completeUnlock()
                    } else {
                        val remaining = authManager.recordFailedAttempt()
                        input.text.clear()

                        if (remaining > 0L) {
                            updateCooldown()
                        } else {
                            status.text = "Incorrect PIN. Try again."
                        }
                    }
                }
            }

            AuthenticationManager.AuthType.PASSWORD -> {

                patternView.visibility = View.GONE

                input.inputType =
                    InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD

                unlock.setOnClickListener {

                    if (authManager.getCooldownRemainingSeconds() > 0L) {
                        updateCooldown()
                        return@setOnClickListener
                    }

                    if (authManager.verifyPassword(input.text.toString())) {
                        completeUnlock()
                    } else {
                        val remaining = authManager.recordFailedAttempt()
                        input.text.clear()

                        if (remaining > 0L) {
                            updateCooldown()
                        } else {
                            status.text = "Incorrect password. Try again."
                        }
                    }
                }
            }

            AuthenticationManager.AuthType.PATTERN -> {

                input.visibility = View.GONE
                unlock.visibility = View.GONE

                patternView.onPatternComplete = { pattern ->

                    if (authManager.getCooldownRemainingSeconds() > 0L) {
                        patternView.clearPattern()
                        updateCooldown()

                    } else if (authManager.verifyPattern(pattern)) {
                        completeUnlock()

                    } else {
                        val remaining = authManager.recordFailedAttempt()
                        patternView.clearPattern()

                        if (remaining > 0L) {
                            updateCooldown()
                        } else {
                            status.text = "Incorrect pattern. Try again."
                        }
                    }
                }
            }

AuthenticationManager.AuthType.BIOMETRIC -> {

    input.visibility = View.GONE
    patternView.visibility = View.GONE

    unlock.text = "USE BIOMETRICS"

    unlock.setOnClickListener {

        status.text = "Starting biometric authentication..."

        try {
            val intent = Intent(
                service,
                BiometricUnlockActivity::class.java
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }

            service.startActivity(intent)

        } catch (e: Exception) {
            android.util.Log.e(
                "AppLockerDebug",
                "Biometric launch failed",
                e
            )

            status.text =
                "Unable to open biometrics. Try again."
        }
    }
}

        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(root, params)
            overlayView = root
            updateCooldown()

        } catch (_: Exception) {
            overlayView = null
            unlockCallback = null
        }
    }

    fun biometricSucceeded() {
        if (overlayView != null) {
            authManager.resetFailedAttempts()

            val callback = unlockCallback
            hide()
            unlockCallback = null
            callback?.invoke()
        }
    }

    fun hide() {
        cooldownRunnable?.let {
            handler.removeCallbacks(it)
        }
        cooldownRunnable = null

        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }

        overlayView = null
    }
}

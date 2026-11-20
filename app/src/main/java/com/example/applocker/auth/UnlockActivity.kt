
package com.example.applocker.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class UnlockActivity : FragmentActivity() {

    companion object {
        const val ACTION_UNLOCK_SUCCESS =
            "com.example.applocker.UNLOCK_SUCCESS"

        const val ACTION_UNLOCK_CANCELLED =
            "com.example.applocker.UNLOCK_CANCELLED"

        const val EXTRA_TARGET_PACKAGE = "target_package"
    }

    private lateinit var manager: AuthenticationManager
    private lateinit var secretInput: EditText
    private lateinit var status: TextView
    private lateinit var unlockButton: Button
    private lateinit var retryBiometricButton: Button

    private var patternView: PatternLockView? = null
    private var targetPackage: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private var cooldownRunnable: Runnable? = null

    private var authType = AuthenticationManager.AuthType.PIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager = AuthenticationManager(this)

        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)

        if (targetPackage.isNullOrBlank()) {
            finish()
            return
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    cancelAndReturnHome()
                }
            }
        )

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(35, 35, 35, 35)
            setBackgroundColor(Color.rgb(244, 245, 250))
        }

        val title = TextView(this).apply {
            text = "APP LOCKER"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(32, 36, 58))
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Authenticate to open this app"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(0, 20, 0, 20)
        }
        root.addView(subtitle)

        secretInput = EditText(this).apply {
            hint = "Enter PIN or password"
            textSize = 18f
            gravity = Gravity.CENTER
        }
        root.addView(
            secretInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        status = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.RED)
            setPadding(0, 15, 0, 15)
        }
        root.addView(status)

        unlockButton = Button(this).apply {
            text = "UNLOCK"
            setOnClickListener {
                verifyCredentials()
            }
        }
        root.addView(unlockButton)

        retryBiometricButton = Button(this).apply {
            text = "TRY BIOMETRICS AGAIN"
            visibility = View.GONE
            setOnClickListener {
                startBiometricPrompt()
            }
        }
        root.addView(retryBiometricButton)

        authType = manager.getPreferredMethod()

        when (authType) {
            AuthenticationManager.AuthType.PIN -> {
                secretInput.inputType =
                    InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD
            }

            AuthenticationManager.AuthType.PASSWORD -> {
                secretInput.inputType =
                    InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            AuthenticationManager.AuthType.PATTERN -> {
                secretInput.visibility = View.GONE
                unlockButton.visibility = View.GONE

                val pattern = PatternLockView(this)
                patternView = pattern

                root.addView(
                    pattern,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        350
                    )
                )

                pattern.onPatternComplete = { value ->
                    verifyPattern(value)
                }
            }

            AuthenticationManager.AuthType.BIOMETRIC -> {
                secretInput.visibility = View.GONE
                unlockButton.visibility = View.GONE
            }
        }

        setContentView(root)

        updateCooldown()

        if (authType == AuthenticationManager.AuthType.BIOMETRIC) {
            handler.postDelayed({
                if (!isFinishing && !isDestroyed) {
                    startBiometricPrompt()
                }
            }, 300)
        } else if (
            authType == AuthenticationManager.AuthType.PIN ||
            authType == AuthenticationManager.AuthType.PASSWORD
        ) {
            if (manager.getCooldownRemainingSeconds() == 0L) {
                handler.postDelayed({
                    if (!isFinishing && !isDestroyed) {
                        secretInput.requestFocus()

                        val imm = getSystemService(
                            Context.INPUT_METHOD_SERVICE
                        ) as InputMethodManager

                        imm.showSoftInput(
                            secretInput,
                            InputMethodManager.SHOW_IMPLICIT
                        )
                    }
                }, 250)
            }
        }
    }

    private fun verifyCredentials() {
        if (manager.getCooldownRemainingSeconds() > 0L) {
            updateCooldown()
            return
        }

        val secret = secretInput.text.toString()

        val valid = when (authType) {
            AuthenticationManager.AuthType.PIN ->
                manager.verifyPin(secret)

            AuthenticationManager.AuthType.PASSWORD ->
                manager.verifyPassword(secret)

            else -> false
        }

        if (valid) {
            onUnlockSuccess()
        } else {
            secretInput.text.clear()

            val remaining = manager.recordFailedAttempt()

            if (remaining > 0L) {
                updateCooldown()
            } else {
                status.text = "Incorrect credentials. Try again."
            }
        }
    }

    private fun verifyPattern(pattern: String) {
        if (manager.getCooldownRemainingSeconds() > 0L) {
            patternView?.clearPattern()
            updateCooldown()
            return
        }

        if (manager.verifyPattern(pattern)) {
            onUnlockSuccess()
        } else {
            patternView?.clearPattern()

            val remaining = manager.recordFailedAttempt()

            if (remaining > 0L) {
                updateCooldown()
            } else {
                status.text = "Incorrect pattern. Try again."
            }
        }
    }

    private fun updateCooldown() {
        cooldownRunnable?.let {
            handler.removeCallbacks(it)
        }
        cooldownRunnable = null

        val remaining = manager.getCooldownRemainingSeconds()

        if (remaining > 0L) {
            status.text =
                "Too many attempts. Try again in ${remaining}s."

            secretInput.isEnabled = false
            unlockButton.isEnabled = false
            patternView?.isEnabled = false

            cooldownRunnable = Runnable {
                updateCooldown()
            }

            handler.postDelayed(cooldownRunnable!!, 1000L)
        } else {
            secretInput.isEnabled = true
            unlockButton.isEnabled = true
            patternView?.isEnabled = true

            if (status.text.toString().startsWith("Too many attempts")) {
                status.text = "You can try again now."
            }
        }
    }

    private fun startBiometricPrompt() {
        if (!manager.isBiometricAvailable()) {
            status.text = "No supported biometric is available."
            retryBiometricButton.visibility = View.VISIBLE
            return
        }

        retryBiometricButton.visibility = View.GONE
        status.text = "Starting biometric authentication..."

        val executor = ContextCompat.getMainExecutor(this)

        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onUnlockSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    status.text = "Not recognized. Try again."
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)

                    status.text = errString
                    retryBiometricButton.visibility = View.VISIBLE
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("App Locker Authentication")
            .setSubtitle("Verify your identity to unlock")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun onUnlockSuccess() {
        manager.resetFailedAttempts()

        val successIntent = Intent(ACTION_UNLOCK_SUCCESS).apply {
            setPackage(packageName)
            putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
        }
        sendBroadcast(successIntent)

        setResult(RESULT_OK)
        finish()
    }

    private fun cancelAndReturnHome() {
        val cancelIntent = Intent(ACTION_UNLOCK_CANCELLED).apply {
            setPackage(packageName)
            putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
        }
        sendBroadcast(cancelIntent)

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(homeIntent)
        finish()
    }

    override fun onDestroy() {
        cooldownRunnable?.let {
            handler.removeCallbacks(it)
        }
        cooldownRunnable = null
        super.onDestroy()
    }
}

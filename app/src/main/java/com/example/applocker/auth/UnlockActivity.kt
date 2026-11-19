
package com.example.applocker.auth

import android.os.Bundle
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class UnlockActivity : FragmentActivity() {

    private lateinit var manager: AuthenticationManager
    private lateinit var secretInput: EditText
    private lateinit var status: TextView

    private val preferences by lazy {
        getSharedPreferences("app_locker_settings", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager = AuthenticationManager(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            setBackgroundColor(Color.rgb(244, 245, 250))
        }

        val title = TextView(this).apply {
            text = "APP LOCKER"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(32, 36, 58))
        }

        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Authenticate to continue"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 25)
            setTextColor(Color.DKGRAY)
        }

        root.addView(subtitle)

        secretInput = EditText(this).apply {
            hint = "Enter your PIN or password"
            textSize = 18f
            gravity = Gravity.CENTER
        }

        root.addView(
            secretInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        status = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
            setTextColor(Color.DKGRAY)
        }

        root.addView(status)

        val unlockButton = Button(this).apply {
            text = "UNLOCK"
            setOnClickListener {
                verifyCredentials()
            }
        }

        root.addView(unlockButton)

        setContentView(root)

        when (manager.getPreferredMethod()) {

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

            AuthenticationManager.AuthType.BIOMETRIC -> {
                secretInput.visibility = android.view.View.GONE
                unlockButton.visibility = android.view.View.GONE
                showBiometricPrompt()
            }

          AuthenticationManager.AuthType.PATTERN -> {
    secretInput.visibility = android.view.View.GONE
    unlockButton.visibility = android.view.View.GONE
    showPatternUnlock()
}
        }
    }

    private fun verifyCredentials() {

        val lockoutUntil = preferences.getLong(
            "lockout_until",
            0L
        )

        if (System.currentTimeMillis() < lockoutUntil) {
            status.text = "Too many attempts. Please wait before trying again."
            return
        }

        val secret = secretInput.text.toString()

        val valid = when (manager.getPreferredMethod()) {
            AuthenticationManager.AuthType.PIN ->
                manager.verifyPin(secret)

            AuthenticationManager.AuthType.PASSWORD ->
                manager.verifyPassword(secret)

            else -> false
        }

        if (valid) {
            onUnlockSuccess()
        } else {
            val attempts = preferences.getInt("failed_attempts", 0) + 1

            val editor = preferences.edit()
                .putInt("failed_attempts", attempts)

            if (attempts >= 5) {
                editor.putLong(
                    "lockout_until",
                    System.currentTimeMillis() + 30_000L
                )
                status.text = "Too many attempts. Try again after 30 seconds."
            } else {
                status.text = "Incorrect credentials. Attempts: $attempts / 5"
            }

            editor.apply()
            secretInput.text.clear()
        }
    }

    private fun showBiometricPrompt() {

        if (!manager.isBiometricAvailable()) {
            status.text = "No eligible biometric is available."
            return
        }

        val executor = ContextCompat.getMainExecutor(this)

        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    onUnlockSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    status.text = "Authentication error: $errString"
                }

                override fun onAuthenticationFailed() {
                    status.text = "Biometric not recognized. Try again."
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("App Locker")
            .setSubtitle("Verify your identity")
            .setNegativeButtonText("Cancel")
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun onUnlockSuccess() {

        preferences.edit()
            .putInt("failed_attempts", 0)
            .putLong("lockout_until", 0L)
            .apply()

        setResult(RESULT_OK)
        finish()
    }

private fun showPatternUnlock() {

    val patternView = PatternLockView(this)

    val params = android.widget.LinearLayout.LayoutParams(
        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
        350
    )

    val parent = findViewById<android.view.ViewGroup>(
        android.R.id.content
    )

    parent.addView(patternView, params)

    patternView.onPatternComplete = { pattern ->

       if (AuthenticationManager(this).verifyPattern(pattern)) {
            onUnlockSuccess()
        } else {
            status.text = "Incorrect pattern. Try again."
            patternView.clearPattern()
        }
    }
}

}


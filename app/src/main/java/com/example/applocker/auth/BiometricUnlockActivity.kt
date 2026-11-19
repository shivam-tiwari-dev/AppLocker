
package com.example.applocker.auth

import android.os.Bundle
import android.content.Intent
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat

class BiometricUnlockActivity : FragmentActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        status = TextView(this).apply {
            text = "Preparing biometric authentication..."
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val layout = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            addView(status)
        }

        setContentView(layout)

        val biometricManager = BiometricManager.from(this)

        if (
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) != BiometricManager.BIOMETRIC_SUCCESS
        ) {
            status.text = "No supported biometric authentication available."
            return
        }

        startBiometricPrompt()
    }

    private fun startBiometricPrompt() {

        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)

                val successIntent = Intent(
                    "com.example.applocker.BIOMETRIC_SUCCESS"
                ).setPackage(packageName)

                sendBroadcast(successIntent)

                setResult(RESULT_OK)
                finish()
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
                setResult(RESULT_CANCELED)
                finish()
            }
        }

        val prompt = BiometricPrompt(
            this,
            executor,
            callback
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("App Locker Authentication")
            .setSubtitle("Verify your identity to unlock")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}

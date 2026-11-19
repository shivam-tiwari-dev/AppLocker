
package com.example.applocker.auth

import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.text.InputType
import android.widget.*
import android.content.Intent
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class AuthSetupActivity : FragmentActivity() {

    private lateinit var manager: AuthenticationManager
    private lateinit var method: Spinner
    private lateinit var secret: EditText
    private lateinit var confirm: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager = AuthenticationManager(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 35, 28, 28)
            setBackgroundColor(Color.rgb(244, 245, 250))
        }

        fun addText(text: String, size: Float) {
            root.addView(TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(Color.rgb(32, 36, 58))
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        addText("SECURITY SETUP", 26f)
        addText("Choose how you want to protect your apps.", 15f)

        method = Spinner(this)
        val methods = listOf("Numeric PIN", "Password", "Pattern", "Biometrics")
        method.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            methods
        )

        root.addView(method)

        secret = EditText(this).apply {
    hint = "Enter your new PIN or password"
    inputType = InputType.TYPE_CLASS_NUMBER or
            InputType.TYPE_NUMBER_VARIATION_PASSWORD
}
        root.addView(secret)

        confirm = EditText(this).apply {
    hint = "Confirm PIN or password"
    inputType = InputType.TYPE_CLASS_NUMBER or
            InputType.TYPE_NUMBER_VARIATION_PASSWORD
}
        root.addView(confirm)

        status = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 15)
        }
        root.addView(status)

        val save = Button(this).apply {
            text = "Save Authentication"
            setOnClickListener { saveAuthentication() }
        }
        root.addView(save)

        method.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    val biometric = position == 3
                    val passwordMode = position == 1

val inputType = if (passwordMode) {
    InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD
} else {
    InputType.TYPE_CLASS_NUMBER or
            InputType.TYPE_NUMBER_VARIATION_PASSWORD
}

secret.inputType = inputType
confirm.inputType = inputType
                    secret.visibility =
                        if (biometric) android.view.View.GONE else android.view.View.VISIBLE
                    confirm.visibility =
                        if (biometric) android.view.View.GONE else android.view.View.VISIBLE
                }
            }

        setContentView(root)
    }

    private fun saveAuthentication() {
        val first = secret.text.toString()
        val second = confirm.text.toString()

        when (method.selectedItemPosition) {
            0 -> {
                if (first != second) {
                    status.text = "PINs do not match."
                    return
                }
                if (!manager.setupPin(first)) {
                    status.text = "PIN must contain 4–8 digits."
                    return
                }
                manager.setPreferredMethod(AuthenticationManager.AuthType.PIN)
            }

            1 -> {
                if (first != second) {
                    status.text = "Passwords do not match."
                    return
                }
                if (!manager.setupPassword(first)) {
                    status.text = "Password must contain 6–64 characters."
                    return
                }
                manager.setPreferredMethod(AuthenticationManager.AuthType.PASSWORD)
            }
2 -> {
    startActivity(
        Intent(this, PatternSetupActivity::class.java)
    )
    return
}
            3 -> {
                if (!manager.isBiometricAvailable()) {
                    status.text = "No eligible biometric authentication is enrolled."
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
                            manager.setPreferredMethod(
                                AuthenticationManager.AuthType.BIOMETRIC
                            )
                            status.text = "Biometric authentication verified."
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {
                            status.text = "Authentication error: $errString"
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("App Locker")
                    .setSubtitle("Verify your device biometric")
                    .setNegativeButtonText("Cancel")
                    .build()

                prompt.authenticate(promptInfo)
                return
            }
        }

        status.text = "Authentication method saved successfully."
    }
}

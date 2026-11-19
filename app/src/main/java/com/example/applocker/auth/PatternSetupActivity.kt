
package com.example.applocker.auth

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class PatternSetupActivity : Activity() {

    private lateinit var patternView: PatternLockView
    private lateinit var message: TextView

    private var firstPattern: String? = null

    private val authManager by lazy {
        AuthenticationManager(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = "Create Pattern Lock"
            textSize = 25f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        message = TextView(this).apply {
            text = "Draw a pattern using at least 4 dots"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
        }

        patternView = PatternLockView(this)

        val clearButton = Button(this).apply {
            text = "CLEAR"
            setOnClickListener {
                patternView.clearPattern()
            }
        }

        layout.addView(title)
        layout.addView(message)

        layout.addView(
            patternView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                350
            )
        )

        layout.addView(clearButton)

        setContentView(layout)

        patternView.onPatternComplete = { pattern ->
            handlePattern(pattern)
        }
    }

    private fun handlePattern(pattern: String) {

        if (pattern.split("-").size < 4) {
            message.text = "Use at least 4 different dots."
            patternView.clearPattern()
            return
        }

        if (firstPattern == null) {
            firstPattern = pattern
            message.text = "Pattern saved temporarily. Draw it again to confirm."
            patternView.clearPattern()
            return
        }

        if (firstPattern != pattern) {
            firstPattern = null
            message.text = "Patterns didn't match. Start again."
            patternView.clearPattern()
            return
        }

        if (authManager.setupPattern(pattern)) {
            authManager.setPreferredMethod(
                AuthenticationManager.AuthType.PATTERN
            )

            Toast.makeText(
                this,
                "Pattern saved successfully!",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        } else {
            message.text = "Invalid pattern. Try again."
            firstPattern = null
            patternView.clearPattern()
        }
    }
}

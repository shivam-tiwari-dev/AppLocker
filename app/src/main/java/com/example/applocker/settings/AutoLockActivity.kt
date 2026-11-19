
package com.example.applocker.settings

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class AutoLockActivity : Activity() {

    private val timeoutOptions = listOf(
        "Immediately" to 0,
        "30 Seconds" to 30,
        "1 Minute" to 60,
        "2 Minutes" to 120,
        "5 Minutes" to 300
    )

    private val preferences by lazy {
        getSharedPreferences("app_locker_settings", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 35, 28, 25)
            setBackgroundColor(Color.rgb(244, 245, 250))
        }

        val title = TextView(this).apply {
            text = "AUTO-LOCK SETTINGS"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(32, 36, 58))
        }

        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Choose when your protected apps should lock again."
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(0, 15, 0, 25)
        }

        root.addView(subtitle)

        val radioGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }

        val savedTimeout = preferences.getInt(
            "auto_lock_timeout_seconds", 30
        )

        var selectedTimeout = savedTimeout

        timeoutOptions.forEach { option ->

            val radioButton = RadioButton(this).apply {
                text = option.first
                textSize = 17f
                setTextColor(Color.rgb(32, 36, 58))
                tag = option.second
                isChecked = option.second == savedTimeout
                setPadding(12, 12, 12, 12)
            }

            radioButton.setOnClickListener {
                selectedTimeout = option.second
            }

            radioGroup.addView(
                radioButton,
                RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        root.addView(radioGroup)

        val saveButton = Button(this).apply {
            text = "SAVE AUTO-LOCK SETTINGS"

            setOnClickListener {
                preferences.edit()
                    .putInt(
                        "auto_lock_timeout_seconds",
                        selectedTimeout
                    )
                    .apply()

                Toast.makeText(
                    this@AutoLockActivity,
                    "Auto-lock preference saved!",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
        }

        root.addView(saveButton)

        setContentView(root)
    }
}

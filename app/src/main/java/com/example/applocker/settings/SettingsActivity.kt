
package com.example.applocker.settings

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 32)
            setBackgroundColor(Color.rgb(244, 245, 250))
        }

        val title = TextView(this).apply {
            text = "Privacy & Settings"
            textSize = 26f
            setTextColor(Color.rgb(32, 36, 58))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val privacy = TextView(this).apply {
            text = """
                YOUR PRIVACY

                • Authentication details are stored locally.
                • No account or cloud login is required.
                • App protection uses Android Accessibility Service.
                • You control which apps are protected.

                SERVICE PERMISSION

                Accessibility access is required to detect when
                protected apps are opened and display the lock screen.

                You can enable or disable this permission anytime
                from Android Settings.
            """.trimIndent()
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(0, 28, 0, 28)
        }

        val accessibilityButton = Button(this).apply {
    text = "Enable App Locker Service"
    setOnClickListener {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}

        val backButton = Button(this).apply {
            text = "Back"
            setOnClickListener { finish() }
        }

        layout.addView(title)
        layout.addView(privacy)
        layout.addView(accessibilityButton)
        layout.addView(backButton)

        setContentView(layout)
    }
}

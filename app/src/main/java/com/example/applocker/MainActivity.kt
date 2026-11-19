
package com.example.applocker

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.content.Context
import android.widget.Button
import android.widget.TextView
import com.example.applocker.auth.AuthSetupActivity
import com.example.applocker.apps.AppSelectionActivity
import com.example.applocker.settings.AutoLockActivity

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnAuthentication).setOnClickListener {
            startActivity(Intent(this, AuthSetupActivity::class.java))
        }

        findViewById<Button>(R.id.btnManageApps).setOnClickListener {
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }

        findViewById<Button>(R.id.btnAutoLock).setOnClickListener {
            startActivity(Intent(this, AutoLockActivity::class.java))
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
    startActivity(
        Intent(this, com.example.applocker.settings.SettingsActivity::class.java)
    )
}

        findViewById<Button>(R.id.btnQuickLock).setOnClickListener {
            sendBroadcast(
                Intent("com.example.applocker.QUICK_LOCK")
                    .setPackage(packageName)
            )
            updateDashboard()
        }
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
    }

    private fun updateDashboard() {
        val preferences = getSharedPreferences(
            "app_locker_settings",
            Context.MODE_PRIVATE
        )

        val lockedApps = preferences.getStringSet(
            "locked_apps",
            emptySet()
        ) ?: emptySet()

        findViewById<TextView>(R.id.txtLockedAppsCount).text =
            "${lockedApps.size} apps protected"

        val accessibilityManager =
            getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

        val serviceEnabled = accessibilityManager
            .getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )
            .any {
                it.resolveInfo.serviceInfo.packageName == packageName &&
                it.resolveInfo.serviceInfo.name.endsWith(
                    "AppLockAccessibilityService"
                )
            }

        findViewById<TextView>(R.id.txtProtectionStatus).text =
            if (serviceEnabled) "Service Enabled" else "Not Activated"
    }
}

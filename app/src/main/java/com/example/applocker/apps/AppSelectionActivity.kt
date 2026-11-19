
package com.example.applocker.apps

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.text.Editable
import android.text.TextWatcher

class AppSelectionActivity : Activity() {

    private lateinit var repository: AppRepository
    private lateinit var appList: LinearLayout
    private lateinit var searchBox: EditText
    private lateinit var countText: TextView

    private val selectedApps = mutableSetOf<String>()
    private var allApps = listOf<AppInfo>()

    private val preferences by lazy {
        getSharedPreferences("app_locker_settings", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = AppRepository(this)

        selectedApps.addAll(
            preferences.getStringSet("locked_apps", emptySet()) ?: emptySet()
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 25, 24, 20)
            setBackgroundColor(Color.rgb(244, 245, 250))
        }

        val title = TextView(this).apply {
            text = "SELECT APPS TO LOCK"
            textSize = 23f
            setTextColor(Color.rgb(32, 36, 58))
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 10)
        }
        root.addView(title)

        countText = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 5, 0, 15)
        }
        root.addView(countText)

        searchBox = EditText(this).apply {
            hint = "Search installed apps..."
            maxLines = 1
            setPadding(15, 10, 15, 10)
            setBackgroundColor(Color.WHITE)
        }

        root.addView(searchBox)

        val scrollView = ScrollView(this)
        appList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(appList)
        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val saveButton = Button(this).apply {
            text = "SAVE LOCKED APPS"
            setOnClickListener {
                preferences.edit()
                    .putStringSet("locked_apps", selectedApps.toSet())
                    .apply()

                Toast.makeText(
                    this@AppSelectionActivity,
                    "${selectedApps.size} apps selected",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
        }

        root.addView(saveButton)

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                displayApps(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        setContentView(root)

        loadApps()
    }

    private fun loadApps() {
        Thread {
            val result = repository.getInstalledApps()

            runOnUiThread {
                allApps = result
                displayApps("")
            }
        }.start()
    }

    private fun displayApps(query: String) {

        appList.removeAllViews()

        val filteredApps = allApps.filter {
            it.appName.contains(query, ignoreCase = true)
        }

        filteredApps.forEach { app ->

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(10, 10, 10, 10)
            }

            val icon = ImageView(this).apply {
                setImageDrawable(app.appIcon)
            }

            row.addView(
                icon,
                LinearLayout.LayoutParams(52, 52)
            )

            val name = TextView(this).apply {
                text = app.appName
                textSize = 16f
                setTextColor(Color.rgb(32, 36, 58))
                setPadding(15, 0, 0, 0)
            }

            row.addView(
                name,
                LinearLayout.LayoutParams(0, 60, 1f)
            )

            val checkbox = CheckBox(this).apply {
                isChecked = selectedApps.contains(app.packageName)

                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        selectedApps.add(app.packageName)
                    } else {
                        selectedApps.remove(app.packageName)
                    }

                    updateCount()
                }
            }

            row.addView(checkbox)

            appList.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            val divider = View(this).apply {
                setBackgroundColor(Color.LTGRAY)
            }

            appList.addView(
                divider,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1
                )
            )
        }

        updateCount()
    }

    private fun updateCount() {
        countText.text = "${selectedApps.size} apps selected"
    }
}

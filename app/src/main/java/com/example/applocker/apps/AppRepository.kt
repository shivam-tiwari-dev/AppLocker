
package com.example.applocker.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class AppRepository(context: Context) {

    private val appContext = context.applicationContext

    fun getInstalledApps(): List<AppInfo> {

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = appContext.packageManager
            .queryIntentActivities(intent, 0)
            .mapNotNull { resolveInfo ->

                val packageName =
                    resolveInfo.activityInfo?.packageName ?: return@mapNotNull null

                if (packageName == appContext.packageName) {
                    return@mapNotNull null
                }

                val appName = resolveInfo.loadLabel(
                    appContext.packageManager
                ).toString()

                val appIcon = resolveInfo.loadIcon(
                    appContext.packageManager
                )

                AppInfo(
                    appName = appName,
                    packageName = packageName,
                    appIcon = appIcon
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }

        return apps
    }
}

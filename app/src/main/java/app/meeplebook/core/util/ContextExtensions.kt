package app.meeplebook.core.util

import android.content.Context
import android.content.pm.PackageManager

fun Context.versionName(): String {
    return try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "?.?"
    } catch (e: PackageManager.NameNotFoundException) {
        "?.?"
    }
}
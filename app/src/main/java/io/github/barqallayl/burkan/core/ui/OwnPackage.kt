package io.github.barqallayl.burkan.core.ui

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

/** This app as Android has it installed: its version name and code, for Settings, a crash report and the log. */
fun Context.ownPackageInfo(): PackageInfo =
    packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))

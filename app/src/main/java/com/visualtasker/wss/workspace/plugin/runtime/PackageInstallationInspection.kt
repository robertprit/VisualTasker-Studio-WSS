package com.visualtasker.wss.workspace.plugin.runtime

import android.content.pm.PackageManager
import android.os.Build

internal data class PackageInstallationInspection(
    val installed: Boolean,
    val packageName: String? = null,
    val failure: Exception? = null,
) {
    init {
        require(!installed || packageName != null)
        require(!installed || failure == null)
    }
}

internal fun inspectSupportedPackages(
    packageNames: List<String>,
    lookup: (String) -> Unit,
): PackageInstallationInspection {
    var firstFailure: Exception? = null
    packageNames.forEach { packageName ->
        try {
            lookup(packageName)
            return PackageInstallationInspection(installed = true, packageName = packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            // A completed negative lookup is a valid false answer.
        } catch (error: Exception) {
            if (firstFailure == null) firstFailure = error
        }
    }
    return PackageInstallationInspection(installed = false, failure = firstFailure)
}

internal fun PackageManager.inspectInstalledPackages(
    packageNames: List<String>,
): PackageInstallationInspection = inspectSupportedPackages(packageNames) { packageName ->
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, 0)
    }
}

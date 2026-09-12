package com.visualtasker.wss.workspace.plugin.runtime

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService
import androidx.core.content.ContextCompat

const val CUSTOM_CHROME_TAB_PLUGIN_OWNER = "visualtasker.customtabs"

data class CustomChromeTabStatus(
    val supported: Boolean,
    val preferredPackage: String?,
    val packageCount: Int,
) {
    val summary: String
        get() = when {
            supported -> "Custom Tabs bereit: ${preferredPackage ?: "System"}"
            else -> "Kein Custom-Tabs-fähiger Browser gefunden"
        }
}

data class CustomChromeTabSettings(
    val toolbarColor: Int = 0xFF1B1626.toInt(),
    val navigationBarColor: Int = 0xFF120F1A.toInt(),
    val showTitle: Boolean = true,
    val shareEnabled: Boolean = false,
    val downloadMenuEnabled: Boolean = true,
    val favoriteMenuEnabled: Boolean = true,
    val bottomBarEnabled: Boolean = true,
    val actionButtonIcon: String = "open",
    val closeButtonIcon: String = "close",
)

data class CustomChromeTabResult(
    val success: Boolean,
    val message: String,
    val warning: Boolean = !success,
)

object CustomChromeTabRegistration {
    fun inspect(context: Context): CustomChromeTabStatus {
        val intent = Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
        val services = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentServices(
                intent,
                PackageManager.ResolveInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentServices(intent, 0)
        }
        val preferred = services.firstOrNull { it.serviceInfo?.packageName == "com.android.chrome" }
            ?: services.firstOrNull()
        return CustomChromeTabStatus(
            supported = preferred != null,
            preferredPackage = preferred?.serviceInfo?.packageName,
            packageCount = services.size,
        )
    }

    fun open(
        context: Context,
        rawUrl: String,
        settings: CustomChromeTabSettings = CustomChromeTabSettings(),
    ): CustomChromeTabResult {
        val status = inspect(context)
        val uri = normalizeUrl(rawUrl)
            ?: return CustomChromeTabResult(false, "ChromeTab.open: ungültige URL '$rawUrl'")
        val builder = CustomTabsIntent.Builder()
            .setShowTitle(settings.showTitle)
            .setToolbarColor(settings.toolbarColor)
            .setNavigationBarColor(settings.navigationBarColor)
            .setShareState(
                if (settings.shareEnabled) {
                    CustomTabsIntent.SHARE_STATE_ON
                } else {
                    CustomTabsIntent.SHARE_STATE_OFF
                },
            )
            .setCloseButtonIcon(actionButtonBitmap(settings.closeButtonIcon))
        val appReturnIntent = PendingIntent.getActivity(
            context,
            1301,
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(Intent.ACTION_MAIN),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (settings.actionButtonIcon.isNotBlank()) {
            builder.setActionButton(
                actionButtonBitmap(settings.actionButtonIcon),
                settings.actionButtonIcon,
                appReturnIntent,
                false,
            )
        }
        if (settings.downloadMenuEnabled) {
            builder.addMenuItem("Download", appReturnIntent)
        }
        if (settings.favoriteMenuEnabled) {
            builder.addMenuItem("Favorit", appReturnIntent)
        }
        val intent = builder.build()
        status.preferredPackage?.let { intent.intent.setPackage(it) }
        intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            intent.launchUrl(context, uri)
            CustomChromeTabResult(true, "ChromeTab.open(${uri}) übergeben", warning = false)
        }.getOrElse { error ->
            runCatching {
                ContextCompat.startActivity(
                    context,
                    Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    null,
                )
                CustomChromeTabResult(
                    success = status.supported,
                    message = "ChromeTab fallback ACTION_VIEW(${uri}); ${error.message.orEmpty()}",
                    warning = !status.supported,
                )
            }.getOrElse { fallbackError ->
                CustomChromeTabResult(false, "ChromeTab.open fehlgeschlagen: ${fallbackError.message.orEmpty()}")
            }
        }
    }

    private fun normalizeUrl(raw: String): Uri? {
        val trimmed = raw.trim().trim('"')
        if (trimmed.isBlank()) return null
        val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
        return runCatching { Uri.parse(withScheme) }
            .getOrNull()
            ?.takeIf { it.scheme in setOf("http", "https") && !it.host.isNullOrBlank() }
    }

    private fun actionButtonBitmap(label: String): Bitmap {
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 26f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawColor(AndroidColor.TRANSPARENT)
        canvas.drawText(label.firstOrNull()?.uppercaseChar()?.toString() ?: "V", 24f, 33f, paint)
        return bitmap
    }
}

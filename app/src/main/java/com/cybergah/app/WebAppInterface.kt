package com.cybergah.app

import android.content.Context
import android.widget.Toast
import android.webkit.JavascriptInterface

/**
 * JavaScript → Android köprüsü.
 * Web sayfasından CybergahApp.xxx() ile çağrılabilir.
 */
class WebAppInterface(private val context: Context) {

    /** Kullanıcıya toast mesajı göster */
    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /** Uygulama versiyonunu döndür */
    @JavascriptInterface
    fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /** Paylaşım dialog'u aç */
    @JavascriptInterface
    fun share(title: String, url: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, title)
            putExtra(android.content.Intent.EXTRA_TEXT, "$title\n$url")
        }
        val chooser = android.content.Intent.createChooser(intent, "Paylaş")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /** Uygulama mı yoksa tarayıcı mı kontrol et */
    @JavascriptInterface
    fun isApp(): Boolean = true

    /** Cihaz temasını döndür (dark/light) */
    @JavascriptInterface
    fun getDeviceTheme(): String {
        val nightMode = context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
    }
}

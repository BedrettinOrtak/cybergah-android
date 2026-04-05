package com.cybergah.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.cybergah.app.databinding.ActivityMainBinding
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    companion object {
        const val BASE_URL = "https://cybergah.com"
        const val TAG = "CybergahApp"
    }

    private lateinit var binding: ActivityMainBinding
    private var isLoading = true
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    // Dosya yükleme launcher
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        fileUploadCallback?.onReceiveValue(uris.toTypedArray())
        fileUploadCallback = null
    }

    // Bildirim izni launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            subscribeToTopics()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash Screen
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { isLoading }

        super.onCreate(savedInstanceState)

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Status bar rengi
        window.statusBarColor = Color.parseColor("#0F172A")
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        setupSwipeRefresh()
        setupBackNavigation()
        requestNotificationPermission()

        // Deep link veya normal açılış
        val url = intent?.data?.toString() ?: BASE_URL
        binding.webView.loadUrl(url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
                allowFileAccess = true
                setSupportMultipleWindows(false)
                userAgentString = "${settings.userAgentString} CybergahApp/1.0"
                // Media otomatik oynatma
                mediaPlaybackRequiresUserGesture = false
            }

            // JavaScript interface
            addJavascriptInterface(WebAppInterface(this@MainActivity), "CybergahApp")

            // WebView client
            webViewClient = object : WebViewClient() {

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.visibility = View.VISIBLE
                    binding.errorLayout.visibility = View.GONE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    isLoading = false

                    // Dark mode senkronizasyonu
                    injectDarkModeSync()
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        showError()
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false

                    // Cybergah domain'i → WebView'da aç
                    if (url.contains("cybergah.com")) {
                        return false
                    }

                    // Harici linkler → tarayıcıda aç
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (_: Exception) { }
                    return true
                }
            }

            // Chrome client - dosya yükleme, fullscreen video, vb.
            webChromeClient = object : WebChromeClient() {

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    binding.progressBar.progress = newProgress
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    fileUploadCallback?.onReceiveValue(null)
                    fileUploadCallback = filePathCallback
                    fileChooserLauncher.launch("*/*")
                    return true
                }

                // Fullscreen video desteği
                private var customView: View? = null
                private var customViewCallback: CustomViewCallback? = null

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    customView?.let {
                        onHideCustomView()
                        return
                    }
                    customView = view
                    customViewCallback = callback
                    binding.fullscreenContainer.apply {
                        addView(view)
                        visibility = View.VISIBLE
                    }
                    binding.webView.visibility = View.GONE

                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }

                override fun onHideCustomView() {
                    binding.fullscreenContainer.apply {
                        removeAllViews()
                        visibility = View.GONE
                    }
                    binding.webView.visibility = View.VISIBLE
                    customView = null
                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null

                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // İndirme desteği
            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                try {
                    val request = DownloadManager.Request(Uri.parse(url)).apply {
                        setMimeType(mimeType)
                        addRequestHeader("User-Agent", userAgent)
                        setDescription("Cybergah'tan indiriliyor...")
                        setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            URLUtil.guessFileName(url, contentDisposition, mimeType)
                        )
                    }
                    val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    dm.enqueue(request)
                    Toast.makeText(this@MainActivity, "İndirme başladı", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "İndirme hatası", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeColors(
                ContextCompat.getColor(this@MainActivity, R.color.brand_red),
                ContextCompat.getColor(this@MainActivity, R.color.brand_red_dark)
            )
            setOnRefreshListener {
                binding.webView.reload()
            }

            // WebView scroll konumuna göre swipe refresh'i kontrol et
            setOnChildScrollUpCallback { _, _ ->
                binding.webView.scrollY > 0
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    // Fullscreen'den çık
                    binding.fullscreenContainer.visibility == View.VISIBLE -> {
                        binding.webView.webChromeClient?.onHideCustomView()
                    }
                    // Hata ekranından geri
                    binding.errorLayout.visibility == View.VISIBLE -> {
                        binding.errorLayout.visibility = View.GONE
                        binding.webView.visibility = View.VISIBLE
                        binding.webView.goBack()
                    }
                    // WebView geçmişinde geri git
                    binding.webView.canGoBack() -> {
                        binding.webView.goBack()
                    }
                    // Uygulamadan çık
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                subscribeToTopics()
            }
        } else {
            subscribeToTopics()
        }
    }

    private fun subscribeToTopics() {
        FirebaseMessaging.getInstance().apply {
            // Tüm kullanıcılar için genel bildirimler
            subscribeToTopic("all")
            // Yeni yazılar
            subscribeToTopic("new_content")
            // Forum bildirimleri
            subscribeToTopic("forum")
        }
    }

    private fun showError() {
        binding.webView.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        binding.swipeRefresh.isRefreshing = false
        isLoading = false
    }

    fun retryConnection() {
        if (isNetworkAvailable()) {
            binding.errorLayout.visibility = View.GONE
            binding.webView.visibility = View.VISIBLE
            binding.webView.reload()
        } else {
            Toast.makeText(this, "İnternet bağlantısı yok", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun injectDarkModeSync() {
        // Android sistem temasına göre dark mode ayarla
        val isDark = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val js = if (isDark) {
            "document.documentElement.classList.add('dark');" +
            "localStorage.setItem('theme', 'dark');"
        } else {
            // Kullanıcı zaten sitede dark mode seçtiyse dokunma
            "if(!localStorage.getItem('theme')){" +
            "document.documentElement.classList.remove('dark');}"
        }
        binding.webView.evaluateJavascript(js, null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Deep link ile açılan URL'ler
        intent.data?.toString()?.let { url ->
            if (url.contains("cybergah.com")) {
                binding.webView.loadUrl(url)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }
}

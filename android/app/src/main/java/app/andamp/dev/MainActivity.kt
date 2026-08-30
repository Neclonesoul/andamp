package app.andamp.dev

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.webkit.WebViewAssetLoader
import app.andamp.dev.bridge.AndampBridge
import app.andamp.dev.library.MediaStoreRepository
import app.andamp.dev.playback.PlaybackService
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : ComponentActivity() {

    companion object {
        private const val APP_HOST = "appassets.androidplatform.net"
        private const val APP_URL =
            "https://appassets.androidplatform.net/assets/index.html"
    }

    private lateinit var webView: WebView
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null

    private val mediaPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestMediaPermissionIfRequired()
        connectMediaController()

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(
                "/assets/",
                WebViewAssetLoader.AssetsPathHandler(this)
            )
            .build()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            // The application UI is packaged with the APK. The WebView does
            // not need arbitrary filesystem or content-provider access.
            settings.allowFileAccess = false
            settings.allowContentAccess = false

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    return request?.url?.let(assetLoader::shouldInterceptRequest)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val uri = request?.url ?: return true
                    return !isTrustedAppUri(uri)
                }
            }

            addJavascriptInterface(
                AndampBridge(
                    controller = { controller },
                    library = MediaStoreRepository(contentResolver),
                    emit = { payload ->
                        post {
                            evaluateJavascript(
                                "window.__andampReceive?.(" +
                                    org.json.JSONObject.quote(payload) +
                                    ");",
                                null
                            )
                        }
                    }
                ),
                "AndampNative"
            )

            loadUrl(APP_URL)
        }

        setContentView(webView)
    }

    private fun requestMediaPermissionIfRequired() {
        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            mediaPermission.launch(Manifest.permission.READ_MEDIA_AUDIO)
        }
    }

    private fun connectMediaController() {
        val token = SessionToken(
            this,
            ComponentName(this, PlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(this, token).buildAsync()

        controllerFuture.addListener(
            {
                controller =
                    runCatching { controllerFuture.get() }.getOrNull()
            },
            mainExecutor
        )
    }

    private fun isTrustedAppUri(uri: Uri): Boolean {
        return uri.scheme == "https" &&
            uri.host == APP_HOST &&
            uri.path?.startsWith("/assets/") == true
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface("AndampNative")

        if (::controllerFuture.isInitialized) {
            MediaController.releaseFuture(controllerFuture)
        }

        super.onDestroy()
    }
}

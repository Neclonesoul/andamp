package app.andamp.dev

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.andamp.dev.bridge.AndampBridge
import app.andamp.dev.playback.PlaybackService
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null

    private val mediaPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            mediaPermission.launch(Manifest.permission.READ_MEDIA_AUDIO)
        }
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture.addListener({ controller = runCatching { controllerFuture.get() }.getOrNull() }, mainExecutor)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val u=request?.url ?: return true
                    return u.scheme != "https" || u.host != "appassets.androidplatform.net"
                }
            }
            addJavascriptInterface(
                AndampBridge(
                    controller = { controller },
                    emit = { payload -> post { evaluateJavascript("window.__andampReceive?.(${org.json.JSONObject.quote(payload)});", null) } }
                ), "AndampNative"
            )
            loadUrl("https://appassets.androidplatform.net/assets/index.html")
        }
        setContentView(webView)
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface("AndampNative")
        if (::controllerFuture.isInitialized) MediaController.releaseFuture(controllerFuture)
        super.onDestroy()
    }
}

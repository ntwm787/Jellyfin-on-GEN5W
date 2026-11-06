package org.jellyfin.mobile.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.launch
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.mobile.AppPreferences
import org.jellyfin.mobile.BuildConfig
import org.jellyfin.mobile.R
import org.jellyfin.mobile.MainActivity
import org.jellyfin.mobile.playback.NativePlaybackBridge
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jellyfin.mobile.bridge.ExternalPlayer
import org.jellyfin.mobile.bridge.NativeInterface
import org.jellyfin.mobile.bridge.NativePlayer
import org.jellyfin.mobile.databinding.FragmentWebviewBinding
import org.jellyfin.mobile.utils.*
import org.jellyfin.mobile.webapp.WebappFunctionChannel
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.Reader
import java.util.*

class WebViewFragment : Fragment() {
    val apiClient: ApiClient by inject()
    private val appPreferences: AppPreferences by inject()
    private val webappFunctionChannel: WebappFunctionChannel by inject()
    private val externalPlayer by lazy { ExternalPlayer(this) }
    private val nativeWebClient: OkHttpClient by inject()

    private var connected = false

    // UI
    private var _webViewBinding: FragmentWebviewBinding? = null
    private val webViewBinding get() = _webViewBinding!!
    val webView: WebView get() = webViewBinding.root

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (!connected || !webappFunctionChannel.triggerInputManagerAction(Constants.INPUT_MANAGER_COMMAND_BACK)) {
                isEnabled = false
                activity?.onBackPressed()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _webViewBinding = FragmentWebviewBinding.inflate(inflater, container, false)
        return webView.apply { applyWindowInsetsAsMargins() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply window insets
        ViewCompat.requestApplyInsets(webView)

        // Setup exclusion rects for gestures
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webView.doOnNextLayout { webView ->
                // Maximum allowed exclusion rect height is 200dp,
                // offsetting 100dp from the center in both directions
                // uses the maximum available space
                val verticalCenter = webView.measuredHeight / 2
                val offset = webView.context.dip(100)

                // Arbitrary, currently 2x minimum touch target size
                val exclusionWidth = webView.context.dip(96)

                webView.systemGestureExclusionRects = listOf(
                    Rect(
                        0,
                        verticalCenter - offset,
                        exclusionWidth,
                        verticalCenter + offset
                    )
                )
            }
        }

        // Setup WebView
        webView.initialize()

        // Process JS functions called from other components (e.g. the PlayerActivity)
        lifecycleScope.launch {
            for (function in webappFunctionChannel) {
                webView.loadUrl("javascript:$function")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _webViewBinding = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun WebView.initialize() {
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
        setDownloadListener { _, _, _, _, _ ->
            android.widget.Toast.makeText(context, "Downloads are disabled on this device.", android.widget.Toast.LENGTH_SHORT).show()
        }
        webViewClient = object : WebViewClientCompat() {
            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (org.jellyfin.mobile.playback.ContentDownloadPolicy.blocks(url)) return true
                val target = Uri.parse(url)
                val server = Uri.parse(appPreferences.instanceUrl)
                return target.scheme != server.scheme || target.authority != server.authority
            }
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
            }

            @android.annotation.TargetApi(21)
            override fun shouldInterceptRequest(webView: WebView, request: WebResourceRequest): WebResourceResponse? =
                interceptRequest(webView, request.url)

            @Suppress("DEPRECATION")
            override fun shouldInterceptRequest(webView: WebView, url: String): WebResourceResponse? =
                interceptRequest(webView, Uri.parse(url))

            private fun interceptRequest(webView: WebView, url: Uri): WebResourceResponse? {
                val path = url.path?.toLowerCase(Locale.ROOT) ?: return null
                val server = Uri.parse(appPreferences.instanceUrl)
                val sameOrigin = url.scheme == server.scheme && url.authority == server.authority
                // This is an APK asset, not network traffic. Keep the guarded
                // player installed even if connectivity changes during startup.
                if (sameOrigin && path.endsWith("/gen5w-client.js")) return webView.context.loadAsset("gen5w-client.js")
                if ((url.scheme == "http" || url.scheme == "https") && !org.jellyfin.mobile.playback.NetworkGate.allows(webView.context)) return emptyResponse
                if (org.jellyfin.mobile.playback.ContentDownloadPolicy.blocks(url.toString())) return emptyResponse
                return when {
                    sameOrigin && (path.endsWith("/web/") || path.endsWith("/web/index.html")) -> {
                        try {
                            nativeWebClient.newCall(Request.Builder().url(url.toString()).build()).execute().use { response ->
                                val body = response.body()?.string() ?: return emptyResponse
                                if (!body.contains("<head>")) return emptyResponse
                                val html = body.replaceFirst("<head>", "<head><script src=\"gen5w-client.js\"></script>")
                                WebResourceResponse("text/html", "UTF-8", html.byteInputStream())
                            }
                        } catch (error: Exception) {
                            Timber.e("Could not prepare native web client: %s", error.javaClass.simpleName)
                            emptyResponse
                        }
                    }
                    path.endsWith(Constants.APPLOADER_PATH) -> {
                        runOnUiThread {
                            webView.evaluateJavascript(JS_INJECTION_CODE) {
                                onConnectedToWebapp()
                            }
                        }
                        null // continue loading normally
                    }
                    path.contains("native") -> webView.context.loadAsset("native/${url.lastPathSegment}")
                    path.endsWith(Constants.SELECT_SERVER_PATH) -> {
                        runOnUiThread { onSelectServer() }
                        emptyResponse
                    }
                    path.endsWith(Constants.SESSION_CAPABILITIES_PATH) -> {
                        runOnUiThread {
                            webView.evaluateJavascript("window.localStorage.getItem('jellyfin_credentials')") { result ->
                                try {
                                    val credentials = JSONObject(result.unescapeJson())
                                    val server = credentials.getJSONArray("Servers").getJSONObject(0)
                                    val address = server.getString("ManualAddress")
                                    val user = server.getString("UserId")
                                    val token = server.getString("AccessToken")
                                    appPreferences.instanceUserId = user
                                    appPreferences.instanceAccessToken = token
                                    apiClient.ChangeServerLocation(address.trimEnd('/'))
                                    apiClient.SetAuthenticationInfo(token, user)
                                    initLocale()
                                } catch (e: JSONException) {
                                    Timber.e(e, "Failed to extract apiclient credentials")
                                }
                            }
                        }
                        null
                    }
                    else -> null
                }
            }

            @Suppress("DEPRECATION")
            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                Timber.e("Received WebView error %d at %s: %s", errorCode, failingUrl, description)
                runOnUiThread { onErrorReceived() }
            }

            @android.annotation.TargetApi(23)
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                val errorMessage = errorResponse.data?.run { bufferedReader().use(Reader::readText) }
                Timber.e("Received WebView HTTP %d error: %s", errorResponse.statusCode, errorMessage)

                if (request.url == Uri.parse(appPreferences.instanceUrl))
                    runOnUiThread { onErrorReceived() }
            }

            @android.annotation.TargetApi(21)
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat) {
                val description = if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION)) error.description else null
                Timber.e("Received WebView error at %s: %s", request.url.toString(), description)
                if (request.url.toString() == appPreferences.instanceUrl)
                    runOnUiThread { onErrorReceived() }
            }
        }
        webChromeClient = WebChromeClient()
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // The web player fetches stream info asynchronously after the user
            // presses Play; KitKat otherwise drops the original user gesture.
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) mediaPlaybackRequiresUserGesture = false
        }
        addJavascriptInterface(NativeInterface(this@WebViewFragment), "NativeInterface")
        addJavascriptInterface(NativePlayer(parentFragmentManager), "NativePlayer")
        addJavascriptInterface(externalPlayer, "ExternalPlayer")
        addJavascriptInterface(NativePlaybackBridge(requireContext(), requireNotNull(appPreferences.instanceUrl)), "NativePlayback")

        val serverUrl = requireNotNull(appPreferences.instanceUrl) { "Server url has not been set!" }.trimEnd('/')
        // KitKat does not consistently intercept redirect targets. Request the
        // web entry point directly so the native plugin loads before app init.
        val webUrl = if (serverUrl.endsWith("/web")) "$serverUrl/" else "$serverUrl/web/"
        loadUrl("${webUrl}?gen5wNative=${BuildConfig.VERSION_NAME}")
    }

    fun onConnectedToWebapp() {
        connected = true
        (activity as? MainActivity)?.requestNoBatteryOptimizations()
    }

    fun returnToBrowse() {
        _webViewBinding?.root?.evaluateJavascript(
            "if (window.__gen5wReturnToBrowse) window.__gen5wReturnToBrowse();", null)
    }

    fun selectNativeTrack(kind: String, index: Int) {
        if (kind != "audio" && kind != "subtitle") return
        _webViewBinding?.root?.evaluateJavascript(
            "window.__gen5wSelectTrack && window.__gen5wSelectTrack('$kind',$index);"
        ) { result ->
            if (result != "true") android.widget.Toast.makeText(context,
                R.string.video_track_change_unavailable, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun onSelectServer(error: Boolean = false) {
        val activity = activity
        if (activity != null && activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (error) {
                val extras = Bundle().apply {
                    putBoolean(Constants.FRAGMENT_CONNECT_EXTRA_ERROR, true)
                }
                parentFragmentManager.replaceFragment<ConnectFragment>(extras)
            } else {
                parentFragmentManager.addFragment<ConnectFragment>()
            }
        }
    }

    fun onErrorReceived() {
        connected = false
        onSelectServer(error = true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.HANDLE_EXTERNAL_PLAYER) {
            externalPlayer.handleActivityResult(resultCode, data)
        }
    }
}

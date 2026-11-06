package org.jellyfin.mobile

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.OrientationEventListener
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import org.jellyfin.mobile.utils.toHttpUrlOrNull
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.mobile.cast.Chromecast
import org.jellyfin.mobile.cast.IChromecast
import org.jellyfin.mobile.fragment.ConnectFragment
import org.jellyfin.mobile.fragment.WebViewFragment
import org.jellyfin.mobile.player.PlayerFragment
import org.jellyfin.mobile.utils.PermissionRequestHelper
import org.jellyfin.mobile.utils.SmartOrientationListener
import org.jellyfin.mobile.utils.lazyView
import org.jellyfin.mobile.utils.replaceFragment
import org.jellyfin.mobile.webapp.RemotePlayerService
import org.koin.android.ext.android.inject
import org.koin.androidx.fragment.android.setupKoinFragmentFactory

class MainActivity : AppCompatActivity() {
    val apiClient: ApiClient by inject()
    val appPreferences: AppPreferences by inject()
    val chromecast: IChromecast = Chromecast()
    private val permissionRequestHelper: PermissionRequestHelper by inject()

    val rootView: CoordinatorLayout by lazyView(R.id.root_view)

    var serviceBinder: RemotePlayerService.ServiceBinder? = null
        private set
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            serviceBinder = binder as? RemotePlayerService.ServiceBinder
        }

        override fun onServiceDisconnected(componentName: ComponentName) {}
    }

    private val orientationListener: OrientationEventListener by lazy { SmartOrientationListener(this) }
    private var nativeVideoOverlay: org.jellyfin.mobile.playback.NativeVideoOverlay? = null
    private var videoKeptScreenOn = false

    fun showNativeVideo() {
        if (nativeVideoOverlay != null || isFinishing) return
        val service = org.jellyfin.mobile.playback.NativePlaybackService.instance ?: return
        if (!service.hasActiveVideo() || !org.jellyfin.mobile.playback.VehicleVideoGate.allowsVideo()) return
        val overlay = org.jellyfin.mobile.playback.NativeVideoOverlay(this,
            org.jellyfin.mobile.playback.NativeVideoOverlay.CloseListener { stop -> hideNativeVideo(stop) })
        nativeVideoOverlay = overlay
        videoKeptScreenOn = window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        rootView.addView(overlay, CoordinatorLayout.LayoutParams(-1, -1))
        overlay.bringToFront()
        overlay.requestFocus()
    }

    fun hideNativeVideo(stopPlayback: Boolean) {
        val overlay = nativeVideoOverlay ?: return
        nativeVideoOverlay = null
        overlay.release()
        rootView.removeView(overlay)
        if (!videoKeptScreenOn) window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (stopPlayback) {
            org.jellyfin.mobile.playback.NativePlaybackService.instance?.stopPlayback()
            supportFragmentManager.fragments.filterIsInstance<WebViewFragment>().firstOrNull()?.returnToBrowse()
        }
    }

    fun selectNativeTrack(kind: String, index: Int) {
        supportFragmentManager.fragments.filterIsInstance<WebViewFragment>().firstOrNull()?.selectNativeTrack(kind, index)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupKoinFragmentFactory()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind player service
        bindService(Intent(this, RemotePlayerService::class.java), serviceConnection, Service.BIND_AUTO_CREATE)

        // Load UI
        appPreferences.instanceUrl?.toHttpUrlOrNull().also { url ->
            with(supportFragmentManager) {
                if (url != null && org.jellyfin.mobile.playback.NetworkGate.allows(this@MainActivity)) {
                    replaceFragment<WebViewFragment>()
                } else {
                    replaceFragment<ConnectFragment>()
                }
            }
        }

        // Setup Chromecast
        chromecast.initializePlugin(this)
    }

    override fun onStart() {
        super.onStart()
        orientationListener.enable()
    }

    override fun onResume() {
        super.onResume()
        if (nativeVideoOverlay != null && org.jellyfin.mobile.playback.NativePlaybackService.instance?.hasActiveVideo() != true)
            hideNativeVideo(false)
        org.jellyfin.mobile.playback.NativeVideoHost.resume(this)
    }

    override fun onPause() {
        org.jellyfin.mobile.playback.NativeVideoHost.pause(this)
        nativeVideoOverlay?.pause()
        org.jellyfin.mobile.playback.NativePlaybackService.instance?.leaveForeground()
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) = permissionRequestHelper.handleRequestPermissionsResult(requestCode, permissions, grantResults)

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onUserLeaveHint() {
        for (fragment in supportFragmentManager.fragments) {
            if (fragment is PlayerFragment && fragment.isVisible) {
                fragment.onUserLeaveHint()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        orientationListener.disable()
    }

    override fun onBackPressed() {
        if (nativeVideoOverlay != null) {
            hideNativeVideo(true)
            return
        }
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else super.onBackPressed()
    }

    override fun onDestroy() {
        nativeVideoOverlay?.release()
        nativeVideoOverlay = null
        if (org.jellyfin.mobile.playback.NativePlaybackService.instance?.hasActiveVideo() == true)
            org.jellyfin.mobile.playback.NativePlaybackService.instance?.stopPlayback()
        unbindService(serviceConnection)
        chromecast.destroy()
        super.onDestroy()
    }
}

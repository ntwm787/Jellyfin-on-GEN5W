package org.jellyfin.mobile.bridge

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.support.v4.media.session.PlaybackStateCompat as PlaybackState
import android.net.Uri
import android.webkit.JavascriptInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jellyfin.apiclient.interaction.AndroidDevice
import org.jellyfin.mobile.fragment.WebViewFragment
import org.jellyfin.mobile.settings.SettingsFragment
import org.jellyfin.mobile.utils.*
import org.jellyfin.mobile.utils.Constants.APP_INFO_NAME
import org.jellyfin.mobile.utils.Constants.APP_INFO_VERSION
import org.jellyfin.mobile.utils.Constants.EXTRA_ALBUM
import org.jellyfin.mobile.utils.Constants.EXTRA_ARTIST
import org.jellyfin.mobile.utils.Constants.EXTRA_CAN_SEEK
import org.jellyfin.mobile.utils.Constants.EXTRA_DURATION
import org.jellyfin.mobile.utils.Constants.EXTRA_IMAGE_URL
import org.jellyfin.mobile.utils.Constants.EXTRA_IS_LOCAL_PLAYER
import org.jellyfin.mobile.utils.Constants.EXTRA_IS_PAUSED
import org.jellyfin.mobile.utils.Constants.EXTRA_ITEM_ID
import org.jellyfin.mobile.utils.Constants.EXTRA_PLAYER_ACTION
import org.jellyfin.mobile.utils.Constants.EXTRA_POSITION
import org.jellyfin.mobile.utils.Constants.EXTRA_TITLE
import org.jellyfin.mobile.webapp.RemotePlayerService
import org.jellyfin.mobile.webapp.RemoteVolumeProvider
import org.jellyfin.mobile.webapp.WebappFunctionChannel
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

class NativeInterface(private val fragment: WebViewFragment) : KoinComponent {
    private val context: Context = fragment.requireContext()
    private val webappFunctionChannel: WebappFunctionChannel by inject()
    private val remoteVolumeProvider: RemoteVolumeProvider by inject()

    @SuppressLint("HardwareIds")
    @JavascriptInterface
    fun getDeviceInformation(): String? = try {
        val device = AndroidDevice.fromContext(context)
        JSONObject().apply {
            put("deviceId", device.deviceId)
            put("deviceName", device.deviceName)
            put("appName", APP_INFO_NAME)
            put("appVersion", APP_INFO_VERSION)
        }.toString()
    } catch (e: JSONException) {
        null
    }

    @JavascriptInterface
    fun enableFullscreen(): Boolean {
        fragment.runOnUiThread {
            fragment.activity?.apply {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                enableFullscreen()
            }
        }
        return true
    }

    @JavascriptInterface
    fun disableFullscreen(): Boolean {
        fragment.runOnUiThread {
            fragment.activity?.apply {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                this.disableFullscreen(true)
            }
        }
        return true
    }

    @JavascriptInterface
    fun openUrl(uri: String, target: String): Boolean = false

    @JavascriptInterface
    fun updateMediaSession(args: String): Boolean {
        val options = try {
            JSONObject(args)
        } catch (e: JSONException) {
            Timber.e("updateMediaSession: %s", e.message)
            return false
        }
        val intent = Intent(context, RemotePlayerService::class.java).apply {
            action = Constants.ACTION_REPORT
            putExtra(EXTRA_PLAYER_ACTION, options.optString(EXTRA_PLAYER_ACTION))
            putExtra(EXTRA_ITEM_ID, options.optString(EXTRA_ITEM_ID))
            putExtra(EXTRA_TITLE, options.optString(EXTRA_TITLE))
            putExtra(EXTRA_ARTIST, options.optString(EXTRA_ARTIST))
            putExtra(EXTRA_ALBUM, options.optString(EXTRA_ALBUM))
            putExtra(EXTRA_IMAGE_URL, options.optString(EXTRA_IMAGE_URL))
            putExtra(EXTRA_POSITION, options.optLong(EXTRA_POSITION, PlaybackState.PLAYBACK_POSITION_UNKNOWN))
            putExtra(EXTRA_DURATION, options.optLong(EXTRA_DURATION))
            putExtra(EXTRA_CAN_SEEK, options.optBoolean(EXTRA_CAN_SEEK))
            putExtra(EXTRA_IS_LOCAL_PLAYER, options.optBoolean(EXTRA_IS_LOCAL_PLAYER, true))
            putExtra(EXTRA_IS_PAUSED, options.optBoolean(EXTRA_IS_PAUSED, true))
        }
        context.startService(intent)
        return true
    }

    @JavascriptInterface
    fun hideMediaSession(): Boolean {
        val intent = Intent(context, RemotePlayerService::class.java).apply {
            action = Constants.ACTION_REPORT
            putExtra(EXTRA_PLAYER_ACTION, "playbackstop")
        }
        context.startService(intent)
        return true
    }

    @JavascriptInterface
    fun updateVolumeLevel(value: Int) {
        remoteVolumeProvider.currentVolume = value
    }

    @JavascriptInterface
    fun downloadFile(args: String): Boolean = false

    @JavascriptInterface
    fun openClientSettings() {
        fragment.parentFragmentManager.addFragment<SettingsFragment>()
    }

    @JavascriptInterface
    fun exitApp() {
        val activity = fragment.requireMainActivity()
        if (activity.serviceBinder?.isPlaying == true) {
            activity.moveTaskToBack(false)
        } else {
            activity.finish()
        }
    }

    @JavascriptInterface
    fun execCast(action: String, args: String) {
        fragment.requireMainActivity().chromecast.execute(action, JSONArray(args), object : JavascriptCallback() {
            override fun callback(keep: Boolean, err: String?, result: String?) {
                webappFunctionChannel.call("""window.NativeShell.castCallback("$action", $keep, $err, $result);""")
            }
        })
    }
}

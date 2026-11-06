package org.jellyfin.mobile

import coil.ImageLoader
import kotlinx.coroutines.channels.Channel
import okhttp3.OkHttpClient
import org.jellyfin.apiclient.AppInfo
import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.android
import org.jellyfin.apiclient.interaction.AndroidDevice
import org.jellyfin.mobile.api.TimberLogger
import org.jellyfin.mobile.fragment.ConnectFragment
import org.jellyfin.mobile.fragment.WebViewFragment
import org.jellyfin.mobile.player.PlayerEvent
import org.jellyfin.mobile.player.PlayerFragment
import org.jellyfin.mobile.utils.Constants
import org.jellyfin.mobile.utils.PermissionRequestHelper
import org.jellyfin.mobile.webapp.RemoteVolumeProvider
import org.jellyfin.mobile.webapp.WebappFunctionChannel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.fragment.dsl.fragment
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val PLAYER_EVENT_CHANNEL = "PlayerEventChannel"

val applicationModule = module {
    single { AppPreferences(androidApplication()) }
    single {
        val app = androidApplication()
        OkHttpClient.Builder().addInterceptor { chain ->
            if (!org.jellyfin.mobile.playback.NetworkGate.allows(app))
                throw java.io.IOException(app.getString(R.string.gen5w_network_required))
            chain.proceed(chain.request())
        }.build()
    }
    single { ImageLoader.Builder(androidApplication()).okHttpClient(get<OkHttpClient>()).build() }
    single {
        Jellyfin {
            appInfo = AppInfo(Constants.APP_INFO_NAME, Constants.APP_INFO_VERSION)
            logger = TimberLogger()
            android(androidApplication())
            httpClient = org.jellyfin.mobile.api.GuardedHttpClient(androidApplication(), logger)
        }
    }
    single {
        get<Jellyfin>().createApi(device = AndroidDevice.fromContext(androidApplication()))
    }
    single { PermissionRequestHelper() }
    single { WebappFunctionChannel() }
    single { RemoteVolumeProvider(get()) }
    single(named(PLAYER_EVENT_CHANNEL)) { Channel<PlayerEvent>() }

    // Fragments
    fragment { ConnectFragment() }
    fragment { WebViewFragment() }
    fragment { PlayerFragment() }
}

package org.jellyfin.mobile

import androidx.multidex.MultiDexApplication
import android.webkit.WebView
import org.jellyfin.mobile.model.databaseModule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.fragment.koin.fragmentFactory
import org.koin.core.context.startKoin
import timber.log.Timber

class JellyfinApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        androidx.appcompat.app.AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        if (BuildConfig.DEBUG) {
            // Setup logging
            Timber.plant(Timber.DebugTree())

            // Enable WebView debugging
            WebView.setWebContentsDebuggingEnabled(true)
        }

        startKoin {
            androidContext(this@JellyfinApplication)
            fragmentFactory()
            modules(applicationModule, databaseModule)
        }
    }
}

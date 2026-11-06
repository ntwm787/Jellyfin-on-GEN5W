package org.jellyfin.mobile.utils

import okhttp3.HttpUrl

/** OkHttp 3.12 keeps Android 4.4 support; retain the upstream call sites. */
fun String.toHttpUrlOrNull(): HttpUrl? = HttpUrl.parse(this)

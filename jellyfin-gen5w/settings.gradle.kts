include(":app", ":api-model", ":api-library", ":api-android", ":preferences")
project(":api-model").projectDir = file("third_party/apiclient/model")
project(":api-library").projectDir = file("third_party/apiclient/library")
project(":api-android").projectDir = file("third_party/apiclient/android")
project(":preferences").projectDir = file("third_party/preferences/library")
pluginManagement {
    resolutionStrategy { eachPlugin {
        if (requested.id.namespace?.startsWith("org.jetbrains.kotlin") == true) useVersion("1.4.10")
    } }
}

include(":koin-core")
project(":koin-core").projectDir = file("third_party/koin/koin-projects/koin-core")

include(":koin-android")
project(":koin-android").projectDir = file("third_party/koin/koin-projects/koin-android")

include(":koin-fragment")
project(":koin-fragment").projectDir = file("third_party/koin/koin-projects/koin-androidx-fragment")

include(":exo-common")
project(":exo-common").projectDir = file("third_party/exoplayer/library/common")

include(":exo-extractor")
project(":exo-extractor").projectDir = file("third_party/exoplayer/library/extractor")

include(":exo-core")
project(":exo-core").projectDir = file("third_party/exoplayer/library/core")

include(":exo-ui")
project(":exo-ui").projectDir = file("third_party/exoplayer/library/ui")

include(":exo-hls")
project(":exo-hls").projectDir = file("third_party/exoplayer/library/hls")

include(":exo-mediasession")
project(":exo-mediasession").projectDir = file("third_party/exoplayer/extensions/mediasession")

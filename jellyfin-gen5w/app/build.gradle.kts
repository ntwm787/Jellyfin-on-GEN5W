
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
}

android {
    compileSdkVersion(30)
    buildToolsVersion("29.0.3")
    defaultConfig {
        applicationId = "org.jellyfin.gen5w"
        minSdkVersion(19)
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
        targetSdkVersion(30)
        versionName = "2.1.1-gen5w.17"
        versionCode = 2010117
        setProperty("archivesBaseName", "jellyfin-android-v$versionName")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
                arguments["room.incremental"] = "true"
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            aaptOptions.cruncherEnabled = false // Disable png crunching
        }
    }

    flavorDimensions("variant")
    productFlavors {
        register("libre") {
            dimension = "variant"
            buildConfigField("boolean", "IS_PROPRIETARY", "false")
        }
        register("proprietary") {
            dimension = "variant"
            buildConfigField("boolean", "IS_PROPRIETARY", "true")
            isDefault = true
        }
    }

    @Suppress("UnstableApiUsage")
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    lintOptions {
        isAbortOnError = false
        disable("MissingTranslation", "ExtraTranslation")
    }
}


dependencies {
    implementation("androidx.multidex:multidex:2.0.1")
    // Add implementation functions for build flavors
    val libreImplementation by configurations
    val proprietaryImplementation by configurations

    // Kotlin
    implementation(kotlin("stdlib-jdk8"))
    implementation(Dependencies.Kotlin.coroutinesCore)
    implementation(Dependencies.Kotlin.coroutinesAndroid)

    // Core
    implementation(project(":koin-android"))
    implementation(Dependencies.Core.appCompat)
    implementation(Dependencies.Core.androidx)
    implementation(Dependencies.Core.activity)
    implementation(Dependencies.Core.fragment)
    implementation(project(":koin-fragment"))
    implementation(project(":exo-core"))

    // Lifecycle
    implementation(Dependencies.Lifecycle.viewModel)
    implementation(Dependencies.Lifecycle.liveData)
    implementation(Dependencies.Lifecycle.runtime)
    implementation(Dependencies.Lifecycle.common)
    implementation(Dependencies.Lifecycle.process)

    // UI
    implementation(Dependencies.UI.constraintLayout)
    implementation(Dependencies.UI.material)
    implementation(Dependencies.UI.webkitX)
    implementation(project(":exo-ui"))
    implementation(project(":preferences"))

    // Room
    implementation(Dependencies.Room.runtime)
    kapt(Dependencies.Room.compiler)

    // Network
    implementation(project(":api-android"))
    implementation(Dependencies.Network.okHttp)
    implementation(Dependencies.Network.coil)
    implementation(project(":exo-hls"))

    // Cast
    implementation(Dependencies.Cast.mediaRouter)
    proprietaryImplementation(Dependencies.Cast.playServicesCast)
    proprietaryImplementation(Dependencies.Cast.playServicesCastFramework)

    // Media
    implementation(Dependencies.Media.media)
    implementation(project(":exo-mediasession"))

    // Health
    implementation(Dependencies.Health.timber)

    // Testing
    testImplementation(Dependencies.Health.junit)
    testRuntimeOnly(Dependencies.Health.junitEngine)
    testImplementation(Dependencies.Health.kotestAssertions)
    testImplementation(Dependencies.Health.kotestProperty)
    testImplementation(Dependencies.Health.kotestRunner)
    testImplementation(Dependencies.Health.mockk)
    androidTestImplementation(Dependencies.Health.androidXRunner)
    androidTestImplementation(Dependencies.Health.androidXEspresso)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        outputs.upToDateWhen { false }
        showStandardStreams = true
    }
}


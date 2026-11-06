plugins { id("com.android.library"); kotlin("android"); kotlin("android.extensions") }
android {
    compileSdkVersion(30)
    buildToolsVersion("29.0.3")
    defaultConfig { minSdkVersion(19); targetSdkVersion(30); vectorDrawables.useSupportLibrary = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_1_8; targetCompatibility = JavaVersion.VERSION_1_8 }
    kotlinOptions { jvmTarget = "1.8" }
}
dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.core:core-ktx:1.3.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.0-beta6")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
}

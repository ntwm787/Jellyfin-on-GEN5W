plugins { id("com.android.library"); kotlin("android"); kotlin("android.extensions") }
android {
    compileSdkVersion(30)
    buildToolsVersion("29.0.3")
    defaultConfig { minSdkVersion(19); targetSdkVersion(30) }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_1_8; targetCompatibility = JavaVersion.VERSION_1_8 }
    kotlinOptions { jvmTarget = "1.8" }
}
dependencies {
    api(project(":api-library"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("androidx.core:core-ktx:1.3.0")
    implementation("androidx.annotation:annotation:1.1.0")
    implementation("com.android.volley:volley:1.2.1")
}

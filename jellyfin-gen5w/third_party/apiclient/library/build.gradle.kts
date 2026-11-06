plugins { kotlin("jvm") }
dependencies {
    api(project(":api-model"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    implementation("org.java-websocket:Java-WebSocket:1.4.1")
    implementation("com.google.code.gson:gson:2.8.6")
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }

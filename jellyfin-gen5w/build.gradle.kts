buildscript {
    repositories { google(); mavenCentral() }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.0")
        classpath(kotlin("gradle-plugin", "1.4.10"))
    }
}
allprojects { repositories { google(); mavenCentral() } }
tasks.register<Delete>("clean") { delete(rootProject.buildDir) }

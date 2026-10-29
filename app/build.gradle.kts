import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

/**
 * Optional local signing config kept out of the repo.
 * If the file is missing, the build still works but a release APK is unsigned.
 */
val signing = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val lastfmApiKey = (
    localProps.getProperty("LASTFM_API_KEY")
        ?: System.getenv("LASTFM_API_KEY")
        ?: ""
).trim()

val lastfmSecret = (
    localProps.getProperty("LASTFM_SECRET")
        ?: System.getenv("LASTFM_SECRET")
        ?: ""
).trim()

val listenTogetherServer = (
    localProps.getProperty("LISTEN_TOGETHER_SERVER")
        ?: System.getenv("LISTEN_TOGETHER_SERVER")
        ?: "https://bitchord-listen-together.onrender.com"
).trim().trimEnd('/')

val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0
val autoVersionName = if (runNumber > 0) "1.9.$runNumber" else "1.9.0"
val autoVersionCode = if (runNumber > 0) 10000 + runNumber else (System.currentTimeMillis() / 60000).toInt()

android {
    namespace = "com.music.aurelis"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.music.aurelis"
        minSdk = 26
        targetSdk = 35
        versionCode = autoVersionCode
        versionName = autoVersionName

        buildConfigField("String", "UPDATE_APK_PREFIX", "\"app-\"")
        buildConfigField("String", "LASTFM_API_KEY", "\"${lastfmApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "LASTFM_SECRET", "\"${lastfmSecret.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "LISTEN_TOGETHER_SERVER", "\"${listenTogetherServer.replace("\\", "\\\\").replace("\"", "\\\"")}\"")

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        val store = signing.getProperty("storeFile")?.let { rootProject.file(it) }
        if (store != null && store.exists()) {
            create("release") {
                storeFile = store
                storePassword = signing.getProperty("storePassword")
                keyAlias = signing.getProperty("keyAlias")
                keyPassword = signing.getProperty("keyPassword")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress.add("")
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            keepDebugSymbols.add("**/*.so")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.findByName("release")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")
    implementation("androidx.media3:media3-common:1.5.1")

    implementation("io.github.anilbeesetti:nextlib-media3ext:1.11.1-0.16.0")

    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("dev.chrisbanes.haze:haze:1.3.1")
    implementation("dev.chrisbanes.haze:haze-materials:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")

    implementation("com.google.guava:guava:33.3.1-android")
    implementation("org.checkerframework:checker-qual:3.48.3")
    implementation("org.jsoup:jsoup:1.18.1")
    compileOnly("org.jspecify:jspecify:1.0.0")
}

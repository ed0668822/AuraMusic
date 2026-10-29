import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// ---------------------------------------------------------------------------
// Lectura de configuración: local.properties > ENV (GitHub Secrets) > default
// ---------------------------------------------------------------------------

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val signingProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

/**
 * Devuelve el primer valor NO VACÍO entre:
 *   1. local.properties
 *   2. variable de entorno (GitHub Actions)
 *   3. default
 */
fun readValue(key: String, default: String = ""): String {
    val local = localProps.getProperty(key)?.trim()
    if (!local.isNullOrEmpty()) return local
    val env = System.getenv(key)?.trim()
    if (!env.isNullOrEmpty()) return env
    return default
}

/** Igual que readValue pero para firmado (keystore.properties o ENV). */
fun readSigning(key: String): String? {
    val local = signingProps.getProperty(key)?.trim()
    if (!local.isNullOrEmpty()) return local
    return System.getenv(key)?.trim()?.takeIf { it.isNotEmpty() }
}

/**
 * Escapa correctamente un String para usarlo como literal en BuildConfig.
 * Cubre backslash, comillas, saltos de línea, tab y CR.
 */
fun escapeForBuildConfig(value: String): String {
    val sb = StringBuilder("\"")
    for (ch in value) {
        when (ch) {
            '\\' -> sb.append("\\\\")
            '"'  -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> sb.append(ch)
        }
    }
    sb.append('"')
    return sb.toString()
}

// --- Variables de configuración ---

// ❌ Last.fm desactivado: no se leen secretos ni se valida nada en CI.
//    Si algún día lo reactivas, vuelve a leer LASTFM_API_KEY / LASTFM_SECRET aquí
//    y añade de nuevo los check(...) para CI.
val lastfmApiKey = ""
val lastfmSecret = ""

val listenTogetherServer = readValue(
    "LISTEN_TOGETHER_SERVER",
    "https://bitchord-listen-together.onrender.com"
).trimEnd('/')

// --- Versionado automático ---

val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0
val autoVersionName = if (runNumber > 0) "1.9.$runNumber" else "1.9.0"
val autoVersionCode = if (runNumber > 0) {
    10000 + runNumber
} else {
    (System.currentTimeMillis() / 60_000L).toInt()
}

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
        buildConfigField("String", "LASTFM_API_KEY", "\"\"")
        buildConfigField("String", "LASTFM_SECRET", "\"\"")
        buildConfigField("String", "LISTEN_TOGETHER_SERVER", escapeForBuildConfig(listenTogetherServer))

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
        val storePath      = readSigning("storeFile")      ?: readSigning("STORE_FILE")
        val storePassword  = readSigning("storePassword")  ?: readSigning("STORE_PASSWORD")
        val keyAlias       = readSigning("keyAlias")       ?: readSigning("KEY_ALIAS")
        val keyPassword    = readSigning("keyPassword")    ?: readSigning("KEY_PASSWORD")
        val store          = storePath?.let { rootProject.file(it) }

        if (store != null && store.exists() &&
            !storePassword.isNullOrEmpty() &&
            !keyAlias.isNullOrEmpty() &&
            !keyPassword.isNullOrEmpty()
        ) {
            create("release") {
                storeFile     = store
                this.storePassword = storePassword
                this.keyAlias      = keyAlias
                this.keyPassword   = keyPassword
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

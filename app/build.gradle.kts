plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0" 
}

// LÓGICA DE VERSIÓN AUTOMÁTICA
// GitHub Actions provee un número de ejecución que incrementa en 1 con cada push.
val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0
// Si estamos en GitHub, usa 1.3.[número]. Si estamos en local, usa 1.3.0
val autoVersionName = if (runNumber > 0) "1.3.$runNumber" else "1.3.0"
// VersionCode usa el número de ejecución. Para compilaciones locales usa la fecha.
val autoVersionCode = if (runNumber > 0) {
    10000 + runNumber
} else {
    (System.currentTimeMillis() / 60000).toInt()
}

android {
    namespace = "com.music.aura"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.music.aura"
        minSdk = 26
        targetSdk = 35
        
        // Aplicamos las variables automáticas
        versionCode = autoVersionCode
        versionName = autoVersionName
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    buildFeatures {
        compose = true
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
    implementation("androidx.compose.foundation:foundation:1.10.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")
    implementation("androidx.media3:media3-common:1.5.1")

    implementation("io.coil-kt.coil3:coil-compose:3.0.4")

    implementation("dev.chrisbanes.haze:haze:1.3.1")
    implementation("dev.chrisbanes.haze:haze-materials:1.3.1")

    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("junit:junit:4.13.2")
}

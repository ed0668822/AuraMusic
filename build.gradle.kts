plugins {
    // ✅ Subimos de 8.9.0 a 8.10.0 para que soporte oficialmente el compileSdk = 36
    id("com.android.application") version "8.10.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10" apply false
}

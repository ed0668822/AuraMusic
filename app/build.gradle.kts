// ... (resto del archivo sin cambios)
signingConfigs {
    create("release") {
        val keystorePath = System.getenv("KEYSTORE_PATH") ?: "aura-release.jks"
        // ✅ CORREGIDO: usar project.rootProject para acceder al rootProject
        val storeFile = project.rootProject.file(keystorePath)
        
        if (storeFile.exists()) {
            this.storeFile = storeFile
            this.storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "aura12345"
            this.keyAlias = System.getenv("KEY_ALIAS") ?: "aura"
            this.keyPassword = System.getenv("KEYSTORE_PASSWORD") ?: "aura12345"
        }
    }
}
// ... (resto del archivo sin cambios)

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    // ❌ ELIMINADA: implementation("androidx.compose.foundation:foundation:1.10.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    // ... (resto de dependencias)
}

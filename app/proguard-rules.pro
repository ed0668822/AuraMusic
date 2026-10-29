# Aurelis © 2026. GPL-3.0-or-later.

# ---------------------------------------------------------
# 1. Conservar TODAS las clases de la app (Modo estricto)
# ---------------------------------------------------------
-keep class com.music.aurelis.** { *; }
-keep interface com.music.aurelis.** { *; }
-keep enum com.music.aurelis.** { *; }

# 2. Conservar métodos nativos (JNI)
-keepclasseswithmembernames class * {
    native <methods>;
}

# 3. Conservar clases de serialización (kotlinx.serialization)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 4. Conservar modelos de datos (data classes) usados en JSON
-keep class com.music.aurelis.data.lyrics.** { *; }
-keep class com.music.aurelis.data.model.** { *; }

# 5. Conservar librerías de terceros que usan reflexión
-keep class org.jsoup.** { *; }
-keep class okhttp3.** { *; }
-keep class androidx.media3.** { *; }
-keep class com.microsoft.onnxruntime.** { *; }

# 6. Evitar que R8 elimine metadatos de depuración (opcional pero útil)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 7. Reglas específicas para las librerías nativas y FFmpeg vendorizado
-keep class androidx.media3.decoder.ffmpeg.** { *; }

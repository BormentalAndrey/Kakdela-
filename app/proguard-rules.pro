# Сохранить в app/proguard-rules.pro

# ---------------------------------------------------------------------------
# Gson
# ---------------------------------------------------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.vasilisina.azbuka.data.ProgressManager$SavedProgress { *; }

# ---------------------------------------------------------------------------
# Kotlin
# ---------------------------------------------------------------------------
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }

# ---------------------------------------------------------------------------
# Compose
# ---------------------------------------------------------------------------
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ---------------------------------------------------------------------------
# Сохранение R-классов для рефлексивного доступа
# ---------------------------------------------------------------------------
-keepclassmembers class com.vasilisina.azbuka.R$drawable {
    public static <fields>;
}

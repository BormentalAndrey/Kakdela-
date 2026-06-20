# Сохранить в app/proguard-rules.pro

# ---------------------------------------------------------------------------
# Gson — сохраняем generic-сигнатуры и классы данных
# ---------------------------------------------------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# Сам Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }

# Наш SavedProgress и все его поля
-keep class com.vasilisina.azbuka.data.ProgressManager$SavedProgress { *; }
-keepclassmembers class com.vasilisina.azbuka.data.ProgressManager$SavedProgress { <fields>; }

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

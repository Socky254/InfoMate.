# --- GSON & TYPE TOKEN OPTIMIZATION ---

# 1. Preserve Generic Signatures (Critical for TypeToken)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# 2. Preserve Gson model classes
-keep class com.infomate.app.ui.ChatMessage { *; }
-keep class com.infomate.app.ui.QuotaInfo { *; }
-keep class com.infomate.app.core.UpdateInfo { *; }
-keep class com.infomate.core.brain.ThoughtStep { *; }

# 3. Prevent Gson from obfuscating its own internals
-keep class com.google.gson.** { *; }

# 4. Handle TypeToken properly
# Keep all anonymous subclasses of TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keepnames class * extends com.google.gson.reflect.TypeToken

# 5. Prevent R8 from removing methods used by reflection
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 6. Preserve Line Numbers for Crash Logs
-keepattributes SourceFile,LineNumberTable

# --- GOOGLE AI / MEDIAPIPE / AUTOVALUE FIXES ---
-dontwarn javax.lang.model.**
-dontwarn com.google.auto.value.**
-dontwarn autovalue.shaded.**
-dontwarn com.google.common.collect.Immutable*

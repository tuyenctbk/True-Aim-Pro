# ProGuard / R8 rules for TrueAim Pro

# Preserve Firestore document models and @Keep annotated classes
-keep @androidx.annotation.Keep class * { *; }
-keep class com.example.data.ai.UsageQuotaDoc { *; }

# Preserve Room entities and DAOs
-keep class com.example.data.room.** { *; }

# Preserve CrosshairStyle enum values used in Room & JSON serialization
-keepclassmembers enum com.example.model.CrosshairStyle {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve line numbers for clean crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

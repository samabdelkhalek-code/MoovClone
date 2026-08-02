# Preserve Kotlin metadata
-keepattributes *Annotation*,Signature,EnclosingMethod
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Kotlin specific rules
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
  public <methods>;
}

# Keep Compose related classes
-keep class androidx.compose.** { *; }
-keepclasseswithmembernames class androidx.compose.** { *; }

# Keep our app classes
-keep class com.moovclone.app.** { *; }
-keepclassmembers class com.moovclone.app.** { *; }

# Keep enums
-keepclassmembers enum * {
  public static **[] values();
  public static ** valueOf(java.lang.String);
}

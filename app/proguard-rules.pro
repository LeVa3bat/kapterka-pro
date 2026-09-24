# Kapterka PRO — R8 rules for release builds.

# Readable crash stack traces (file names hidden, line numbers kept).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room entities / enums are mapped by generated code; enums are also stored by
# name, so their constant names must stay stable.
-keepclassmembers enum com.example.data.model.** { *; }

# Firestore snapshots are read field-by-field (no reflection mapping), but keep
# model classes intact in case a toObject() mapping is added later.
-keep class com.example.data.model.** { <init>(...); <fields>; }

# Unused SDKs pulled in transitively must not fail the build.
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Firebase / Firestore transport -------------------------------------------
# Firestore talks over gRPC, whose transport is discovered at runtime through
# java.util.ServiceLoader. R8 full mode can strip or rename those providers,
# which silently disables all cloud sync. Keep the transport stack intact.
-keep class io.grpc.** { *; }
-keepnames class io.grpc.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firestore.** { *; }
-keep class com.google.protobuf.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }
-keep class com.google.firebase.auth.** { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-dontwarn io.grpc.**
-dontwarn com.google.protobuf.**
-dontwarn com.squareup.okhttp.**

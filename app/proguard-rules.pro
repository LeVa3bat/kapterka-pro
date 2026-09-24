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

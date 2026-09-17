# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# WorkManager (WeatherRefreshWorker) keeps its job state in a Room database.
# Its generated implementation, WorkDatabase_Impl, is instantiated by
# reflection through a no-arg constructor. R8 full mode (AGP default) strips
# that constructor as unreachable, so InitializationProvider crashes at startup
# with NoSuchMethodException: androidx.work.impl.WorkDatabase_Impl.<init> [].
# Keep the constructor of every Room database implementation.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { <init>(); }
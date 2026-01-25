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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# ===== Firebase =====
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Firebase Auth models
-keepclassmembers class com.google.firebase.auth.** {
    *;
}

# ===== Room Database =====
# Keep Room entities
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# Keep Room DAOs
-keep @androidx.room.Dao class *
-keepclassmembers @androidx.room.Dao class * { *; }

# Keep Room Database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Database class *
-dontwarn androidx.room.paging.**

# ===== Hilt Dependency Injection =====
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModelFactory { *; }

# Keep Hilt modules and components
-keep @dagger.hilt.InstallIn class *
-keep @dagger.Module class *
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# ===== Jetpack Compose =====
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep all Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# ===== Google Play Services Auth =====
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keepclassmembers class com.google.android.gms.auth.** { *; }

# ===== Kotlin =====
# Keep Kotlin Metadata for reflection
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Keep data classes
-keepclassmembers class * {
    public <init>(...);
}

# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ===== DataStore =====
-keep class androidx.datastore.*.** { *; }

# ===== WorkManager =====
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# ===== Application-specific =====
# Keep all data models (entities, domain models)
-keep class com.fintrack.app.core.data.local.model.** { *; }
-keep class com.fintrack.app.core.domain.model.** { *; }
-keep class com.fintrack.app.data.local.model.** { *; }
-keep class com.fintrack.app.core.data.local.entity.** { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===== Vico Charts =====
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# ===== Coil Image Loading =====
-keep class coil.** { *; }
-dontwarn coil.**

# ===== General Android =====
-keep class * extends android.app.Activity
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider

# Keep all View constructors
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ===== Remove Logging in Release Builds =====
# Strip all debug and verbose logs from android.util.Log
# This removes PII exposure risk and reduces APK size
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Strip debug and verbose logs from our AppLogger
# Error and warning logs are preserved for production diagnostics
-assumenosideeffects class com.fintrack.app.core.util.AppLogger {
    public static *** d(...);
    public static *** v(...);
}

# ===== Optimization Warnings Suppression =====
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
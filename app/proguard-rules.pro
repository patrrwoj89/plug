# ProGuard rules for TV Hub Skeleton

# Keep serializable/parcelable models
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltApplication { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.SerialName <methods>;
}
-keep @kotlinx.serialization.Serializable class * { *; }

# Retrofit / OkHttp / Reflective models
-keep class com.polishmediahub.app.data.remote.** { *; }
-keep class com.polishmediahub.app.data.local.** { *; }
-keep class com.polishmediahub.app.data.remote.health.** { *; }
-keep class com.polishmediahub.app.data.remote.cache.** { *; }
-keep class com.polishmediahub.app.model.** { *; }
-keep class retrofit2.** { *; }
-keepclassmembers class retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okio.**
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers @androidx.room.Entity class * {
    <init>(...);
    @androidx.room.PrimaryKey <fields>;
    @androidx.room.ColumnInfo <fields>;
}
-dontwarn androidx.room.paging.**

# Compose / Coil
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class coil.** { *; }
-dontwarn coil.**

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# JSoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite* { <fields>; }

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Keep application class
-keep class com.polishmediahub.app.TVHubApplication { *; }

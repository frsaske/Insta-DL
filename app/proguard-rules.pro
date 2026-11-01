# Add project specific ProGuard rules here.

# Keep Room Database entities and DAOs
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# Keep Chaquopy and Python bindings
-keep class com.chaquo.python.** { *; }

# Keep OkHttp & JSON models
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-dontwarn okhttp3.**
-dontwarn okio.**

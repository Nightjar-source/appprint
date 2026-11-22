# R8 full mode is on; this file intentionally stays minimal.
# Compose Multiplatform ships optimized rules; Arabic text shaping is runtime, not field name-based.
-keepattributes SourceFile,LineNumberTable

# --------------------------------------------------------------------------------------
# AdMob pulls in androidx.work (WorkManager). Its Room database is created by reflection
# (Room.getGeneratedImplementation -> Class.forName("androidx.work.impl.WorkDatabase_Impl")),
# and WorkManager's own consumer rules do NOT keep the generated Room implementation.
# With R8 full mode the class is stripped, so the app crashes on startup with
# "Failed to create an instance of androidx.work.impl.WorkDatabase" via
# androidx.startup.InitializationProvider. Keep the generated class + its constructor:
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keepclassmembers class androidx.work.impl.WorkDatabase_Impl { public <init>(...); }
-keepnames class * extends androidx.work.impl.WorkDatabase
-keep class androidx.room.RoomDatabase { *; }
-keepnames class * extends androidx.room.RoomDatabase

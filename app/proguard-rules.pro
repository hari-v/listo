# Listo release build rules (R8).

# Glance creates widget button handlers (ActionCallback) from their class names.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }

# WorkManager creates the daily worker from its class name.
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Room finds the generated database class by name.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Keep readable stack traces for bug reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

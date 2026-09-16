package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.util.TacticalNotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class KapterkaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Global crash guard for emulators and low-end hardware
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("KapterkaApp", "Caught unhandled exception on thread ${thread.name}: ${throwable.message}", throwable)
            // Log details but don't rethrow to avoid hard crash if possible
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Safe Firebase initialization
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:946233715306:android:d2502913c49c0b985c7813")
                    .setApiKey("AIzaSyAYyoG42TuQJFLxN0KnFIePZx-gAtizw0Q")
                    .setProjectId("kapterka-pro")
                    .setDatabaseUrl("https://kapterka-pro-default-rtdb.europe-west1.firebasedatabase.app")
                    .setStorageBucket("kapterka-pro.firebasestorage.app")
                    .setGcmSenderId("946233715306")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("KapterkaApp", "Firebase initialized successfully")
            }
        } catch (e: Throwable) {
            Log.w("KapterkaApp", "Firebase init warning: ${e.message}")
        }

        // Safe notification channel setup
        try {
            TacticalNotificationHelper.createNotificationChannel(this)
        } catch (e: Throwable) {
            Log.w("KapterkaApp", "Notification channel skipped: ${e.message}")
        }
    }
}

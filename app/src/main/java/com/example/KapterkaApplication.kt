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

        // Firebase projects are intentionally isolated by product.
        if (BuildConfig.IS_UNIVERSAL_APP) {
            try {
                val app = if (FirebaseApp.getApps(this).isEmpty()) {
                    FirebaseApp.initializeApp(this)
                } else {
                    FirebaseApp.getInstance()
                }
                if (app == null) {
                    Log.e("SkladPro", "Dedicated Firebase configuration is missing")
                } else if (app.options.projectId != "sklad-pro-a1ec0") {
                    Log.e("SkladPro", "Refusing unexpected Firebase project: ${app.options.projectId}")
                } else {
                    Log.i("SkladPro", "Dedicated Firebase initialized")
                }
            } catch (e: Throwable) {
                Log.e("SkladPro", "Firebase init error: ${e.message}", e)
            }
        } else if (!BuildConfig.IS_NEXT_SAFE_TEST) {
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
        } else {
            Log.i("KapterkaApp", "NEXT-SAFE build: production Firebase disabled")
        }

        // Tactical notifications belong only to the legacy product.
        if (!BuildConfig.IS_UNIVERSAL_APP) {
            try {
                TacticalNotificationHelper.createNotificationChannel(this)
            } catch (e: Throwable) {
                Log.w("KapterkaApp", "Notification channel skipped: ${e.message}")
            }
        }
    }
}

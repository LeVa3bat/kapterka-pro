package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class KapterkaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(
                "SkladPro",
                "Caught unhandled exception on thread ${thread.name}: ${throwable.message}",
                throwable
            )
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // universal/alpha is a standalone Sklad PRO product. It must initialize only
        // the dedicated Firebase project supplied by app/google-services.json.
        try {
            val app = if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            } else {
                FirebaseApp.getInstance()
            }

            when {
                app == null ->
                    Log.e("SkladPro", "Dedicated Firebase configuration is missing")
                app.options.projectId != "sklad-pro-a1ec0" ->
                    Log.e(
                        "SkladPro",
                        "Refusing unexpected Firebase project: ${app.options.projectId}"
                    )
                else ->
                    Log.i("SkladPro", "Dedicated Firebase initialized")
            }
        } catch (e: Throwable) {
            Log.e("SkladPro", "Firebase init error: ${e.message}", e)
        }
    }
}

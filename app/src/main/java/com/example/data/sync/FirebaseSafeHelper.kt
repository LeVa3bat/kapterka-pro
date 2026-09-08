package com.example.data.sync

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseSafeHelper {
    private const val TAG = "FirebaseSafeHelper"
    @Volatile
    private var initialized = false

    fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                synchronized(this) {
                    if (FirebaseApp.getApps(context).isEmpty()) {
                        val options = FirebaseOptions.Builder()
                            .setApplicationId("com.aistudio.kapterka.jmwqve")
                            .setProjectId("kapterka-pro-tactical")
                            .setApiKey("AIzaSyB-kapterka-tactical-safekey")
                            .build()
                        FirebaseApp.initializeApp(context.applicationContext, options)
                    }
                }
            }
            FirebaseFirestore.getInstance()
        } catch (t: Throwable) {
            Log.w(TAG, "Firestore is unavailable or not configured: ${t.message}")
            null
        }
    }
}

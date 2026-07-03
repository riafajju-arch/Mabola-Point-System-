package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    
    var isFirebaseEnabled: Boolean = false
        private set
        
    var auth: FirebaseAuth? = null
        private set
        
    var db: FirebaseFirestore? = null
        private set

    fun initialize(context: Context) {
        try {
            val apiKey = BuildConfig.FIREBASE_API_KEY
            val projectId = BuildConfig.FIREBASE_PROJECT_ID
            val appId = BuildConfig.FIREBASE_APP_ID

            if (apiKey.isEmpty() || apiKey.startsWith("your_") ||
                projectId.isEmpty() || projectId.startsWith("your_") ||
                appId.isEmpty() || appId.startsWith("your_")
            ) {
                Log.w(TAG, "Firebase credentials are not configured or are placeholders. Falling back to Local SQLite mode.")
                isFirebaseEnabled = false
                return
            }

            // Check if already initialized to avoid crash
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setProjectId(projectId)
                    .setApplicationId(appId)
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                FirebaseApp.getInstance()
            }

            auth = FirebaseAuth.getInstance(app)
            db = FirebaseFirestore.getInstance(app)
            isFirebaseEnabled = true
            Log.i(TAG, "Firebase successfully initialized programmatically!")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase programmatically: ${e.message}", e)
            isFirebaseEnabled = false
        }
    }
}

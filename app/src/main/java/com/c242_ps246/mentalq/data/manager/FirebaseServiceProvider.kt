package com.c242_ps246.mentalq.data.manager

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseServiceProvider @Inject constructor() {
    fun auth(): FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    fun database(): FirebaseDatabase? = runCatching { FirebaseDatabase.getInstance() }.getOrNull()

    companion object {
        const val CONFIGURATION_ERROR =
            "Firebase is not configured. Add app/google-services.json to enable Google sign-in and chat."
    }
}

package com.vaultpulse.app

import android.app.Application
import android.util.Log
import com.vaultpulse.app.data.local.VaultDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VaultPulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("VaultPulse", "VaultPulse Application started.")

        // Warm up SQLCipher single connection in background to avoid lock contention on startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                VaultDatabase.getInstance(this@VaultPulseApp).accountDao().getAll()
                Log.i("VaultPulse", "Encrypted database warmed up successfully.")
            } catch (e: Exception) {
                Log.w("VaultPulse", "Database warmup: ${e.message}")
            }
        }
    }
}

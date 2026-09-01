package com.rank.tempbox

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object Integrity {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            "tempbox_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun p(): SharedPreferences =
        prefs ?: throw IllegalStateException("Integrity.init() must be called before use")

    fun putInt(key: String, value: Int) { p().edit().putInt(key, value).apply() }
    fun getInt(key: String, default: Int): Int = p().getInt(key, default)

    fun putLong(key: String, value: Long) { p().edit().putLong(key, value).apply() }
    fun getLong(key: String, default: Long): Long = p().getLong(key, default)

    fun putBoolean(key: String, value: Boolean) { p().edit().putBoolean(key, value).apply() }
    fun getBoolean(key: String, default: Boolean): Boolean = p().getBoolean(key, default)

    fun putString(key: String, value: String) { p().edit().putString(key, value).apply() }
    fun getString(key: String, default: String?): String? = p().getString(key, default)
}

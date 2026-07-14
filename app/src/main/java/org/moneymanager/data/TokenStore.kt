package org.moneymanager.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface SessionStore {
    fun getToken(): String?
    fun getEmail(): String
    fun saveSession(token: String, email: String)
    fun clearToken()
    fun getPushDeviceID(): Int?
    fun savePushDeviceID(id: Int)
    fun clearPushDeviceID()
}

class TokenStore(context: Context) : SessionStore {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "money_manager_secure",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun getToken(): String? = preferences.getString(KEY_TOKEN, null)

    override fun getEmail(): String = preferences.getString(KEY_EMAIL, "").orEmpty()

    override fun saveSession(token: String, email: String) {
        preferences.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    override fun clearToken() {
        preferences.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EMAIL)
            .apply()
    }

    override fun getPushDeviceID(): Int? = preferences.getInt(KEY_PUSH_DEVICE_ID, 0).takeIf { it > 0 }

    override fun savePushDeviceID(id: Int) {
        preferences.edit().putInt(KEY_PUSH_DEVICE_ID, id).apply()
    }

    override fun clearPushDeviceID() {
        preferences.edit().remove(KEY_PUSH_DEVICE_ID).apply()
    }

    private companion object {
        const val KEY_TOKEN = "jwt"
        const val KEY_EMAIL = "email"
        const val KEY_PUSH_DEVICE_ID = "push_device_id"
    }
}

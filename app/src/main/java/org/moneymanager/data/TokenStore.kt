package org.moneymanager.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "money_manager_secure",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getToken(): String? = preferences.getString(KEY_TOKEN, null)

    fun getEmail(): String = preferences.getString(KEY_EMAIL, "").orEmpty()

    fun saveSession(token: String, email: String) {
        preferences.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun clearToken() {
        preferences.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EMAIL)
            .apply()
    }

    private companion object {
        const val KEY_TOKEN = "jwt"
        const val KEY_EMAIL = "email"
    }
}

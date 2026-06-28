package com.medifind.app.data.repository

import android.content.Context

class TokenManager(context: Context) {
    private val prefs = context.getSharedPreferences("medifind_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? = prefs.getString("jwt_token", null)

    fun saveUserInfo(name: String, email: String, role: String) {
        prefs.edit()
            .putString("user_name", name)
            .putString("user_email", email)
            .putString("user_role", role)
            .apply()
    }

    fun getUserName(): String? = prefs.getString("user_name", null)
    fun getUserEmail(): String? = prefs.getString("user_email", null)
    fun getUserRole(): String? = prefs.getString("user_role", null)

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
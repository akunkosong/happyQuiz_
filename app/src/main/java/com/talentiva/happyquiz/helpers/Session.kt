package com.talentiva.happyquiz.helpers


import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    private const val PREF_NAME = "user_pref"
    private const val KEY_UUID = "uuid"
    private const val KEY_USERNAME = "namauser"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveUser(context: Context, uuid: String, namauser: String) {
        val editor = getPrefs(context).edit()
        editor.putString(KEY_UUID, uuid)
        editor.putString(KEY_USERNAME, namauser)
        editor.apply()
    }

    fun getUuid(context: Context): String? {
        return getPrefs(context).getString(KEY_UUID, null)
    }

    fun getUsername(context: Context): String? {
        return getPrefs(context).getString(KEY_USERNAME, null)
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}

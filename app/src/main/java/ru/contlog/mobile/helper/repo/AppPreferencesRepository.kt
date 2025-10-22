package ru.contlog.mobile.helper.repo

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kotlinx.serialization.json.Json
import ru.contlog.mobile.helper.model.ApiAuthData

class AppPreferencesRepository(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun saveString(key: String, value: String) {
        sharedPreferences.edit {
            putString(key, value).apply()
        }
    }

    fun getString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun remove(key: String) {
        sharedPreferences.edit {
            remove(key).apply()
        }
    }

    fun saveApiAuthData(key: String, value: ApiAuthData?) {
        if (value == null) {
            sharedPreferences.edit {
                remove(key)
            }
            return
        }

        val strValue = jsonCoder.encodeToString<ApiAuthData>(value)
        saveString(key, strValue)
    }

    fun getApiAuthData(key: String): ApiAuthData? {
        val strValue = getString(key, "")
        if (strValue.isEmpty()) {
            return null
        }

        try {
            val value = jsonCoder.decodeFromString<ApiAuthData>(strValue)
            return value
        } catch (e: Exception) {
            Log.e(TAG, "getApiAuthData: error decoding user data", e)
        }

        return null
    }

    companion object {
        const val TAG = "AppPreferencesRepository"

        private val jsonCoder = Json {
            ignoreUnknownKeys = true
        }
    }
}
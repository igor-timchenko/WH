// Пакет репозитория для работы с локальными настройками приложения
package ru.contlog.mobile.helper.repo

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit // Позволяет использовать DSL-синтаксис для SharedPreferences
import kotlinx.serialization.json.Json
import ru.contlog.mobile.helper.model.ApiAuthData

/**
 * Репозиторий для сохранения и загрузки данных в локальное хранилище (SharedPreferences).
 *
 * Основное назначение:
 *   - хранение данных авторизации (ApiAuthData) между сессиями,
 *   - сохранение других строковых настроек (например, номера телефона).
 *
 * Использует:
 *   - SharedPreferences — стандартное локальное хранилище Android,
 *   - kotlinx.serialization — для сериализации/десериализации сложных объектов (ApiAuthData).
 */
class AppPreferencesRepository(private val context: Context) {

    // Имя файла настроек: "app_prefs.xml"
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    /**
     * Сохраняет строковое значение по ключу.
     */
    fun saveString(key: String, value: String) {
        sharedPreferences.edit {
            putString(key, value)
            apply() // Асинхронная запись (предпочтительно для UI-потока)
        }
    }

    /**
     * Получает строковое значение по ключу.
     * Если значение отсутствует — возвращает defaultValue.
     */
    fun getString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    /**
     * Удаляет значение по ключу.
     */
    fun remove(key: String) {
        sharedPreferences.edit {
            remove(key)
            apply()
        }
    }

    /**
     * Сохраняет объект ApiAuthData (данные авторизации) в SharedPreferences.
     * Если значение null — удаляет запись.
     */
    fun saveApiAuthData(key: String, value: ApiAuthData?) {
        if (value == null) {
            sharedPreferences.edit {
                remove(key)
                apply()
            }
            return
        }

        // Сериализуем объект в JSON-строку
        val strValue = jsonCoder.encodeToString<ApiAuthData>(value)
        saveString(key, strValue)
    }

    /**
     * Загружает объект ApiAuthData из SharedPreferences.
     * Возвращает null, если:
     *   - запись отсутствует,
     *   - строка пустая,
     *   - произошла ошибка десериализации.
     */
    fun getApiAuthData(key: String): ApiAuthData? {
        val strValue = getString(key, "")
        if (strValue.isEmpty()) {
            return null
        }

        try {
            val value = jsonCoder.decodeFromString<ApiAuthData>(strValue)
            return value
        } catch (e: Exception) {
            // Логируем ошибку (например, если структура ApiAuthData изменилась после обновления)
            Log.e(TAG, "getApiAuthData: error decoding user data", e)
        }

        return null
    }

    companion object {
        const val TAG = "AppPreferencesRepository"

        // Настройка JSON-парсера: игнорировать неизвестные поля
        // Это важно для обратной совместимости при обновлении модели ApiAuthData
        private val jsonCoder = Json {
            ignoreUnknownKeys = true
        }
    }
}
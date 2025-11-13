// Указание пакета, в котором расположен репозиторий для работы с настройками приложения.
package ru.contlog.mobile.helper.repo

// Импорт Context — основной компонент Android для доступа к системным ресурсам.
import android.content.Context
// Импорт SharedPreferences — механизм хранения простых данных в формате "ключ-значение".
import android.content.SharedPreferences
// Импорт Log для логирования ошибок при десериализации.
import android.util.Log
// Расширение edit {} для удобной и безопасной записи в SharedPreferences (из androidx.core).
import androidx.core.content.edit
// Импорт Json для сериализации/десериализации объектов в JSON и обратно.
import kotlinx.serialization.json.Json
// Модель данных авторизации, которую нужно сохранять/загружать.
import ru.contlog.mobile.helper.model.ApiAuthData
import ru.contlog.mobile.helper.model.Division

// Класс для работы с локальным хранилищем (SharedPreferences) приложения.
// Инкапсулирует операции сохранения и чтения настроек, включая сложные объекты (например, ApiAuthData).
class AppPreferencesRepository(private val context: Context) {
    // Получение экземпляра SharedPreferences с именем "app_prefs" и приватным режимом доступа
    // (доступен только этому приложению).
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Сохранение строки по ключу.
    fun saveString(key: String, value: String) {
        // Использование безопасного DSL-блока edit {} из androidx.core.
        // apply() — асинхронная запись (рекомендуется для UI-потока).
        sharedPreferences.edit {
            putString(key, value).apply()
        }
    }

    // Получение строки по ключу с указанием значения по умолчанию.
    fun getString(key: String, defaultValue: String): String {
        // getString() может вернуть null, если ключ не найден.
        // Оператор elvis (?:) гарантирует возврат defaultValue в этом случае.
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    // Удаление значения по ключу.
    fun remove(key: String) {
        sharedPreferences.edit {
            remove(key).apply()
        }
    }

    // Сохранение объекта ApiAuthData в SharedPreferences как JSON-строку.
    fun saveApiAuthData(key: String, value: ApiAuthData?) {
        // Если значение null — удаляем запись (очистка при выходе из аккаунта).
        if (value == null) {
            sharedPreferences.edit {
                remove(key)
            }
            return
        }

        // Сериализация объекта в JSON-строку.
        val strValue = jsonCoder.encodeToString<ApiAuthData>(value)
        // Сохранение строки через уже существующий метод.
        saveString(key, strValue)
    }

    // Загрузка объекта ApiAuthData из SharedPreferences.
    fun getApiAuthData(key: String): ApiAuthData? {
        // Получение JSON-строки. Если ключ отсутствует — вернётся пустая строка.
        val strValue = getString(key, "")
        // Если строка пуста — данных нет.
        if (strValue.isEmpty()) {
            return null
        }

        try {
            // Десериализация JSON-строки в объект ApiAuthData.
            val value = jsonCoder.decodeFromString<ApiAuthData>(strValue)
            return value
        } catch (e: Exception) {
            // Логирование ошибки (например, если структура JSON изменилась после обновления).
            Log.e(TAG, "getApiAuthData: error decoding user data", e)
        }

        // В случае ошибки — возвращаем null (безопасное поведение).
        return null
    }

    fun saveDivisionsList(key: String, value: List<Division>?) {
        if (value == null) {
            sharedPreferences.edit {
                remove(key)
            }
            return
        }

        val strValue = jsonCoder.encodeToString<List<Division>>(value)
        saveString(key, strValue)
    }

    fun getDivisionsList(key: String): List<Division>? {
        val strValue = getString(key, "")
        if (strValue.isEmpty()) {
            return null
        }

        try {
            val value = jsonCoder.decodeFromString<List<Division>>(strValue)
            return value
        } catch (e: Exception) {
            Log.e(TAG, "getDivisionsList: error decoding divisions list", e)
        }

        return null
    }

    companion object {
        // Тег для логирования.
        const val TAG = "AppPreferencesRepository"

        // Конфигурация JSON-парсера: игнорировать неизвестные поля
        // (защита от крашей при изменении модели на сервере).
        private val jsonCoder = Json {
            ignoreUnknownKeys = true
        }
    }
}
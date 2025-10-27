// Пакет, в котором находится модель данных для авторизации
package ru.contlog.mobile.helper.model

// Импорты для сериализации с помощью kotlinx.serialization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель данных, представляющая собой ответ сервера при успешной аутентификации по SMS.
 *
 * Используется для хранения учётных данных, необходимых для последующих запросов к API:
 * - уникального идентификатора пользователя (uid),
 * - ключа доступа (api_key).
 *
 * Аннотация @Serializable позволяет автоматически преобразовывать JSON ↔ объект Kotlin
 * с помощью библиотеки kotlinx.serialization.
 */
@Serializable
data class ApiAuthData(
    /**
     * Уникальный идентификатор пользователя на сервере.
     * В JSON-ответе сервера поле называется "fl_uid", но в коде Kotlin мы используем имя "uid".
     */
    @SerialName("fl_uid") val uid: String,

    /**
     * Ключ API, выдаваемый сервером после успешной аутентификации.
     * Используется для авторизации последующих запросов (например, в заголовке Authorization).
     * В JSON поле называется "api_key".
     */
    @SerialName("api_key") val apiKey: String,
)
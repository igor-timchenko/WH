// Пакет, в котором расположена модель данных для передачи параметров проверки SMS-кода
package ru.contlog.mobile.helper.model

// Импорты для поддержки сериализации через kotlinx.serialization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель данных, представляющая параметры запроса для проверки SMS-кода на сервере.
 *
 * Используется при вызове метода аутентификации, где клиент отправляет:
 * - номер телефона,
 * - полученный по SMS PIN-код.
 *
 * Аннотация @Serializable позволяет автоматически преобразовать этот объект в JSON
 * при отправке HTTP-запроса (например, с помощью Ktor или Retrofit с плагином kotlinx.serialization).
 */
@Serializable
data class AuthCheckSmsParams(
    /**
     * Номер телефона пользователя в формате, ожидаемом сервером (например, "79123456789").
     * В JSON будет сериализован как поле "tel".
     */
    val tel: String,

    /**
     * PIN-код, полученный пользователем по SMS.
     * На сервере это поле ожидается под именем "sms_pin", поэтому используется @SerialName.
     * Без аннотации Kotlin-поле `smsPin` было бы сериализовано как "smsPin",
     * но сервер ждёт именно "sms_pin" (snake_case).
     */
    @SerialName("sms_pin") val smsPin: String,
)

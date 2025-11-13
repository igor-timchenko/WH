package ru.contlog.mobile.helper.model


// Импортируем аннотацию SerialName из kotlinx.serialization,
// чтобы задать имя поля в JSON, отличное от имени свойства в Kotlin
import kotlinx.serialization.SerialName

// Импортируем аннотацию Serializable, которая помечает класс как пригодный
// для автоматической сериализации и десериализации (например, в/из JSON)
import kotlinx.serialization.Serializable


// Аннотация @Serializable указывает библиотеке kotlinx.serialization,
// что экземпляры этого класса можно преобразовывать в JSON и обратно
@Serializable
// Data-класс представляет параметры запроса, отправляемого на сервер
// для проверки корректности введённого пользователем SMS-кода
data class AuthCheckSmsParams(

    // Номер телефона в международном формате (без знака "+", например: "79123456789")
    // В JSON-запросе это поле будет называться "tel"
    val tel: String,

    // SMS-код, введённый пользователем (обычно 4 цифры)
    // Аннотация @SerialName("sms_pin") указывает, что в JSON это поле должно иметь имя "sms_pin",
    // что соответствует соглашениям именования на стороне сервера (часто используется snake_case)
    @SerialName("sms_pin") val smsPin: String,
)

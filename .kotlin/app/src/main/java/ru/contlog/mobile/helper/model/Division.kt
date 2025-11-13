package ru.contlog.mobile.helper.model

// Импорт аннотации SerialName из kotlinx.serialization —
// позволяет указать, какое имя поле должно иметь в JSON (например, при десериализации ответа от сервера)
import kotlinx.serialization.SerialName

// Импорт аннотации Serializable из kotlinx.serialization —
// помечает класс как пригодный для автоматической сериализации и десериализации через kotlinx.serialization
import kotlinx.serialization.Serializable


// Аннотация @Serializable указывает, что этот класс может быть преобразован в JSON и обратно
// с использованием библиотеки kotlinx.serialization (например, при работе с Ktor или Retrofit)
@Serializable

// Data-класс, представляющий подразделение (например, склад, офис, точку выдачи) в системе
// Реализует также java.io.Serializable — стандартный интерфейс Java для сериализации,
// что позволяет передавать объект через Bundle (например, при навигации между фрагментами в Android)
data class Division(

    // Уникальный идентификатор подразделения.
    // В JSON-ответе сервера поле называется "division_uid", что указано с помощью @SerialName
    @SerialName("division_uid") val uid: String,

    // Название подразделения (например, "Центральный склад", "Офис на Ленина").
    // В JSON-ответе сервера соответствует полю "division_name"
    @SerialName("division_name") val name: String,

    // Адрес подразделения. Имя поля в JSON совпадает с именем свойства в Kotlin,
    // поэтому аннотация @SerialName не требуется
    val address: String,

    // Числовой код подразделения (например, 101, 205 и т.д.).
    // В JSON-ответе сервера поле называется "division_code"
    @SerialName("division_code") val divisionCode: Int,
) : java.io.Serializable            // Реализация java.io.Serializable позволяет безопасно передавать объект через Intent/Bundle в Android

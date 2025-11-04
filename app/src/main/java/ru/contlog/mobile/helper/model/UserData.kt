package ru.contlog.mobile.helper.model

// Импорт аннотации SerialName из библиотеки kotlinx.serialization,
// которая позволяет указать, какое имя поле имеет в JSON-ответе сервера
// (особенно полезно, когда имена в JSON отличаются от соглашений Kotlin)
import kotlinx.serialization.SerialName

// Импорт аннотации Serializable, помечающей класс как пригодный
// для автоматической сериализации и десериализации (например, при парсинге JSON)
import kotlinx.serialization.Serializable

// Аннотация @Serializable указывает, что объекты этого класса могут быть
// автоматически преобразованы из JSON в Kotlin-объект и обратно
// с использованием библиотеки kotlinx.serialization
@Serializable

// Data-класс, представляющий данные авторизованного пользователя
data class UserData(

    // Уникальный идентификатор пользователя на стороне сервера.
    // В JSON-ответе сервера поле называется "fl_uid"
    @SerialName("fl_uid") val uid: String,

    // Полное имя пользователя (ФИО).
    // В JSON-ответе сервера поле называется "fl_name"
    @SerialName("fl_name") val name: String,

    // Должность или роль пользователя в компании.
    // Имя поля в JSON совпадает с именем свойства в Kotlin,
    // поэтому аннотация @SerialName не требуется
    val position: String,

    // Строка с фотографией пользователя, закодированная в формате Base64.
    // Имя поля в JSON совпадает с именем свойства, поэтому @SerialName не используется
    val photo: String,
)

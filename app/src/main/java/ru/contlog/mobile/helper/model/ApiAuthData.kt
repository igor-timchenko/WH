package ru.contlog.mobile.helper.model

// Импорт аннотации для указания соответствия имени поля в JSON (используется библиотекой kotlinx.serialization)
import kotlinx.serialization.SerialName

// Импорт аннотации, помечающей класс как сериализуемый (поддерживается kotlinx.serialization)
import kotlinx.serialization.Serializable

// Аннотация @Serializable указывает, что этот класс может быть автоматически сериализован в JSON
// и десериализован из JSON при использовании библиотеки kotlinx.serialization
// (например, при работе с Ktor, Retrofit с kotlinx-адаптером и т.д.)
@Serializable

// Data-класс для хранения данных авторизации, полученных от сервера после успешной аутентификации
data class ApiAuthData(

    // Поле `uid` представляет уникальный идентификатор пользователя (или организации) на стороне сервера.
    // Аннотация @SerialName("fl_uid") указывает, что в JSON-ответе это поле называется именно "fl_uid"
    @SerialName("fl_uid") val uid: String,

    // Поле `apiKey` — это секретный ключ, используемый для авторизации последующих запросов к API.
    // В JSON-ответе сервера оно передаётся под именем "api_key"
    @SerialName("api_key") val apiKey: String,
)
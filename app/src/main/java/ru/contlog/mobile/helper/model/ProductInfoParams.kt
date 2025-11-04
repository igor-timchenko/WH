package ru.contlog.mobile.helper.model

// Импорт аннотации SerialName из библиотеки kotlinx.serialization,
// которая позволяет указать, какое имя поле должно иметь в JSON-представлении
// (полезно при несоответствии стиля именования Kotlin и серверного API)
import kotlinx.serialization.SerialName

// Импорт аннотации Serializable из kotlinx.serialization,
// помечающей класс как пригодный для автоматической сериализации и десериализации
// (например, при отправке данных на сервер в формате JSON)
import kotlinx.serialization.Serializable

// Аннотация @Serializable указывает, что объекты этого класса могут быть
// автоматически преобразованы в JSON и восстановлены из JSON
// с использованием библиотеки kotlinx.serialization
@Serializable

// Data-класс, представляющий параметры запроса для получения подробной информации о продукте
// в контексте конкретного подразделения
data class ProductInfoParams(

    // Уникальный идентификатор подразделения (division UID).
    // В JSON-запросе поле будет называться "division_uid"
    @SerialName("division_uid") val divisionUID: String,

    // Числовой код подразделения (например, 101, 205 и т.д.).
    // В JSON-запросе поле будет называться "division_code"
    @SerialName("division_code") val divisionCode: Int,

    // Код продукта (внутренний идентификатор товара в системе).
    // В JSON-запросе поле будет называться "product_code"
    @SerialName("product_code") val productCode: String,
)

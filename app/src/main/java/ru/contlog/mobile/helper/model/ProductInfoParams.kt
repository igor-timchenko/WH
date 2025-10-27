// Пакет, в котором находится модель параметров запроса информации о продукте
package ru.contlog.mobile.helper.model

// Импорты для поддержки JSON-сериализации через kotlinx.serialization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель данных, представляющая параметры запроса к серверу для получения информации о продукте
 * в контексте конкретного подразделения.
 *
 * Используется, когда нужно запросить данные о товаре (например, местах хранения),
 * учитывая:
 *   - в каком подразделении ищем (по UID и коду),
 *   - какой именно продукт запрашиваем (по коду).
 *
 * Аннотация @Serializable позволяет автоматически преобразовать объект в JSON
 * при отправке POST-запроса к API.
 */
@Serializable
data class ProductInfoParams(
    /**
     * Уникальный идентификатор подразделения (например, склада или цеха).
     * В JSON будет сериализован как "division_uid".
     * Пример: "div_789abc"
     */
    @SerialName("division_uid") val divisionUID: String,

    /**
     * Числовой код подразделения (часто используется в учётных системах как короткий идентификатор).
     * В JSON — "division_code".
     * Пример: 101
     */
    @SerialName("division_code") val divisionCode: Int,

    /**
     * Код продукта (артикул, внутренний номер товара и т.п.).
     * В JSON — "product_code".
     * Пример: "PRD-2025-X7"
     */
    @SerialName("product_code") val productCode: String,
)

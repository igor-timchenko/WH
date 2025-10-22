package ru.contlog.mobile.helper.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    @SerialName("ОбъектUID") val productUID: String,
    @SerialName("Состояние") val state: String,
    @SerialName("КодШК") val barcodeCode: Int,
    @SerialName("КодОбъекта") val productCode: String,
    @SerialName("Штрихкод") val barcode: String,
    @SerialName("ЕдиницаИзмеренияСтрокой") val unitName: String,
    @SerialName("СсылкаОбъектСтрокой") val productLinkString: String,
    @SerialName("ДатаСоздания") val createdOn: LocalDateTime,
    @SerialName("Тип") val unitType: String,
    @SerialName("КоэффициентШК") val skCoefficient: Int,
    @SerialName("Данные") val places: List<ProductPlace>,
//    @SerialName("ДанныеЧестныйЗнак") val fairMarkData: Unit,
    @SerialName("ТипОбъекта") val objectType: String,
) {
    val imageSrc = "https://my.contlog.ru/img/$productUID"
}

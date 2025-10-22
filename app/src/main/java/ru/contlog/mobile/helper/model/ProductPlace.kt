package ru.contlog.mobile.helper.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductPlace(
    @SerialName("АдресКод") val addressCode: String,
    @SerialName("ОстатокВРезерве") val leftoversInReserve: Int,
    @SerialName("ОстатокСвободный") val leftoversFree: Int,
    @SerialName("СрокГодности") val bestBefore: LocalDateTime,
    @SerialName("ДатаПроизводства") val productionDate: LocalDateTime,
    @SerialName("СправкаА") val infoA: String,
    @SerialName("ЭтоОсновноеМесто") val primaryPlace: Boolean,
    @SerialName("РазмерМУП") val sizeMUP: Int,
    @SerialName("РазмерТУП") val sizeTUP: Int,
    @SerialName("Код") val code: String,
    @SerialName("Артикул") val article: String,
    @SerialName("НоменклатураСтрокой") val nomenclatureString: String,
    @SerialName("ТоварМеркурий") val productMercury: Boolean,
    @SerialName("ТоварЧестныйЗнак") val productFairMark: Boolean,
    @SerialName("ЭтоРеклама") val isAdvertisement: Boolean,
    @SerialName("ПартияПоставщика") val suppliersBatch: String,
    @SerialName("АдресUID") val addressUID: String,
    @SerialName("НоменклатураUID") val nomenclatureUID: String,
)

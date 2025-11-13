package ru.contlog.mobile.helper.model

// Импорт класса LocalDateTime из kotlinx-datetime для работы с датой и временем
// в типобезопасном и кроссплатформенном виде
import kotlinx.datetime.LocalDateTime

// Импорт аннотации SerialName из kotlinx.serialization,
// позволяющей сопоставить имя свойства в Kotlin с именем поля в JSON (особенно при кириллических или нестандартных ключах)
import kotlinx.serialization.SerialName

// Импорт аннотации Serializable, помечающей класс как пригодный
// для автоматической сериализации и десериализации через kotlinx.serialization
import kotlinx.serialization.Serializable


// Аннотация @Serializable указывает, что объекты этого класса могут быть
// автоматически преобразованы в JSON и из JSON при работе с API
@Serializable

// Data-класс, представляющий конкретное место хранения или размещения продукта
// (например, ячейка на складе, полка в магазине и т.д.)
data class ProductPlace(

    // Уникальный код адреса (места хранения). В JSON поле называется "АдресКод"
    @SerialName("АдресКод") val addressCode: String,

    // Количество товара, находящегося в резерве (забронировано, но не доступно для выдачи).
    // JSON-ключ: "ОстатокВРезерве"
    @SerialName("ОстатокВРезерве") val leftoversInReserve: Int,

    // Количество товара, доступного для выдачи (свободный остаток).
    // JSON-ключ: "ОстатокСвободный"
    @SerialName("ОстатокСвободный") val leftoversFree: Int,

    // Срок годности товара (дата, до которой продукт пригоден к использованию).
    // Используется kotlinx.datetime.LocalDateTime. JSON-ключ: "СрокГодности"
    @SerialName("СрокГодности") val bestBefore: LocalDateTime,

    // Дата производства товара. JSON-ключ: "ДатаПроизводства"
    @SerialName("ДатаПроизводства") val productionDate: LocalDateTime,

    // Дополнительная информационная справка (тип "А"). JSON-ключ: "СправкаА"
    @SerialName("СправкаА") val infoA: String,

    // Флаг, указывающий, является ли это место основным для данного продукта.
    // JSON-ключ: "ЭтоОсновноеМесто"
    @SerialName("ЭтоОсновноеМесто") val primaryPlace: Boolean,

    // Размер в минимальной учётной единице (МУП). JSON-ключ: "РазмерМУП"
    @SerialName("РазмерМУП") val sizeMUP: Int,

    // Размер в транспортной учётной единице (ТУП). JSON-ключ: "РазмерТУП"
    @SerialName("РазмерТУП") val sizeTUP: Int,

    // Код места или партии. JSON-ключ: "Код"
    @SerialName("Код") val code: String,

    // Артикул продукта (внутренний номер в каталоге). JSON-ключ: "Артикул"
    @SerialName("Артикул") val article: String,

    // Строковое представление номенклатуры (полное название товара с характеристиками).
    // JSON-ключ: "НоменклатураСтрокой"
    @SerialName("НоменклатураСтрокой") val nomenclatureString: String,

    // Флаг, указывающий, что товар подлежит учёту в системе "Меркурий" (ветеринарный контроль в РФ).
    // JSON-ключ: "ТоварМеркурий"
    @SerialName("ТоварМеркурий") val productMercury: Boolean,

    // Флаг, указывающий, что товар зарегистрирован в системе маркировки "Честный ЗНАК".
    // JSON-ключ: "ТоварЧестныйЗнак"
    @SerialName("ТоварЧестныйЗнак") val productFairMark: Boolean,

    // Флаг, указывающий, что данная позиция является рекламной или демонстрационной.
    // JSON-ключ: "ЭтоРеклама"
    @SerialName("ЭтоРеклама") val isAdvertisement: Boolean,

    // Номер партии, предоставленный поставщиком. JSON-ключ: "ПартияПоставщика"
    @SerialName("ПартияПоставщика") val suppliersBatch: String,

    // Уникальный идентификатор адреса (места хранения) в формате UID. JSON-ключ: "АдресUID"
    @SerialName("АдресUID") val addressUID: String,

    // Уникальный идентификатор номенклатуры (продукта) в формате UID. JSON-ключ: "НоменклатураUID"
    @SerialName("НоменклатураUID") val nomenclatureUID: String,
)

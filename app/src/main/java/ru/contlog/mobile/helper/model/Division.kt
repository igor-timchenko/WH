// Пакет, в котором находится модель данных подразделения (отдела, цеха, филиала и т.п.)
package ru.contlog.mobile.helper.model

// Импорты для сериализации JSON (kotlinx.serialization) и передачи между компонентами Android (java.io.Serializable)
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * Модель данных, представляющая подразделение (например, цех, отдел, склад).
 *
 * Используется для:
 *   - десериализации списка подразделений из JSON-ответа сервера,
 *   - передачи объекта между фрагментами через Bundle (благодаря реализации java.io.Serializable).
 *
 * Аннотация @Serializable (из kotlinx.serialization) позволяет преобразовывать объект в/из JSON.
 * Реализация java.io.Serializable позволяет безопасно передавать объект через Android-аргументы (Bundle).
 */
@Serializable
data class Division(
    /**
     * Уникальный идентификатор подразделения на сервере.
     * В JSON поле называется "division_uid", но в коде используется удобное имя "uid".
     */
    @SerialName("division_uid") val uid: String,

    /**
     * Название подразделения (например, "Цех №3", "Администрация").
     * В JSON — "division_name".
     */
    @SerialName("division_name") val name: String,

    /**
     * Адрес подразделения.
     * Имя поля совпадает в JSON и Kotlin, поэтому @SerialName не требуется.
     */
    val address: String,

    /**
     * Числовой код подразделения (например, 101, 205).
     * В JSON сервер присылает как "division_code".
     */
    @SerialName("division_code") val divisionCode: Int,
) : java.io.Serializable // реализация для передачи через Bundle (например, при навигации между фрагментами)
// Пакет вспомогательных утилит
package ru.contlog.mobile.helper.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char

/**
 * Глобальный форматтер даты для отображения в формате ДД.ММ.ГГГГ.
 *
 * Пример: 27.10.2025
 *
 * Использует DSL kotlinx.datetime для безопасного и читаемого определения формата.
 * Создан один раз и переиспользуется — эффективно по памяти.
 */
val DDMMYYYYFormatter = LocalDateTime.Format {
    day()           // День месяца (1-31)
    char('.')       // Символ точки-разделителя
    monthNumber()   // Номер месяца (1-12)
    char('.')       // Ещё одна точка
    year()          // Полный год (например, 2025)
}

/**
 * Расширение-функция для [LocalDateTime], позволяющая легко преобразовать дату
 * в строку в формате "ДД.ММ.ГГГГ".
 *
 * Пример использования:
 * ```kotlin
 * val now = LocalDateTime.now()
 * val dateString = now.asDDMMYYYY // "27.10.2025"
 * ```
 *
 * Удобно использовать в адаптерах, ViewModel или любом месте, где нужно отобразить дату.
 */
val LocalDateTime.asDDMMYYYY: String
    get() {
        return this.format(DDMMYYYYFormatter)
    }
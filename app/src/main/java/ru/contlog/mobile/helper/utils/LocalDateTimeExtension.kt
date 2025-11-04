package ru.contlog.mobile.helper.utils

// Импорт класса LocalDateTime из библиотеки kotlinx-datetime для работы с датой и временем
import kotlinx.datetime.LocalDateTime

// Импорт функции format и DSL-элементов для создания форматтеров даты/времени
import kotlinx.datetime.format
import kotlinx.datetime.format.char

// Создаём глобальный форматтер даты с шаблоном ДД.ММ.ГГГГ (например, "05.11.2025")
val DDMMYYYYFormatter = LocalDateTime.Format {
    // Добавляем день месяца (например, 05)
    day()
    // Добавляем символ точки в качестве разделителя
    char('.')
    // Добавляем номер месяца (например, 11)
    monthNumber()
    // Добавляем ещё одну точку
    char('.')
    // Добавляем год (например, 2025)
    year()
}

// Расширение-свойство для LocalDateTime, которое возвращает строковое представление даты
// в формате ДД.ММ.ГГГГ с использованием созданного форматтера
val LocalDateTime.asDDMMYYYY: String
    get() {
        // Форматируем текущий объект LocalDateTime с помощью DDMMYYYYFormatter
        return this.format(DDMMYYYYFormatter)
    }
// Объявляем пакет, в котором находится класс — исключения, связанные с вспомогательными функциями мобильного приложения
package ru.contlog.mobile.helper.exceptions

// Класс ApiRequestException — это пользовательское (кастомное) исключение,
// которое будет выбрасываться при ошибках во время выполнения запросов к API.
// Он принимает два параметра конструктора:
// - apiMethod: название метода API, в котором произошла ошибка (например, "login", "fetchData")
// - exceptionMessage: текстовое описание самой ошибки (например, "Timeout", "404 Not Found")
class ApiRequestException(
    val apiMethod: String,                  // Название вызванного API-метода
    val exceptionMessage: String,           // Сообщение об ошибке, связанной с этим вызовом
) : Exception() {                           // Наследуем стандартный класс Exception

    // Переопределяем свойство message из базового класса Exception,
    // чтобы формировать более информативное сообщение об ошибке.
    override val message: String
        get() = "[${apiMethod}] ${exceptionMessage}"            // Формат: [название_метода] описание_ошибки
}
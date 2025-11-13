// Объявление пакета, в котором находится класс.
// Все классы в этом файле принадлежат пространству имён ru.contlog.mobile.helper.exceptions.
package ru.contlog.mobile.helper.exceptions

// Объявление пользовательского исключения ApiRequestException.
// Класс наследуется от базового класса Exception, что делает его проверяемым исключением в Kotlin/JVM.
class ApiRequestException(
    // Конструктор принимает название метода API, в котором произошла ошибка.
    // Например: "login", "fetchUserData" и т.д.
    val apiMethod: String,

    // Конструктор также принимает текстовое сообщение об ошибке.
    // Может содержать детали от сервера (например, "401 Unauthorized") или описание клиентской ошибки.
    val exceptionMessage: String,

    val humanMessage: String,
) : Exception() {  // Вызов конструктора родительского класса Exception без параметров.

    // Переопределение свойства message из родительского класса Exception.
    // Это свойство обычно используется при логировании или выводе ошибки.
    override val message: String
        // Геттер формирует строку в удобочитаемом формате:
        // сначала указывается метод API в квадратных скобках, затем — сообщение об ошибке.
        get() = "[${apiMethod}] ${exceptionMessage}"
}
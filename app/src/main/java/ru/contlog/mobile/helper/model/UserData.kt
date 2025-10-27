// Пакет, в котором находится модель данных о текущем пользователе
package ru.contlog.mobile.helper.model

// Импорты для поддержки JSON-сериализации через kotlinx.serialization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель данных, представляющая информацию о текущем авторизованном пользователе.
 *
 * Используется после успешной аутентификации (например, по SMS) для отображения:
 *   - ФИО,
 *   - должности,
 *   - аватара (в виде Base64-строки).
 *
 * Получается из API и сохраняется в ViewModel (например, AppViewModel) для использования
 * в профиле пользователя (например, в DivisionsListFragment).
 */
@Serializable
data class UserData(
    /**
     * Уникальный идентификатор пользователя в системе.
     * В JSON-ответе сервера поле называется "fl_uid".
     * Пример: "user_12345"
     */
    @SerialName("fl_uid") val uid: String,

    /**
     * Полное имя пользователя (ФИО).
     * В JSON — "fl_name".
     * Пример: "Иванов Иван Иванович"
     */
    @SerialName("fl_name") val name: String,

    /**
     * Должность или роль пользователя.
     * Имя поля совпадает в JSON и Kotlin, поэтому @SerialName не требуется.
     * Пример: "Кладовщик", "Администратор склада"
     */
    val position: String,

    /**
     * Фотография пользователя, закодированная в формате Base64.
     * Это обычная строка, содержащая бинарные данные изображения в текстовом виде.
     * Пример: "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwc... (очень длинная строка)"
     *
     * В UI (например, в ImageView) она декодируется с помощью:
     *   val bytes = Base64.decode(photo, Base64.DEFAULT)
     *   val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
     */
    val photo: String,
)

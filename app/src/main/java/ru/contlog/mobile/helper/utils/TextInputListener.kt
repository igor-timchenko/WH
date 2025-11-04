package ru.contlog.mobile.helper.utils

// Импорты классов из Android SDK для работы с текстовыми полями
import android.text.Editable      // Изменяемая последовательность символов (используется в afterTextChanged)
import android.text.TextWatcher   // Интерфейс для отслеживания изменений текста в EditText

// Кастомная реализация TextWatcher, которая упрощает обработку изменений текста
// Принимает лямбду onTextChange, вызываемую при каждом изменении текста
class TextInputListener(private val onTextChange: (s: String?) -> Unit) : TextWatcher {
    // Метод вызывается после изменения текста (редко используется при простой обработке)
    // В данной реализации игнорируется, так как основная логика в onTextChanged
    override fun afterTextChanged(s: Editable?) {}

    // Метод вызывается непосредственно перед изменением текста (редко используется)
    // В данной реализации не содержит логики
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    // Метод вызывается во время изменения текста — основное место для обработки
    override fun onTextChanged(
        s: CharSequence?,          // Новый текст (может быть null)
        start: Int,                // Начальный индекс изменения
        before: Int,               // Количество символов, заменённых в старом тексте
        count: Int                 // Количество вставленных символов
    ) {
        // Вызываем переданную лямбду onTextChange, передавая строковое представление текста
        // Если s == null, передаём null; иначе преобразуем CharSequence в String
        onTextChange(if (s == null) {
            null
        } else {
            s.toString()
        })
    }
}
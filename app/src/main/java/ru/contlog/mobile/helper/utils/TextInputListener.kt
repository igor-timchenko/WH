package ru.contlog.mobile.helper.utils

import android.text.Editable
import android.text.TextWatcher

/**
 * Упрощённая обёртка над [android.text.TextWatcher] для удобной реакции на изменения текста в EditText.
 *
 * Позволяет избежать многословного создания анонимного TextWatcher'а и сосредоточиться
 * только на логике обработки изменений текста.
 *
 * Пример использования:
 * ```kotlin
 * editText.addTextChangedListener(TextInputListener { text ->
 *     button.isEnabled = !text.isNullOrEmpty()
 * })
 * ```
 */
class TextInputListener(
    // Коллбэк, вызываемый при любом изменении текста (включая null)
    private val onTextChange: (s: String?) -> Unit
) : TextWatcher {

    /**
     * Вызывается после завершения изменения текста.
     * Не используется в данной реализации — основная логика в [onTextChanged].
     */
    override fun afterTextChanged(s: Editable?) {}

    /**
     * Вызывается непосредственно перед изменением текста.
     * Не используется — нам важен результат, а не состояние "до".
     */
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    /**
     * Вызывается при изменении текста.
     * Преобразует CharSequence в String (или null) и передаёт в пользовательский коллбэк.
     */
    override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) {
        onTextChange(
            if (s == null) {
                null
            } else {
                s.toString()
            }
        )
    }
}
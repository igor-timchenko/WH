package ru.contlog.mobile.helper

import android.text.Editable
import android.text.TextWatcher

class TextInputListener(private val onTextChange: (s: String?) -> Unit) : TextWatcher {
    override fun afterTextChanged(s: Editable?) {}

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) {
        onTextChange(if (s == null) {
            null
        } else {
            s.toString()
        })
    }
}
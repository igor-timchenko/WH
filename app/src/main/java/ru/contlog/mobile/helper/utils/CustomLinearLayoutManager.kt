// Пакет вспомогательных утилит
package ru.contlog.mobile.helper.utils

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

/**
 * Кастомный LayoutManager для RecyclerView, расширяющий LinearLayoutManager.
 *
 * Основное назначение: **динамическое включение/отключение вертикальной прокрутки**.
 *
 * Используется в сценариях, где нужно временно "заморозить" прокрутку RecyclerView,
 * например:
 *   - при взаимодействии с вложенным RecyclerView (чтобы не было конфликта жестов),
 *   - во время анимаций,
 *   - при модальных состояниях UI.
 */
class CustomLinearLayoutManager(context: Context) : LinearLayoutManager(context, VERTICAL, false) {

    /**
     * Флаг, разрешающий или запрещающий прокрутку.
     * По умолчанию — true (прокрутка разрешена).
     */
    var isScrollEnabled = true

    /**
     * Переопределяет метод, определяющий, может ли список прокручиваться по вертикали.
     *
     * Возвращает true ТОЛЬКО если:
     *   - прокрутка разрешена (isScrollEnabled == true),
     *   - и базовый LinearLayoutManager разрешает прокрутку (например, есть элементы за пределами экрана).
     */
    override fun canScrollVertically(): Boolean = isScrollEnabled && super.canScrollVertically()
}
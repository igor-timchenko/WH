package ru.contlog.mobile.helper.utils

// Импорт класса Context из Android SDK — предоставляет доступ к ресурсам и системным сервисам
import android.content.Context

// Импорт стандартного LayoutManager для RecyclerView с вертикальной прокруткой
import androidx.recyclerview.widget.LinearLayoutManager

// Кастомный LayoutManager, наследующий LinearLayoutManager,
// который позволяет программно включать и отключать прокрутку
class CustomLinearLayoutManager(context: Context) : LinearLayoutManager(context, VERTICAL, false) {
    // Флаг, управляющий возможностью прокрутки: true — прокрутка разрешена, false — запрещена
    var isScrollEnabled = true

    // Переопределенный метод, определяющий, может ли RecyclerView прокручиваться по вертикали
    // Возвращает true, только если включена прокрутка (isScrollEnabled == true)
    // И стандартная логика LinearLayoutManager разрешает прокрутку
    override fun canScrollVertically(): Boolean = isScrollEnabled && super.canScrollVertically()
}
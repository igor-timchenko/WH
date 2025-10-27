// Пакет адаптеров RecyclerView
package ru.contlog.mobile.helper.rvadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.contlog.mobile.helper.databinding.ItemDivisionBinding
import ru.contlog.mobile.helper.model.Division

/**
 * Адаптер для отображения списка подразделений (отделов, складов и т.п.) в RecyclerView.
 *
 * Особенности:
 *   - Поддерживает обновление данных без полного пересоздания списка (частичные уведомления),
 *   - Обрабатывает клик по кнопке "Открыть" в каждом элементе,
 *   - Использует ViewBinding для безопасной и удобной работы с UI.
 */
class DivisionsRVAdapter(
    // Коллбэк, вызываемый при нажатии на кнопку "Открыть" в элементе списка
    private val onOpen: (Division) -> Unit
) : RecyclerView.Adapter<DivisionsRVAdapter.VH>() {

    // Внутренний список данных — источник истины для RecyclerView
    private val divisionsList = mutableListOf<Division>()

    /**
     * Создаёт ViewHolder для нового элемента списка.
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemDivisionBinding.inflate(inflater, parent, false)
        return VH(binding, onOpen)
    }

    /**
     * Привязывает данные к существующему ViewHolder'у.
     */
    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {
        holder.bind(divisionsList[position])
    }

    /**
     * Возвращает общее количество элементов в списке.
     */
    override fun getItemCount(): Int = divisionsList.size

    /**
     * Обновляет данные в адаптере с анимацией.
     *
     * ⚠️ Текущая реализация НЕ оптимальна:
     *   - удаляет все элементы,
     *   - добавляет все заново.
     * Это приводит к полной перерисовке списка, даже если изменился один элемент.
     *
     * ✅ Рекомендуется использовать DiffUtil для частичных обновлений.
     */
    fun setData(newData: List<Division>) {
        val oldCount = divisionsList.size
        divisionsList.clear()
        notifyItemRangeRemoved(0, oldCount) // Удаляем все старые элементы
        divisionsList.addAll(newData)
        notifyItemRangeInserted(0, divisionsList.size) // Добавляем все новые
        // ⚠️ Было notifyItemRangeChanged — это неверно! Нужно INSERT, а не CHANGE.
    }

    /**
     * ViewHolder — контейнер для UI-элементов одного элемента списка.
     */
    class VH(
        private val binding: ItemDivisionBinding,
        private val onOpen: (Division) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Привязывает данные подразделения к UI.
         */
        fun bind(division: Division) {
            binding.name.text = division.name
            binding.address.text = division.address

            // Устанавливаем обработчик клика на кнопку "Открыть"
            binding.open.setOnClickListener {
                onOpen(division)
            }
        }
    }
}
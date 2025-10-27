// Пакет адаптеров RecyclerView
package ru.contlog.mobile.helper.rvadapters

// Импорты
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.contlog.mobile.helper.R
import ru.contlog.mobile.helper.databinding.ItemDivisionBinding // ⚠️ Не используется — можно удалить
import ru.contlog.mobile.helper.databinding.ItemProductBinding
import ru.contlog.mobile.helper.model.Division // ⚠️ Не используется — можно удалить
import ru.contlog.mobile.helper.model.Product
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager

/**
 * Адаптер для отображения списка продуктов с возможностью раскрытия вложенного списка мест хранения.
 *
 * Особенности:
 *   - Поддерживает сворачивание/разворачивание блока с местами хранения,
 *   - Управляет конфликтом прокрутки между внешним и вложенным RecyclerView,
 *   - Загружает изображение продукта через Glide.
 */
class ProductsRVAdapter(
    // Коллбэк для управления прокруткой внешнего RecyclerView при взаимодействии с вложенным списком
    val onChildScrollRequested: (Boolean) -> Unit
) : RecyclerView.Adapter<ProductsRVAdapter.VH>() {

    // Внутренний список продуктов — источник данных для адаптера
    private val productsList = mutableListOf<Product>()

    /**
     * Создаёт ViewHolder для нового элемента списка.
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProductBinding.inflate(inflater, parent, false)
        return VH(binding, onChildScrollRequested)
    }

    /**
     * Привязывает данные продукта к ViewHolder'у.
     */
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(productsList[position])
    }

    /**
     * Возвращает общее количество продуктов в списке.
     */
    override fun getItemCount(): Int = productsList.size

    /**
     * Обновляет данные в адаптере.
     *
     * ⚠️ КРИТИЧЕСКАЯ ОШИБКА:
     *   - Используется notifyItemRangeChanged вместо notifyItemRangeInserted.
     *   - Это приводит к отсутствию анимации и возможным артефактам UI.
     */
    fun setData(newData: List<Product>) {
        val oldCount = productsList.size
        productsList.clear()
        notifyItemRangeRemoved(0, oldCount)      // Удаляем все старые элементы
        productsList.addAll(newData)
        notifyItemRangeChanged(0, productsList.size) // ❌ НЕВЕРНО! Должно быть INSERT
    }

    /**
     * ViewHolder для элемента продукта.
     */
    class VH(
        private val binding: ItemProductBinding,
        private val onChildScrollRequested: (Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Привязывает данные продукта к UI.
         *
         * @SuppressLint("ClickableViewAccessibility") — подавляет предупреждение о нарушении доступности.
         * Использование OnTouchListener может мешать работе TalkBack.
         */
        @SuppressLint("ClickableViewAccessibility")
        fun bind(product: Product) {
            // === Основная информация о продукте ===
            binding.productCode.text = product.productCode
            binding.barcodeCode.text = product.barcodeCode.toString()
            binding.productName.text = product.productLinkString

            // === Настройка вложенного RecyclerView для мест хранения ===
            binding.productPlaces.layoutManager = LinearLayoutManager(
                binding.root.context,
                LinearLayoutManager.VERTICAL,
                false
            )

            // === Управление прокруткой вложенного списка ===
            // При касании вложенного списка — запрещаем внешнему RecyclerView перехватывать события прокрутки
            binding.productPlaces.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        onChildScrollRequested(false) // Отключить прокрутку внешнего списка
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        onChildScrollRequested(true) // Включить прокрутку внешнего списка
                        v.parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false // Не перехватываем событие полностью
            }

            // === Подсчёт общего количества товара ===
            val count = product.places.sumOf {
                it.leftoversFree + it.leftoversInReserve
            }

            if (product.places.isEmpty()) {
                // Нет мест хранения — показываем заглушку
                binding.expansionTitle.text =
                    binding.root.context.getString(R.string.label_no_product_places)
                binding.expansionHeader.setOnClickListener {} // Пустой обработчик (блок неактивен)
                binding.expansionIndicator.visibility = View.GONE
                binding.productPlaces.adapter = null
            } else {
                // Есть места — настраиваем раскрывающийся блок
                binding.expansionTitle.text =
                    binding.root.context.getString(R.string.title_places_exp, count)

                // Переключение видимости содержимого при клике на заголовок
                binding.expansionHeader.setOnClickListener {
                    binding.expansionContent.visibility =
                        if (binding.expansionContent.isGone) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    // Меняем иконку стрелки в зависимости от состояния
                    binding.expansionIndicator.setImageDrawable(
                        if (binding.expansionContent.isGone) {
                            AppCompatResources.getDrawable(
                                binding.root.context,
                                R.drawable.outline_arrow_drop_down_24
                            )
                        } else {
                            AppCompatResources.getDrawable(
                                binding.root.context,
                                R.drawable.outline_arrow_drop_up_24
                            )
                        }
                    )
                }

                // Устанавливаем адаптер для вложенного списка
                // Сортируем: основные места (primaryPlace = true) — вверху списка
                binding.productPlaces.adapter =
                    ProductPlacesRVAdapter(
                        product.unitName,
                        product.places.sortedBy { !it.primaryPlace }
                    )
            }

            // === Загрузка изображения продукта ===
            // ⚠️ Нет обработки ошибок и заглушек — если изображение не загрузится, будет пусто
            Glide.with(binding.root)
                .load(product.imageSrc)
                .into(binding.productImage)
        }
    }
}
package ru.contlog.mobile.helper.rvadapters

// Импорты стандартных и вспомогательных классов Android
import android.annotation.SuppressLint            // Для подавления предупреждений (например, accessibility)
import android.view.LayoutInflater              // Для создания View из XML
import android.view.MotionEvent                 // Для обработки касаний
import android.view.View                        // Базовый класс представления
import android.view.ViewGroup                   // Контейнер для элементов RecyclerView
// Для доступа к ресурсам через AppCompat (например,getDrawable)
import androidx.appcompat.content.res.AppCompatResources
// Базовый класс адаптера для RecyclerView
import androidx.recyclerview.widget.RecyclerView
// Библиотека для загрузки изображений
import com.bumptech.glide.Glide
// Ресурсы приложения (строки, цвета, drawables)
import ru.contlog.mobile.helper.R
// ViewBinding для элемента списка продуктов
import ru.contlog.mobile.helper.databinding.ItemProductBinding
import ru.contlog.mobile.helper.model.Product
// Расширение Kotlin для удобной работы с видимостью (isGone)
import androidx.core.view.isGone
// LayoutManager для вложенного RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import ru.contlog.mobile.helper.model.ProductPlace

// Адаптер для RecyclerView, отображающий список продуктов.
// Принимает callback onChildScrollRequested для управления прокруткой вложенного списка.
class ProductsRVAdapter(val onChildScrollRequested: (Boolean) -> Unit) :
    RecyclerView.Adapter<ProductsRVAdapter.VH>() {
    // Внутренний изменяемый список для хранения данных продуктов
    private val productsList = mutableListOf<Product>()

    // Создаёт ViewHolder при необходимости отобразить новый элемент (вне экрана)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {
        // Получаем LayoutInflater из контекста родителя
        val inflater = LayoutInflater.from(parent.context)
        // Создаём ViewBinding для layout-файла элемента продукта
        val binding = ItemProductBinding.inflate(inflater, parent, false)

        // Возвращаем новый экземпляр ViewHolder'а, передавая ему binding и callback
        return VH(binding, onChildScrollRequested)
    }

    // Привязывает данные к существующему ViewHolder (повторное использование при прокрутке)
    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {
        // Передаём элемент списка в метод bind ViewHolder'а
        holder.bind(productsList[position])
    }

    // Возвращает общее количество элементов в списке
    override fun getItemCount(): Int = productsList.size

    // Метод для обновления данных в адаптере с анимацией
    fun setData(newData: List<Product>) {
        // Сохраняем старое количество элементов
        val oldCount = productsList.size
        // Очищаем текущий список
        productsList.clear()
        // Уведомляем адаптер об удалении всех старых элементов
        notifyItemRangeRemoved(0, oldCount)
        // Добавляем новые данные
        productsList.addAll(newData)
        // Уведомляем адаптер об изменении элементов (для перерисовки)
        notifyItemRangeChanged(0, productsList.size)
    }

    // Внутренний класс ViewHolder — держит ссылки на UI-элементы одного элемента списка
    class VH(
        private val binding: ItemProductBinding,
        private val onChildScrollRequested: (Boolean) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) { // itemView = binding.root

        // Подавляем предупреждение о нарушении accessibility — кастомная обработка касаний оправдана
        @SuppressLint("ClickableViewAccessibility")
        fun bind(product: Product) {
            // Показываем индикатор раскрываемости (стрелка)
            binding.expansionIndicator.visibility = View.VISIBLE

            // Устанавливаем код продукта
            binding.productCode.text = if (product.places.isEmpty()) {product.places[0].code} else {product.productCode}

            // Устанавливаем числовой код штрихкода
            binding.barcodeCode.text = product.barcodeCode.toString()
            // Устанавливаем название продукта
            binding.productName.text = product.productLinkString

            // Настраиваем LayoutManager для вложенного RecyclerView (места хранения)
            binding.productPlaces.layoutManager = LinearLayoutManager(
                binding.root.context,
                LinearLayoutManager.VERTICAL,
                false
            )

            // Обрабатываем касания во вложенном списке, чтобы корректно работала прокрутка
            binding.productPlaces.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Запрещаем родительскому RecyclerView перехватывать касания
                        onChildScrollRequested(false)
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // Разрешаем родительскому RecyclerView снова перехватывать касания
                        onChildScrollRequested(true)
                        v.parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false // не потреблять событие — передаём дальше
            }

            // Вычисляем общее количество товара (свободный остаток + в резерве)
            val count = product.places.sumOf {
                it.leftoversFree + it.leftoversInReserve
            }

            // Если у продукта нет мест хранения
            if (product.places.isEmpty()) {
                // Устанавливаем заголовок "Нет мест хранения"
                binding.expansionTitle.text =
                    binding.root.context.getString(R.string.label_no_product_places)
                // Отключаем обработчик клика (нечего раскрывать)
                binding.expansionHeader.setOnClickListener {}
                // Скрываем индикатор раскрываемости
                binding.expansionIndicator.visibility = View.GONE
                // Очищаем адаптер вложенного списка
                binding.productPlaces.adapter = null
            } else {
                // Устанавливаем заголовок с количеством мест
                binding.expansionTitle.text =
                    binding.root.context.getString(R.string.title_places_exp, count, product.unitName)
                // Настраиваем обработчик клика по заголовку для раскрытия/скрытия списка мест
                binding.expansionHeader.setOnClickListener {
                    // Переключаем видимость контента
                    binding.expansionContent.visibility =
                        if (binding.expansionContent.isGone) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    // Меняем иконку индикатора в зависимости от состояния
                    binding.expansionIndicator.setImageDrawable(
                        if (binding.expansionContent.isGone) {
                            // Стрелка вниз (закрыто)
                            AppCompatResources.getDrawable(
                                binding.root.context,
                                R.drawable.outline_arrow_drop_down_24
                            )
                        } else {
                            // Стрелка вверх (открыто)
                            AppCompatResources.getDrawable(
                                binding.root.context,
                                R.drawable.outline_arrow_drop_up_24
                            )
                        }
                    )
                }

                // Устанавливаем адаптер для вложенного списка мест
                // Места сортируются так, чтобы основное место отображалось первым
                binding.productPlaces.adapter =
                    ProductPlacesRVAdapter(product.unitName, product.places.sortedBy {
                        !it.primaryPlace // primaryPlace = true → !true = false → идёт раньше false
                    })
            }

            // Загружаем изображение продукта с помощью Glide
            Glide.with(binding.root).load(product.imageSrc)
                // Устанавливаем placeholder при ошибке загрузки
                .error(R.drawable.baseline_error_outline_24).into(binding.productImage)
        }
    }
}
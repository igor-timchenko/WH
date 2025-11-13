package ru.contlog.mobile.helper.rvadapters

// Импорты стандартных классов Android для работы с UI и RecyclerView
import android.view.LayoutInflater      // Для создания View из XML-файлов
import android.view.ViewGroup           // Контейнер для элементов RecyclerView
// Базовый класс для адаптера и ViewHolder'а списков
import androidx.recyclerview.widget.RecyclerView
// Ресурсы приложения (цвета, строки и т.д.)
import ru.contlog.mobile.helper.R
// ViewBinding, сгенерированный из layout-файла элемента списка мест продукта
import ru.contlog.mobile.helper.databinding.ItemProductPlaceBinding
import ru.contlog.mobile.helper.model.ProductPlace
// Расширение для форматирования даты
import ru.contlog.mobile.helper.utils.asDDMMYYYY

// Адаптер для RecyclerView, отображающий список мест хранения продукта (ProductPlace)
// Принимает единицу измерения (например, "шт") и список мест
class ProductPlacesRVAdapter(
    private val itemUnit: String,              // Единица измерения (например, "шт", "кг")
    private val placesList: List<ProductPlace> // Список мест хранения продукта
) :
    RecyclerView.Adapter<ProductPlacesRVAdapter.VH>() { // Наследуется от базового адаптера RecyclerView

    // Создаёт ViewHolder при необходимости отобразить новый элемент (вне экрана)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {
        // Получаем LayoutInflater из контекста родительского ViewGroup
        val inflater = LayoutInflater.from(parent.context)
        // Создаём ViewBinding для layout-файла элемента списка
        val binding = ItemProductPlaceBinding.inflate(inflater, parent, false)

        // Возвращаем новый экземпляр ViewHolder'а
        return VH(binding)
    }

    // Привязывает данные к существующему ViewHolder (повторное использование при прокрутке)
    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {
        // Передаём элемент списка и единицу измерения в метод bind ViewHolder'а
        holder.bind(placesList[position], itemUnit)
    }

    // Возвращает общее количество элементов в списке
    override fun getItemCount(): Int = placesList.size

    // Внутренний класс ViewHolder — держит ссылки на UI-элементы одного элемента списка
    class VH(private val binding: ItemProductPlaceBinding) :
        RecyclerView.ViewHolder(binding.root) { // itemView = binding.root

        // Метод для привязки данных модели к UI
        fun bind(place: ProductPlace, itemUnit: String) {
            // Формируем название места: код адреса + "(Основное место)", если это основное место
            binding.placeName.text = buildString {
                append(place.addressCode.trim())
                if (place.primaryPlace) {
                    append(" (Основное место)")
                }
            }

            // Отображаем свободный остаток с единицей измерения
            binding.leftoversFree.text = buildString {
                append(place.leftoversFree)
                append(" ")
                append(itemUnit)
            }
            // Задаём цвет текста: чёрный, если остаток > 0, иначе красный
            binding.leftoversFree.setTextColor(binding.root.context.getColor(if (place.leftoversFree > 0) {
                R.color.black
            } else {
                R.color.red
            }))

            // Отображаем остаток в резерве с единицей измерения
            binding.leftoversInReserve.text = buildString {
                append(place.leftoversInReserve)
                append(" ")
                append(itemUnit)
            }
            // Задаём цвет текста: чёрный, если резерв > 0, иначе красный
            binding.leftoversInReserve.setTextColor(binding.root.context.getColor(if (place.leftoversInReserve > 0) {
                R.color.black
            } else {
                R.color.red
            }))

            // Отображаем номер партии поставщика
            binding.suppliersBatch.text = place.suppliersBatch
            // Форматируем и отображаем дату производства (в формате ДД.ММ.ГГГГ)
            binding.productionDate.text = place.productionDate.asDDMMYYYY
            // Форматируем и отображаем срок годности (в формате ДД.ММ.ГГГГ)
            binding.bestBefore.text = place.bestBefore.asDDMMYYYY
        }
    }
}
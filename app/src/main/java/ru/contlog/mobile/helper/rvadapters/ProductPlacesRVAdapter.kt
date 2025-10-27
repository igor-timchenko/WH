// Пакет адаптеров RecyclerView
package ru.contlog.mobile.helper.rvadapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.contlog.mobile.helper.R
import ru.contlog.mobile.helper.databinding.ItemProductPlaceBinding
import ru.contlog.mobile.helper.model.Product
import ru.contlog.mobile.helper.model.ProductPlace
import ru.contlog.mobile.helper.utils.asDDMMYYYY

/**
 * Адаптер для отображения списка мест хранения продукта (ProductPlace) в RecyclerView.
 *
 * Особенности:
 *   - Принимает единицу измерения (itemUnit) извне (например, "шт", "кг"),
 *   - Отображает остатки, даты, партию и выделяет нулевые остатки красным цветом,
 *   - Помечает основное место хранения.
 *
 * ⚠️ Важно: этот адаптер НЕ поддерживает обновление данных после создания!
 * Список placesList передаётся в конструктор и не может быть изменён.
 */
class ProductPlacesRVAdapter(
    private val itemUnit: String,
    private val placesList: List<ProductPlace>
) : RecyclerView.Adapter<ProductPlacesRVAdapter.VH>() {

    /**
     * Создаёт ViewHolder для нового элемента.
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProductPlaceBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    /**
     * Привязывает данные к ViewHolder'у.
     */
    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {
        holder.bind(placesList[position], itemUnit)
    }

    /**
     * Возвращает количество элементов.
     */
    override fun getItemCount(): Int = placesList.size

    /**
     * ViewHolder для элемента списка мест хранения.
     */
    class VH(private val binding: ItemProductPlaceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(place: ProductPlace, itemUnit: String) {
            // === Название места с пометкой "Основное место" ===
            binding.placeName.text = buildString {
                append(place.addressCode.trim())
                if (place.primaryPlace) {
                    append(" (Основное место)")
                }
            }

            // === Свободные остатки ===
            binding.leftoversFree.text = buildString {
                append(place.leftoversFree)
                append(" ")
                append(itemUnit)
            }
            // Цвет: чёрный, если остаток > 0, иначе красный
            binding.leftoversFree.setTextColor(
                binding.root.context.getColor(
                    if (place.leftoversFree > 0) R.color.black else R.color.red
                )
            )

            // === Остатки в резерве ===
            binding.leftoversInReserve.text = buildString {
                append(place.leftoversInReserve)
                append(" ")
                append(itemUnit)
            }
            binding.leftoversInReserve.setTextColor(
                binding.root.context.getColor(
                    if (place.leftoversInReserve > 0) R.color.black else R.color.red
                )
            )

            // === Прочие данные ===
            binding.suppliersBatch.text = place.suppliersBatch
            binding.productionDate.text = place.productionDate.asDDMMYYYY
            binding.bestBefore.text = place.bestBefore.asDDMMYYYY

            // ⚠️ Закомментировано или не используется:
            // - Glide (импорт есть, но не применяется — возможно, планировалось изображение),
            // - AppCompatResources, isGone, LinearLayoutManager — не используются в этом адаптере.
        }
    }
}
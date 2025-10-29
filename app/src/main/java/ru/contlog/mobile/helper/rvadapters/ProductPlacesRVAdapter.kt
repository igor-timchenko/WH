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

class ProductPlacesRVAdapter(
    private val itemUnit: String,
    private val placesList: List<ProductPlace>
) :
    RecyclerView.Adapter<ProductPlacesRVAdapter.VH>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProductPlaceBinding.inflate(inflater, parent, false)

        return VH(binding)
    }

    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {
        holder.bind(placesList[position], itemUnit)
    }

    override fun getItemCount(): Int = placesList.size

    class VH(private val binding: ItemProductPlaceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(place: ProductPlace, itemUnit: String) {
            binding.placeName.text = buildString {
                append(place.addressCode.trim())
                if (place.primaryPlace) {
                    append(" (Основное место)")
                }
            }

            binding.leftoversFree.text = buildString {
                append(place.leftoversFree)
                append(" ")
                append(itemUnit)
            }
            binding.leftoversFree.setTextColor(binding.root.context.getColor(if (place.leftoversFree > 0) {
                R.color.black
            } else {
                R.color.red
            }))

            binding.leftoversInReserve.text =  buildString {
                append(place.leftoversInReserve)
                append(" ")
                append(itemUnit)
            }
            binding.leftoversInReserve.setTextColor(binding.root.context.getColor(if (place.leftoversInReserve > 0) {
                R.color.black
            } else {
                R.color.red
            }))

            binding.suppliersBatch.text = place.suppliersBatch
            binding.productionDate.text = place.productionDate.asDDMMYYYY
            binding.bestBefore.text = place.bestBefore.asDDMMYYYY
        }
    }
}
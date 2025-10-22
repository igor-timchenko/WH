package ru.contlog.mobile.helper.rvadapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.contlog.mobile.helper.R
import ru.contlog.mobile.helper.databinding.ItemDivisionBinding
import ru.contlog.mobile.helper.databinding.ItemProductBinding
import ru.contlog.mobile.helper.model.Division
import ru.contlog.mobile.helper.model.Product
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager

class ProductsRVAdapter(val onChildScrollRequested: (Boolean) -> Unit) :
    RecyclerView.Adapter<ProductsRVAdapter.VH>() {
    private val productsList = mutableListOf<Product>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProductBinding.inflate(inflater, parent, false)

        return VH(binding, onChildScrollRequested)
    }

    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {
        holder.bind(productsList[position])
    }

    override fun getItemCount(): Int = productsList.size

    fun setData(newData: List<Product>) {
        val oldCount = productsList.size
        productsList.clear()
        notifyItemRangeRemoved(0, oldCount)
        productsList.addAll(newData)
        notifyItemRangeChanged(0, productsList.size)
    }

    class VH(private val binding: ItemProductBinding, private val onChildScrollRequested: (Boolean) -> Unit) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("ClickableViewAccessibility")
        fun bind(product: Product) {
            binding.productCode.text = product.productCode
            binding.barcodeCode.text = product.barcodeCode.toString()
            binding.productName.text = product.productLinkString

            binding.productPlaces.layoutManager = LinearLayoutManager(
                binding.root.context,
                LinearLayoutManager.VERTICAL,
                false
            )
            binding.productPlaces.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        onChildScrollRequested(false)
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        onChildScrollRequested(true)
                        v.parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false
            }

            val count = product.places.sumOf {
                it.leftoversFree + it.leftoversInReserve
            }
            if (product.places.isEmpty()) {
                binding.expansionTitle.text =
                    binding.root.context.getString(R.string.label_no_product_places)
                binding.expansionHeader.setOnClickListener {}
                binding.expansionIndicator.visibility = View.GONE

                binding.productPlaces.adapter = null
            } else {
                binding.expansionTitle.text =
                    binding.root.context.getString(R.string.title_places_exp, count)
                binding.expansionHeader.setOnClickListener {
                    binding.expansionContent.visibility =
                        if (binding.expansionContent.isGone) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
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

                binding.productPlaces.adapter =
                    ProductPlacesRVAdapter(product.unitName, product.places.sortedBy {
                        !it.primaryPlace
                    })
            }

            Glide.with(binding.root).load(product.imageSrc).into(binding.productImage)
        }
    }
}
package ru.contlog.mobile.helper.rvadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.contlog.mobile.helper.databinding.ItemDivisionBinding
import ru.contlog.mobile.helper.model.Division

class DivisionsRVAdapter(private val onOpen: (Division) -> Unit) : RecyclerView.Adapter<DivisionsRVAdapter.VH>() {
    private val divisionsList = mutableListOf<Division>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemDivisionBinding.inflate(inflater, parent, false)

        return VH(binding, onOpen)
    }

    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {
        holder.bind(divisionsList[position])
    }

    override fun getItemCount(): Int = divisionsList.size

    fun setData(newData: List<Division>) {
        val oldCount = divisionsList.size
        divisionsList.clear()
        notifyItemRangeRemoved(0, oldCount)
        divisionsList.addAll(newData)
        notifyItemRangeChanged(0, divisionsList.size)
    }

    class VH(private val binding: ItemDivisionBinding, private val onOpen: (Division) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        fun bind(division: Division) {
            binding.name.text = division.name
            binding.address.text = division.address
            binding.open.setOnClickListener {
                onOpen(division)
            }
        }
    }
}
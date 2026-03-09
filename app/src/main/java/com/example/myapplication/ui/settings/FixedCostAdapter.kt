package com.example.myapplication.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.FixedCostSetting
import com.example.myapplication.databinding.ItemFixedCostBinding

data class FixedCostItemVO(
    val entity: FixedCostSetting,
    val categoryName: String,
    val isIncome: Boolean
)

class FixedCostAdapter(
    private val onClick: (FixedCostItemVO) -> Unit
) : ListAdapter<FixedCostItemVO, FixedCostAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemFixedCostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FixedCostItemVO) {
            binding.tvCategoryName.text = item.categoryName
            binding.tvMemoAndDay.text = "毎月${item.entity.dayOfMonth}日 - ${item.entity.name}"
            
            if (item.isIncome) {
                binding.tvAmount.text = "+¥%,d".format(item.entity.amount)
                binding.tvAmount.setTextColor(0xFF2196F3.toInt())
            } else {
                binding.tvAmount.text = "-¥%,d".format(item.entity.amount)
                binding.tvAmount.setTextColor(0xFFF44336.toInt())
            }
            
            binding.root.setOnClickListener {
                onClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFixedCostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<FixedCostItemVO>() {
        override fun areItemsTheSame(oldItem: FixedCostItemVO, newItem: FixedCostItemVO) = oldItem.entity.id == newItem.entity.id
        override fun areContentsTheSame(oldItem: FixedCostItemVO, newItem: FixedCostItemVO) = oldItem == newItem
    }
}

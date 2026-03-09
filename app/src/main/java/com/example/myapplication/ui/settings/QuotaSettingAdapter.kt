package com.example.myapplication.ui.settings

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemQuotaSettingBinding

data class QuotaItemVO(
    val categoryId: Int,
    val categoryName: String,
    val existingQuotaId: Long,
    var amount: Long
)

class QuotaSettingAdapter : ListAdapter<QuotaItemVO, QuotaSettingAdapter.ViewHolder>(DiffCallback()) {

    // 内部で編集データを保持するリスト
    private var currentItems = mutableListOf<QuotaItemVO>()

    override fun submitList(list: List<QuotaItemVO>?) {
        currentItems = list?.map { it.copy() }?.toMutableList() ?: mutableListOf()
        super.submitList(currentItems)
    }

    fun getCurrentItems(): List<QuotaItemVO> = currentItems

    inner class ViewHolder(private val binding: ItemQuotaSettingBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private var watcher: TextWatcher? = null

        fun bind(item: QuotaItemVO) {
            binding.tvCategoryName.text = item.categoryName
            
            // 複数回呼ばれることによる重複を防止
            if (watcher != null) {
                binding.etQuotaAmount.removeTextChangedListener(watcher)
            }
            
            binding.etQuotaAmount.setText(if (item.amount > 0) item.amount.toString() else "")
            
            watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    item.amount = s?.toString()?.toLongOrNull() ?: 0L
                }
            }
            binding.etQuotaAmount.addTextChangedListener(watcher)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuotaSettingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<QuotaItemVO>() {
        override fun areItemsTheSame(oldItem: QuotaItemVO, newItem: QuotaItemVO) = oldItem.categoryId == newItem.categoryId
        override fun areContentsTheSame(oldItem: QuotaItemVO, newItem: QuotaItemVO) = oldItem == newItem
    }
}

package com.example.myapplication.ui.input

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.Category
import com.example.myapplication.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onClick: (Category) -> Unit,
    private val onLongClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var selectedCategoryId: Int? = null
    private val mutableItems = mutableListOf<Category>()
    var onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null

    override fun submitList(list: List<Category>?) {
        mutableItems.clear()
        mutableItems.addAll(list ?: emptyList())
        super.submitList(list?.toList())
    }

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.tvCategoryName.text = category.name
            
            // アイコンの色設定 (サンプルとしてパースに失敗したらグレーにする)
            val color = try {
                Color.parseColor("#${category.colorCode}")
            } catch (e: Exception) {
                Color.LTGRAY
            }

            val bg = binding.ivCategoryIcon.background.mutate() as GradientDrawable
            if (category.id == selectedCategoryId) {
                // 選択時は枠線をつけるなど
                bg.setStroke(4, Color.BLACK)
                bg.setColor(color)
            } else {
                bg.setStroke(0, Color.TRANSPARENT)
                bg.setColor(color)
            }
            binding.ivCategoryIcon.background = bg

            // 本来は category.iconName からリソースIDを取得して設定する
            // binding.ivCategoryIcon.setImageResource(...)

            binding.root.setOnClickListener {
                val oldSelected = selectedCategoryId
                selectedCategoryId = category.id
                // 選択状態の更新
                if (oldSelected != null) {
                    val oldIndex = currentList.indexOfFirst { it.id == oldSelected }
                    if (oldIndex != -1) notifyItemChanged(oldIndex)
                }
                notifyItemChanged(adapterPosition)
                
                onClick(category)
            }

            binding.root.setOnLongClickListener {
                onStartDrag?.invoke(this)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getSelectedCategoryId(): Int? = selectedCategoryId
    
    fun clearSelection() {
        val oldSelected = selectedCategoryId
        selectedCategoryId = null
        if (oldSelected != null) {
            val oldIndex = currentList.indexOfFirst { it.id == oldSelected }
            if (oldIndex != -1) notifyItemChanged(oldIndex)
        }
    }

    fun moveItem(from: Int, to: Int) {
        val list = currentList.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        super.submitList(list)
    }

    fun getOrderedCategories(): List<Category> {
        return currentList.mapIndexed { index, category ->
            category.copy(displayOrder = index)
        }
    }
}

class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
    override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem == newItem
    }
}

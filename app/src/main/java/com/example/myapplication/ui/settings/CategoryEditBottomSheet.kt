package com.example.myapplication.ui.settings

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.databinding.DialogCategoryEditBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CategoryEditBottomSheet(
    private val existingCategory: Category?,
    private val onSave: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogCategoryEditBinding? = null
    private val binding get() = _binding!!

    private val presetColors = listOf(
        "F44336", "E91E63", "9C27B0", "673AB7",
        "3F51B5", "2196F3", "03A9F4", "00BCD4",
        "009688", "4CAF50", "8BC34A", "CDDC39",
        "FFEB3B", "FFC107", "FF9800", "FF5722",
        "795548", "9E9E9E", "607D8B", "000000"
    )
    
    private var selectedColorCode = "F44336"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogCategoryEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupColorPalette()

        if (existingCategory != null) {
            binding.etName.setText(existingCategory.name)
            if (existingCategory.type == TransactionType.INCOME) {
                binding.rbIncome.isChecked = true
            } else {
                binding.rbExpense.isChecked = true
            }
            selectedColorCode = existingCategory.colorCode
            binding.btnDelete.visibility = View.VISIBLE
        } else {
            binding.btnDelete.visibility = View.GONE
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "カテゴリ名を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val type = if (binding.rbIncome.isChecked) TransactionType.INCOME else TransactionType.EXPENSE

            val result = Category(
                id = existingCategory?.id ?: 0,
                name = name,
                type = type,
                colorCode = selectedColorCode,
                iconName = existingCategory?.iconName ?: "ic_category_default",
                displayOrder = existingCategory?.displayOrder ?: 0
            )

            onSave(result)
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            if (existingCategory != null) {
                // デフォルトカテゴリの判定はここでは省略するか、ID等で判定（今回は全削除可能とする）
                onDelete(existingCategory)
                dismiss()
            }
        }
    }

    private fun setupColorPalette() {
        val size = (40 * resources.displayMetrics.density).toInt()
        val margin = (4 * resources.displayMetrics.density).toInt()

        presetColors.forEach { colorHex ->
            val colorView = ImageView(requireContext()).apply {
                layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#$colorHex"))
                    if (selectedColorCode == colorHex) {
                        setStroke(8, Color.BLACK)
                    }
                }
                background = drawable
                
                setOnClickListener {
                    selectedColorCode = colorHex
                    binding.glColors.removeAllViews()
                    setupColorPalette() // Re-render to show selection stroke
                }
            }
            binding.glColors.addView(colorView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

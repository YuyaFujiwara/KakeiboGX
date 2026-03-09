package com.example.myapplication.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.FixedCostSetting
import com.example.myapplication.databinding.DialogFixedCostBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FixedCostBottomSheet(
    private val categories: List<Category>,
    private val existingSetting: FixedCostSetting?,
    private val onSave: (FixedCostSetting) -> Unit,
    private val onDelete: (FixedCostSetting) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogFixedCostBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogFixedCostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (categories.isEmpty()) {
            Toast.makeText(requireContext(), "カテゴリが存在しません", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        if (existingSetting != null) {
            val index = categories.indexOfFirst { it.id == existingSetting.categoryId }
            if (index != -1) binding.spinnerCategory.setSelection(index)
            binding.etAmount.setText(existingSetting.amount.toString())
            binding.etDay.setText(existingSetting.dayOfMonth.toString())
            binding.etMemo.setText(existingSetting.name)
            binding.btnDelete.visibility = View.VISIBLE
        } else {
            binding.btnDelete.visibility = View.GONE
        }

        binding.btnSave.setOnClickListener {
            val selectedCategory = categories[binding.spinnerCategory.selectedItemPosition]
            val amount = binding.etAmount.text.toString().toLongOrNull() ?: 0L
            val day = binding.etDay.text.toString().toIntOrNull() ?: 1
            val memo = binding.etMemo.text.toString()

            if (amount <= 0 || day !in 1..31) {
                Toast.makeText(requireContext(), "入力内容（1~31日の指定など）を確認してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = FixedCostSetting(
                id = existingSetting?.id ?: 0L,
                name = memo,
                amount = amount,
                type = selectedCategory.type,
                categoryId = selectedCategory.id,
                frequency = com.example.myapplication.data.entity.Frequency.MONTHLY,
                dayOfMonth = day,
                startDate = existingSetting?.startDate ?: java.time.LocalDate.now()
            )
            onSave(result)
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            if (existingSetting != null) {
                onDelete(existingSetting)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

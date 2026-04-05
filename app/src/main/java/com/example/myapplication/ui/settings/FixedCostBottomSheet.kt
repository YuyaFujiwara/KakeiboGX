package com.example.myapplication.ui.settings

import android.app.DatePickerDialog
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FixedCostBottomSheet(
    private val categories: List<Category>,
    private val existingSetting: FixedCostSetting?,
    private val onSave: (FixedCostSetting) -> Unit,
    private val onDelete: (FixedCostSetting) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogFixedCostBinding? = null
    private val binding get() = _binding!!
    private var selectedEndDate: LocalDate? = null
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

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

            // 終了日の復元
            if (existingSetting.endDate != null) {
                selectedEndDate = existingSetting.endDate
                binding.tvEndDate.text = "終了日: ${existingSetting.endDate.format(dateFormatter)}"
            }
        } else {
            binding.btnDelete.visibility = View.GONE
        }

        // 終了日タップでDatePicker表示
        binding.tvEndDate.setOnClickListener {
            val base = selectedEndDate ?: LocalDate.now().plusMonths(12)
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedEndDate = LocalDate.of(year, month + 1, day)
                    binding.tvEndDate.text = "終了日: ${selectedEndDate!!.format(dateFormatter)}"
                },
                base.year,
                base.monthValue - 1,
                base.dayOfMonth
            ).show()
        }

        // 長押しで終了日クリア
        binding.tvEndDate.setOnLongClickListener {
            selectedEndDate = null
            binding.tvEndDate.text = ""
            binding.tvEndDate.hint = "終了日（タップして設定・省略可）"
            Toast.makeText(requireContext(), "終了日をクリアしました", Toast.LENGTH_SHORT).show()
            true
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
                startDate = existingSetting?.startDate ?: LocalDate.now(),
                endDate = selectedEndDate
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

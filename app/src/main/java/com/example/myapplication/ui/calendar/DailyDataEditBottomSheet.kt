package com.example.myapplication.ui.calendar

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.DailyData
import com.example.myapplication.databinding.DialogDailyDataEditBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DailyDataEditBottomSheet(
    private val dailyData: DailyData,
    private val categories: List<Category>,
    private val onSave: (DailyData) -> Unit,
    private val onDelete: (DailyData) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogDailyDataEditBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: LocalDate

    init {
        selectedDate = dailyData.date
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogDailyDataEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        val currentCategoryIndex = categories.indexOfFirst { it.id == dailyData.categoryId }
        if (currentCategoryIndex != -1) {
            binding.spinnerCategory.setSelection(currentCategoryIndex)
        }

        binding.etAmount.setText(dailyData.amount.toString())
        binding.etMemo.setText(dailyData.memo)

        updateDateButtonText()

        binding.btnDate.setOnClickListener {
            val dialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    updateDateButtonText()
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            )
            dialog.show()
        }

        binding.btnSave.setOnClickListener {
            val amountStr = binding.etAmount.text.toString()
            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "金額を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val amount = amountStr.toLongOrNull() ?: 0L
            val memo = binding.etMemo.text.toString()
            val selectedCategory = categories[binding.spinnerCategory.selectedItemPosition]

            val result = dailyData.copy(
                date = selectedDate,
                categoryId = selectedCategory.id,
                type = selectedCategory.type,
                amount = amount,
                memo = memo
            )
            onSave(result)
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            onDelete(dailyData)
            dismiss()
        }
    }

    private fun updateDateButtonText() {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        binding.btnDate.text = selectedDate.format(formatter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

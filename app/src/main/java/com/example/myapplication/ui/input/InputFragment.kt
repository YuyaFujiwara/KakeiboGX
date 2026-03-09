package com.example.myapplication.ui.input

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.data.entity.DailyData
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.databinding.FragmentInputBinding
import com.example.myapplication.ui.MainViewModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class InputFragment : Fragment() {

    private var _binding: FragmentInputBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private var currentDate = LocalDate.now()
    private var currentType = TransactionType.EXPENSE
    private var currentAmount = 0L

    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        updateDateText()

        binding.btnPrevDay.setOnClickListener {
            currentDate = currentDate.minusDays(1)
            updateDateText()
        }

        binding.btnNextDay.setOnClickListener {
            currentDate = currentDate.plusDays(1)
            updateDateText()
        }

        binding.btnCalendar.setOnClickListener {
            val dialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    currentDate = LocalDate.of(year, month + 1, dayOfMonth)
                    updateDateText()
                },
                currentDate.year,
                currentDate.monthValue - 1,
                currentDate.dayOfMonth
            )
            dialog.show()
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentType = if (tab?.position == 0) TransactionType.EXPENSE else TransactionType.INCOME
                categoryAdapter.clearSelection()
                // 電卓UIの金額をクリアなど
                currentAmount = 0L
                binding.tvAmount.text = "¥0"
                
                // 再フィルタリングしてリストを更新
                val filtered = viewModel.allCategories.value.filter { it.type == currentType }
                categoryAdapter.submitList(filtered)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.tvAmount.setOnClickListener {
            val dialog = CalculatorBottomSheet(currentAmount) { result ->
                currentAmount = result
                binding.tvAmount.text = "¥%,d".format(currentAmount)
            }
            dialog.show(parentFragmentManager, "CalculatorBottomSheet")
        }

        categoryAdapter = CategoryAdapter(
            onClick = { category ->
                // クリック処理(アダプター側で選択状態は管理済み)
            },
            onLongClick = { category ->
                Toast.makeText(requireContext(), "${category.name} の編集(未実装)", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvCategories.adapter = categoryAdapter

        binding.btnSubmit.setOnClickListener {
            val selectedCategoryId = categoryAdapter.getSelectedCategoryId()
            if (selectedCategoryId == null) {
                Toast.makeText(requireContext(), "カテゴリを選択してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val memo = binding.etMemo.text.toString()
            
            val dailyData = DailyData(
                date = currentDate,
                amount = currentAmount,
                memo = memo,
                type = currentType,
                categoryId = selectedCategoryId
            )

            viewModel.insertDailyData(dailyData)
            Toast.makeText(requireContext(), "登録しました", Toast.LENGTH_SHORT).show()
            
            // 入力欄リセット
            currentAmount = 0L
            binding.tvAmount.text = "¥0"
            binding.etMemo.text?.clear()
            categoryAdapter.clearSelection()
        }
    }

    private fun updateDateText() {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd (E)")
        binding.tvDate.text = currentDate.format(formatter)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allCategories.collectLatest { categories ->
                    // 支出/収入に応じてフィルタリングする
                    val filtered = categories.filter { it.type == currentType }
                    categoryAdapter.submitList(filtered)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

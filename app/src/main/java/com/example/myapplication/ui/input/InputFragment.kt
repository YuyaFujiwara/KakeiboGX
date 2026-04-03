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
import androidx.recyclerview.widget.RecyclerView
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

        // プリセットボタン
        binding.btnPreset.setOnClickListener {
            val dialog = PresetBottomSheet(
                currentType = currentType,
                presets = viewModel.allPresets.value,
                onPresetSelected = { preset ->
                    binding.etMemo.setText(preset.memo)
                    if (preset.amount > 0) {
                        currentAmount = preset.amount
                        binding.tvAmount.text = "¥%,d".format(currentAmount)
                    }
                    // カテゴリも自動選択
                    if (preset.categoryId != null) {
                        val categories = viewModel.allCategories.value.filter { it.type == currentType }
                        val index = categories.indexOfFirst { it.id == preset.categoryId }
                        if (index >= 0) {
                            // カテゴリアダプタのクリックをシミュレート
                        }
                    }
                    viewModel.incrementPresetUsageCount(preset.id)
                },
                onPresetAdded = { preset ->
                    viewModel.insertPreset(preset)
                },
                onPresetDeleted = { preset ->
                    viewModel.deletePreset(preset)
                }
            )
            dialog.show(parentFragmentManager, "PresetBottomSheet")
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

        // ドラッグ＆ドロップ並べ替え
        val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(
            object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                androidx.recyclerview.widget.ItemTouchHelper.UP or
                androidx.recyclerview.widget.ItemTouchHelper.DOWN or
                androidx.recyclerview.widget.ItemTouchHelper.LEFT or
                androidx.recyclerview.widget.ItemTouchHelper.RIGHT,
                0
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    categoryAdapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    // ドロップ完了時に並び順を保存
                    viewModel.updateCategoryOrder(categoryAdapter.getOrderedCategories())
                }

                override fun isLongPressDragEnabled(): Boolean = false // 手動で開始する
            }
        )
        itemTouchHelper.attachToRecyclerView(binding.rvCategories)

        categoryAdapter.onStartDrag = { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }

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

package com.example.myapplication.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.FixedCostSetting
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.databinding.FragmentFixedCostBinding
import com.example.myapplication.ui.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FixedCostFragment : Fragment() {

    private var _binding: FragmentFixedCostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: FixedCostAdapter
    private var allCategories: List<Category> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFixedCostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = "固定収支の設定"
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = FixedCostAdapter { item ->
            val dialog = FixedCostBottomSheet(allCategories, item.entity,
                onSave = { entity -> viewModel.updateFixedCostSetting(entity) },
                onDelete = { entity -> viewModel.deleteFixedCostSetting(entity) }
            )
            dialog.show(parentFragmentManager, "FixedCostBottomSheet")
        }

        binding.rvFixedCosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFixedCosts.adapter = adapter

        binding.fabAddFixedCost.setOnClickListener {
            val dialog = FixedCostBottomSheet(allCategories, null,
                onSave = { entity -> viewModel.insertFixedCostSetting(entity) },
                onDelete = {}
            )
            dialog.show(parentFragmentManager, "FixedCostBottomSheet")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var currentFixedCosts = viewModel.allFixedCostSettings.value
                
                launch {
                    viewModel.allCategories.collectLatest { categories ->
                        allCategories = categories
                        updateList(currentFixedCosts)
                    }
                }
                
                launch {
                    viewModel.allFixedCostSettings.collectLatest { fixedCosts ->
                        currentFixedCosts = fixedCosts
                        updateList(fixedCosts)
                    }
                }
            }
        }
    }

    private fun updateList(fixedCosts: List<FixedCostSetting>) {
        if (allCategories.isEmpty()) return
        val items = fixedCosts.map { fc ->
            val category = allCategories.find { it.id == fc.categoryId }
            FixedCostItemVO(
                entity = fc,
                categoryName = category?.name ?: "不明",
                isIncome = category?.type == TransactionType.INCOME
            )
        }
        adapter.submitList(items)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

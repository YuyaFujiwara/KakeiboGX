package com.example.myapplication.ui.settings

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.entity.QuotaSetting
import com.example.myapplication.databinding.FragmentQuotaSettingBinding
import com.example.myapplication.ui.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QuotaSettingFragment : Fragment() {

    private var _binding: FragmentQuotaSettingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: QuotaSettingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuotaSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = "予算(クォータ)設定"
        // back button settings
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = QuotaSettingAdapter()
        binding.rvQuotas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuotas.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allCategories.collectLatest { categories ->
                    val expenseCategories = categories.filter { it.type == com.example.myapplication.data.entity.TransactionType.EXPENSE }
                    val currentQuotas = viewModel.allQuotaSettings.value
                    
                    val items = expenseCategories.map { cat ->
                        val quota = currentQuotas.find { it.categoryId == cat.id }
                        QuotaItemVO(
                            categoryId = cat.id,
                            categoryName = cat.name,
                            existingQuotaId = quota?.id ?: 0L,
                            amount = quota?.amount ?: 0L
                        )
                    }
                    adapter.submitList(items)
                }
            }
        }

        binding.fabSaveQuotas.setOnClickListener {
            val items = adapter.getCurrentItems()
            var hasChanges = false
            items.forEach { item ->
                if (item.amount > 0) {
                    val entity = QuotaSetting(
                        id = item.existingQuotaId,
                        categoryId = item.categoryId,
                        amount = item.amount
                    )
                    if (item.existingQuotaId == 0L) {
                        viewModel.insertQuotaSetting(entity)
                    } else {
                        viewModel.updateQuotaSetting(entity)
                    }
                    hasChanges = true
                } else if (item.existingQuotaId != 0L && item.amount == 0L) {
                    val entity = QuotaSetting(id = item.existingQuotaId, categoryId = item.categoryId, amount = 0L)
                    viewModel.deleteQuotaSetting(entity)
                    hasChanges = true
                }
            }
            if (hasChanges) {
                Toast.makeText(requireContext(), "保存しました", Toast.LENGTH_SHORT).show()
            }
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

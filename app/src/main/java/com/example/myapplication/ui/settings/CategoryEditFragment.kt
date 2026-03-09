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
import com.example.myapplication.databinding.FragmentCategoryEditBinding
import com.example.myapplication.ui.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CategoryEditFragment : Fragment() {

    private var _binding: FragmentCategoryEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: CategoryEditAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoryEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = "カテゴリの確認・編集"
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = CategoryEditAdapter { category ->
            val dialog = CategoryEditBottomSheet(category,
                onSave = { updatedCategory -> viewModel.updateCategory(updatedCategory) },
                onDelete = { deletedCategory -> viewModel.deleteCategory(deletedCategory) }
            )
            dialog.show(parentFragmentManager, "CategoryEditBottomSheet")
        }

        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter

        binding.fabAddCategory.setOnClickListener {
            val dialog = CategoryEditBottomSheet(null,
                onSave = { newCategory -> viewModel.insertCategory(newCategory) },
                onDelete = {} // 新規追加なので削除は呼ばれない
            )
            dialog.show(parentFragmentManager, "CategoryEditBottomSheet")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allCategories.collectLatest { categories ->
                    // 支出・収入でソートして表示
                    adapter.submitList(categories.sortedBy { it.type })
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

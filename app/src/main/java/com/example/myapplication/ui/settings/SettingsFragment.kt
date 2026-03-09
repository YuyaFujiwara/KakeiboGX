package com.example.myapplication.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.databinding.FragmentSettingsBinding
import com.example.myapplication.ui.MainViewModel

import androidx.navigation.fragment.findNavController

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { 
            viewModel.exportCsv(it, requireContext()) 
            Toast.makeText(requireContext(), "CSVをエクスポートしました", Toast.LENGTH_SHORT).show()
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { 
            viewModel.importCsv(it, requireContext())
            Toast.makeText(requireContext(), "CSVをインポートしました", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnEditCategories.setOnClickListener {
            findNavController().navigate(com.example.myapplication.R.id.action_settings_to_category_edit)
        }

        binding.btnFixedCost.setOnClickListener {
            findNavController().navigate(com.example.myapplication.R.id.action_settings_to_fixed_cost)
        }

        binding.btnQuota.setOnClickListener {
            findNavController().navigate(com.example.myapplication.R.id.action_settings_to_quota)
        }

        binding.btnExportCsv.setOnClickListener {
            exportLauncher.launch("household_data.csv")
        }

        binding.btnImportCsv.setOnClickListener {
            importLauncher.launch(arrayOf("text/csv", "*/*"))
        }

        binding.btnInsertDummy.setOnClickListener {
            insertDummyCategories()
            Toast.makeText(requireContext(), "ダミーカテゴリを挿入しました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun insertDummyCategories() {
        val dummies = listOf(
            Category(name = "食費", type = TransactionType.EXPENSE, iconName = "ic_food", colorCode = "FD8104", displayOrder = 1),
            Category(name = "日用品", type = TransactionType.EXPENSE, iconName = "ic_home", colorCode = "00B547", displayOrder = 2),
            Category(name = "交通費", type = TransactionType.EXPENSE, iconName = "ic_train", colorCode = "2196F3", displayOrder = 3),
            Category(name = "美容", type = TransactionType.EXPENSE, iconName = "ic_beauty", colorCode = "FF54A8", displayOrder = 4),
            Category(name = "医療費", type = TransactionType.EXPENSE, iconName = "ic_medical", colorCode = "61E396", displayOrder = 6),
            Category(name = "給与", type = TransactionType.INCOME, iconName = "ic_money", colorCode = "4CAF50", displayOrder = 1)
        )
        dummies.forEach { viewModel.insertCategory(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

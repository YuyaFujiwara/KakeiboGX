package com.example.myapplication.ui.settings

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.data.sync.DriveHelper
import com.example.myapplication.databinding.FragmentSettingsBinding
import com.example.myapplication.ui.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.navigation.fragment.findNavController

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var driveHelper: DriveHelper

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { 
            viewModel.exportCsv(it, requireContext()) 
            Toast.makeText(requireContext(), "CSVをエクスポートしました", Toast.LENGTH_SHORT).show()
        }
    }

    private var pendingImportClearFirst = false

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { 
            viewModel.importCsv(it, requireContext(), pendingImportClearFirst)
            Toast.makeText(requireContext(), "CSVをインポートしました", Toast.LENGTH_SHORT).show()
        }
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    driveHelper.initDriveService(account)
                    updateSyncUI()
                    Toast.makeText(requireContext(), "ログインしました: ${account.email}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "ログイン失敗: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
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

        driveHelper = DriveHelper(requireContext())

        // 既存のセットアップ
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
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("CSVインポート")
                .setMessage("既存の記録データをどうしますか？")
                .setPositiveButton("上書き（全削除してインポート）") { _, _ ->
                    pendingImportClearFirst = true
                    importLauncher.launch(arrayOf("text/csv", "*/*"))
                }
                .setNeutralButton("追加（既存データを残す）") { _, _ ->
                    pendingImportClearFirst = false
                    importLauncher.launch(arrayOf("text/csv", "*/*"))
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }

        binding.btnInsertDummy.setOnClickListener {
            insertDummyCategories()
            Toast.makeText(requireContext(), "ダミーカテゴリを挿入しました", Toast.LENGTH_SHORT).show()
        }

        // --- 同期セクション ---

        // サインイン済みなら Drive サービスを初期化
        val account = driveHelper.getAccount()
        if (account != null) {
            driveHelper.initDriveService(account)
        }
        updateSyncUI()

        binding.btnGoogleSignIn.setOnClickListener {
            if (driveHelper.isSignedIn()) {
                // ログアウト
                lifecycleScope.launch {
                    driveHelper.signOut()
                    updateSyncUI()
                    Toast.makeText(requireContext(), "ログアウトしました", Toast.LENGTH_SHORT).show()
                }
            } else {
                signInLauncher.launch(driveHelper.getSignInIntent())
            }
        }

        binding.btnSyncNow.setOnClickListener {
            performSync()
        }
    }

    private fun updateSyncUI() {
        val isSignedIn = driveHelper.isSignedIn()
        val account = driveHelper.getAccount()

        if (isSignedIn && account != null) {
            binding.tvSyncStatus.text = "ログイン中: ${account.email}"
            binding.btnGoogleSignIn.text = "ログアウト"
            binding.btnSyncNow.isEnabled = true
        } else {
            binding.tvSyncStatus.text = "未ログイン"
            binding.btnGoogleSignIn.text = "Googleアカウントでログイン"
            binding.btnSyncNow.isEnabled = false
        }

        // 最終同期日時の表示
        val prefs = requireContext().getSharedPreferences("sync_prefs", 0)
        val lastSyncTime = prefs.getLong("last_sync_time", 0)
        if (lastSyncTime > 0) {
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
            binding.tvLastSync.text = "最終同期: ${sdf.format(Date(lastSyncTime))}"
        } else {
            binding.tvLastSync.text = "最終同期: --"
        }
    }

    private fun performSync() {
        binding.btnSyncNow.isEnabled = false
        binding.btnSyncNow.text = "同期中..."

        viewModel.performSync(driveHelper) { success, message ->
            if (!isAdded) return@performSync
            requireActivity().runOnUiThread {
                binding.btnSyncNow.isEnabled = true
                binding.btnSyncNow.text = "今すぐ同期"

                if (success) {
                    // 最終同期日時を保存
                    requireContext().getSharedPreferences("sync_prefs", 0)
                        .edit()
                        .putLong("last_sync_time", System.currentTimeMillis())
                        .apply()
                    updateSyncUI()
                    Toast.makeText(requireContext(), "同期完了", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "同期失敗: $message", Toast.LENGTH_SHORT).show()
                }
            }
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

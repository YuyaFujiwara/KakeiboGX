package com.example.myapplication.ui.compose.settings

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.entity.Category
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.ui.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    navController: NavController? = null,
    viewModel: MainViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            viewModel.exportCsv(it, context, null)
            Toast.makeText(context, "CSVをエクスポートしました", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            viewModel.importCsv(it, context, false)
            Toast.makeText(context, "CSVをインポートしました", Toast.LENGTH_SHORT).show()
        }
    }

    var isSignedIn by remember { mutableStateOf(viewModel.driveHelper.isSignedIn()) }
    var accountEmail by remember { mutableStateOf(viewModel.driveHelper.getAccount()?.email ?: "") }
    var isSyncing by remember { mutableStateOf(false) }

    val prefs = context.getSharedPreferences("sync_prefs", 0)
    var lastSyncTime by remember { mutableStateOf(prefs.getLong("last_sync_time", 0)) }

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    viewModel.driveHelper.initDriveService(account)
                    isSignedIn = true
                    accountEmail = account.email ?: ""
                    Toast.makeText(context, "ログインしました: ${account.email}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "ログイン失敗", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("基本設定", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { navController?.navigate("category_edit") }, modifier = Modifier.fillMaxWidth()) {
                Text("カテゴリの確認・編集")
            }
            OutlinedButton(onClick = { navController?.navigate("fixed_cost") }, modifier = Modifier.fillMaxWidth()) {
                Text("固定収支の設定")
            }
            OutlinedButton(onClick = { navController?.navigate("quota") }, modifier = Modifier.fillMaxWidth()) {
                Text("予算(クォータ)の設定")
            }
        }

        item {
            Text("データ管理", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val fileName = "household_data.csv"
                    exportLauncher.launch(fileName)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ローカルへCSVエクスポート")
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("text/csv", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CSVインポート")
            }
            OutlinedButton(
                onClick = {
                    val dummies = listOf(
                        Category(name = "食費", type = TransactionType.EXPENSE, iconName = "ic_food", colorCode = "FD8104", displayOrder = 1),
                        Category(name = "給与", type = TransactionType.INCOME, iconName = "ic_money", colorCode = "4CAF50", displayOrder = 1)
                    )
                    dummies.forEach { viewModel.insertCategory(it) }
                    Toast.makeText(context, "ダミーカテゴリを挿入しました", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("テスト用(ダミーデータ初期化)")
            }
        }

        item {
            Text("クラウド同期", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(if (isSignedIn) "ログイン中: $accountEmail" else "未ログイン", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    if (isSignedIn) {
                        coroutineScope.launch {
                            viewModel.driveHelper.signOut()
                            isSignedIn = false
                            accountEmail = ""
                            Toast.makeText(context, "ログアウトしました", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        signInLauncher.launch(viewModel.driveHelper.getSignInIntent())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSignedIn) "ログアウト" else "Googleアカウントでログイン")
            }

            OutlinedButton(
                onClick = {
                    isSyncing = true
                    viewModel.performSync(viewModel.driveHelper) { success, message ->
                        isSyncing = false
                        if (success) {
                            lastSyncTime = prefs.getLong("last_sync_time", 0)
                            Toast.makeText(context, "同期完了", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "同期失敗: $message", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isSignedIn && !isSyncing
            ) {
                Text(if (isSyncing) "同期中..." else "今すぐ同期")
            }

            val syncText = if (lastSyncTime > 0) {
                "最終同期: " + SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(lastSyncTime))
            } else {
                "最終同期: --"
            }
            Text(syncText, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

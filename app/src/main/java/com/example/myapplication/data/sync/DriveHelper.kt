package com.example.myapplication.data.sync

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.HttpResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Google Drive 上の kakeibo_sync.json を読み書きするヘルパー。
 *
 * - ファイルは「家計簿」フォルダ内の kakeibo_sync.json
 * - appDataFolder は使わず、通常の Drive 領域に置く（PCからも見えるように）
 */
class DriveHelper(private val context: Context) {

    companion object {
        private const val TAG = "DriveHelper"
        private const val SYNC_FILE_NAME = "kakeibo_sync.json"
        private const val SYNC_FOLDER_NAME = "家計簿"
        const val RC_SIGN_IN = 9001
    }

    private var driveService: Drive? = null

    // ==================== Google Sign-In ====================

    fun getSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent = getSignInClient().signInIntent

    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && !account.isExpired
    }

    fun getAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    suspend fun signOut() {
        withContext(Dispatchers.Main) {
            getSignInClient().signOut()
        }
    }

    /**
     * サインイン済みアカウントで Drive サービスを初期化
     */
    fun initDriveService(account: GoogleSignInAccount): Boolean {
        return try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE)
            )
            credential.selectedAccount = account.account
            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("KakeiboGX")
                .build()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Drive service init failed", e)
            false
        }
    }

    // ==================== Drive ファイル操作 ====================

    /**
     * 「家計簿」フォルダの ID を取得。なければ作成。
     */
    private suspend fun getOrCreateFolderId(): String = withContext(Dispatchers.IO) {
        val drive = driveService ?: throw IllegalStateException("Drive service is not initialized")
        // 既存フォルダを検索
        val result = drive.files().list()
            .setQ("name = '$SYNC_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        if (result.files.isNotEmpty()) {
            return@withContext result.files[0].id
        }

        // 作成
        val folderMetadata = com.google.api.services.drive.model.File().apply {
            name = SYNC_FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
        }
        val folder = drive.files().create(folderMetadata)
            .setFields("id")
            .execute()
        folder.id
    }

    /**
     * kakeibo_sync.json のファイル ID を取得。なければ null。
     */
    private suspend fun findSyncFileId(folderId: String): String? = withContext(Dispatchers.IO) {
        val drive = driveService ?: throw IllegalStateException("Drive service is not initialized")
        val result = drive.files().list()
            .setQ("name = '$SYNC_FILE_NAME' and '$folderId' in parents and trashed = false")
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()
        result.files.firstOrNull()?.id
    }

    /**
     * Drive から同期ファイルを読み込む。ファイルが存在しない場合は null。
     */
    suspend fun readSyncFile(): String? = withContext(Dispatchers.IO) {
        val drive = driveService ?: throw IllegalStateException("Drive service is not initialized")
        val folderId = getOrCreateFolderId()
        val fileId = findSyncFileId(folderId) ?: return@withContext null

        val outputStream = ByteArrayOutputStream()
        try {
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        } catch (e: HttpResponseException) {
            if (e.statusCode == 416) {
                return@withContext "" // 0バイトファイルの場合は416エラーになるため、空文字として扱う
            }
            throw e
        }
        outputStream.toString("UTF-8")
    }

    /**
     * Drive に同期ファイルを書き込む。ファイルが無ければ新規作成。
     */
    suspend fun writeSyncFile(jsonContent: String) = withContext(Dispatchers.IO) {
        val drive = driveService ?: throw IllegalStateException("Drive service is not initialized")
        val folderId = getOrCreateFolderId()
        val fileId = findSyncFileId(folderId)

        val content = ByteArrayContent.fromString("application/json", jsonContent)

        if (fileId != null) {
            // 既存ファイルを更新
            drive.files().update(fileId, null, content).execute()
        } else {
            // 新規作成
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = SYNC_FILE_NAME
                parents = listOf(folderId)
            }
            drive.files().create(fileMetadata, content)
                .setFields("id")
                .execute()
        }
        Log.d(TAG, "Sync file written successfully")
    }

    /**
     * Drive に CSVファイルを書き込む（既存更新 または 新規作成）
     */
    suspend fun writeCsvFile(csvContent: String): Boolean = withContext(Dispatchers.IO) {
        val drive = driveService ?: return@withContext false
        val folderId = getOrCreateFolderId()

        try {
            // 既存の本物CSVファイル（スプレッドシート化されていないもの）を検索
            val result = drive.files().list()
                .setQ("name = 'household_data.csv' and mimeType = 'text/csv' and '$folderId' in parents and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()
            val fileId = result.files.firstOrNull()?.id

            val content = ByteArrayContent.fromString("text/csv", csvContent)

            if (fileId != null) {
                drive.files().update(fileId, null, content).execute()
            } else {
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = "household_data.csv"
                    parents = listOf(folderId)
                }
                drive.files().create(fileMetadata, content).setFields("id").execute()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write CSV file", e)
            false
        }
    }
}

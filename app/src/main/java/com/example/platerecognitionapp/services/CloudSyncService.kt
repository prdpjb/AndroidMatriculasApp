package com.example.platerecognitionapp.services

import android.content.Context
import android.net.Uri
import com.example.platerecognitionapp.data.PlateDatabase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CloudSyncService(private val context: Context) {
    private val plateDatabase = PlateDatabase.getDatabase(context)
    private val dataBackupService = DataBackupService.getInstance(context)

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    private fun getDriveService(account: GoogleSignIn): Drive? {
        // Verificação de variáveis de ambiente
        val driveScope = System.getenv("DRIVE_SCOPE") ?: DriveScopes.DRIVE_FILE
        val appName = System.getenv("APP_NAME") ?: "PlateRecognitionApp"

        val credential = GoogleAccountCredential
            .usingOAuth2(context, listOf(driveScope))
            .setSelectedAccount(account.account)

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            JacksonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(appName)
            .build()
    }

    suspend fun syncToCloud(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Verificar login do Google
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext false

            val driveService = getDriveService(account) ?: return@withContext false

            // Criar backup local
            val tempFile = File(context.cacheDir, "temp_backup.json")
            val backupUri = Uri.fromFile(tempFile)
            
            val backupSuccess = dataBackupService.backupToFile(backupUri)
            if (!backupSuccess) return@withContext false

            // Criar arquivo no Google Drive
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = "plate_backup_${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}.json"
                parents = listOf("root")  // Pasta raiz do Drive
            }

            val fileContent = tempFile.readBytes()
            val mediaContent = com.google.api.client.http.InputStreamContent("application/json", fileContent.inputStream())

            driveService.Files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFromCloud(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Verificar login do Google
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext false

            val driveService = getDriveService(account) ?: return@withContext false

            // Baixar arquivo do Google Drive
            val outputStream = ByteArrayOutputStream()
            driveService.Files().get(fileId).executeMediaAndDownloadTo(outputStream)

            // Criar arquivo temporário
            val tempFile = File(context.cacheDir, "temp_restore.json")
            tempFile.writeBytes(outputStream.toByteArray())

            // Restaurar backup
            val backupUri = Uri.fromFile(tempFile)
            dataBackupService.restoreFromFile(backupUri)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun requestSignIn(): Intent {
        return googleSignInClient.signInIntent
    }

    companion object {
        fun getInstance(context: Context): CloudSyncService {
            return CloudSyncService(context.applicationContext)
        }
    }
}

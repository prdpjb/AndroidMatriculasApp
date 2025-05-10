package com.example.platerecognitionapp.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.platerecognitionapp.data.Plate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class DataEncryptionService(private val context: Context) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyGenParameterSpec(
                KeyGenParameterSpec.Builder(
                    MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE)
                .build()
            )
            .build()
    }

    private val encryptedSharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_plate_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun encryptPlate(plate: Plate): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = generateSecretKey()
        val iv = generateIV()
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        val encryptedData = cipher.doFinal(plate.plateNumber.toByteArray())
        
        // Salvar chave e IV de forma segura
        encryptedSharedPreferences.edit()
            .putString("${plate.id}_key", secretKey.encoded.toString())
            .putString("${plate.id}_iv", iv.toString())
            .apply()
        
        return encryptedData.toString()
    }

    fun decryptPlate(encryptedPlate: String, plateId: Int): String {
        val secretKeyBytes = encryptedSharedPreferences
            .getString("${plateId}_key", null)?.toByteArray()
        val ivBytes = encryptedSharedPreferences
            .getString("${plateId}_iv", null)?.toByteArray()
        
        if (secretKeyBytes == null || ivBytes == null) {
            throw SecurityException("Chave de descriptografia não encontrada")
        }
        
        val secretKey = SecretKey(secretKeyBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(ivBytes))
        
        val decryptedData = cipher.doFinal(encryptedPlate.toByteArray())
        return String(decryptedData)
    }

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    private fun generateIV(): ByteArray {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        return iv
    }

    suspend fun secureDeletePlate(plate: Plate) = withContext(Dispatchers.IO) {
        // Sobrescrever dados sensíveis antes de excluir
        val secureDelete = ByteArray(plate.plateNumber.length) { 0 }
        
        // Remover entradas de preferências seguras
        encryptedSharedPreferences.edit()
            .remove("${plate.id}_key")
            .remove("${plate.id}_iv")
            .apply()
    }

    companion object {
        fun getInstance(context: Context): DataEncryptionService {
            return DataEncryptionService(context.applicationContext)
        }
    }
}

package com.example.platerecognitionapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.platerecognitionapp.services.DataBackupService
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.example.platerecognitionapp.databinding.ActivitySettingsBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var dataBackupService: DataBackupService

    private val createBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { 
            lifecycleScope.launch {
                val success = dataBackupService.backupToFile(it)
                Toast.makeText(
                    this@SettingsActivity, 
                    if (success) "Backup criado com sucesso" else "Falha ao criar backup", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val restoreBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { 
            lifecycleScope.launch {
                val success = dataBackupService.restoreFromFile(it)
                Toast.makeText(
                    this@SettingsActivity, 
                    if (success) "Backup restaurado com sucesso" else "Falha ao restaurar backup", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataBackupService = DataBackupService.getInstance(this)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var dataBackupService: DataBackupService

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            dataBackupService = DataBackupService.getInstance(requireContext())

            findPreference<Preference>("create_backup")?.setOnPreferenceClickListener {
                (activity as? SettingsActivity)?.createBackup()
                true
            }

            findPreference<Preference>("restore_backup")?.setOnPreferenceClickListener {
                (activity as? SettingsActivity)?.restoreBackup()
                true
            }
        }
    }

    fun createBackup() {
        val fileName = dataBackupService.generateDefaultBackupFileName()
        createBackupLauncher.launch(fileName)
    }

    fun restoreBackup() {
        restoreBackupLauncher.launch(arrayOf("application/json"))
    }

    companion object {
        fun isAutoSaveEnabled(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("auto_save", true)
        }

        fun isNotificationEnabled(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("notifications", true)
        }

        fun isBiometricAuthEnabled(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("biometric_auth", false)
        }

        fun isDataEncryptionEnabled(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("data_encryption", true)
        }
    }
}

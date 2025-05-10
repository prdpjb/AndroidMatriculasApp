package com.example.platerecognitionapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.platerecognitionapp.security.BiometricAuthService
import com.example.platerecognitionapp.SettingsActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.platerecognitionapp.data.PlateDatabase
import com.example.platerecognitionapp.databinding.ActivityHistoryBinding
import com.example.platerecognitionapp.ui.PlateHistoryAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var plateDatabase: PlateDatabase
    private lateinit var plateHistoryAdapter: PlateHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verificar autenticação biométrica
        if (SettingsActivity.isBiometricAuthEnabled(this)) {
            val biometricAuthService = BiometricAuthService.getInstance(this)
            biometricAuthService.authenticateUser(
                this,
                onSuccess = { setupHistoryView() },
                onError = { finish() }
            )
        } else {
            setupHistoryView()
        }
    }

    private fun setupHistoryView() {
        plateDatabase = PlateDatabase.getDatabase(this)
        plateHistoryAdapter = PlateHistoryAdapter()

        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = plateHistoryAdapter
        }

        // Observar lista de matrículas
        lifecycleScope.launch(Dispatchers.Main) {
            plateDatabase.plateDao().getAllPlates().collectLatest { plates ->
                plateHistoryAdapter.submitList(plates)
            }
        }

        // Limpar histórico
        binding.clearHistoryButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                plateDatabase.plateDao().clearAll()
            }
        }
    }
}

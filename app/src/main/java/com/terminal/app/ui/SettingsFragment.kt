package com.terminal.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.slider.Slider
import com.terminal.app.databinding.FragmentSettingsBinding
import java.io.*
import java.util.zip.ZipInputStream

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: android.content.SharedPreferences

    private val zipPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                extractZipToHome(uri)
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
        prefs = requireContext().getSharedPreferences("terminal_prefs", Context.MODE_PRIVATE)
        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        val wakeLockEnabled = prefs.getBoolean("wake_lock", false)
        val temperature = prefs.getFloat("temperature_limit", 45f)
        val memory = prefs.getFloat("memory_limit", 512f)
        val storage = prefs.getFloat("storage_limit", 2f)

        binding.wakeLockSwitch.isChecked = wakeLockEnabled
        binding.temperatureSlider.value = temperature
        binding.memorySlider.value = memory
        binding.storageSlider.value = storage

        updateTemperatureText(temperature.toInt())
        updateMemoryText(memory.toInt())
        updateStorageText(storage.toInt())

        val zipLoaded = prefs.getString("zip_loaded", null)
        binding.zipStatus.text = zipLoaded ?: "No file loaded"
    }

    private fun setupListeners() {
        binding.wakeLockSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("wake_lock", isChecked).apply()
            if (isChecked) {
                acquireWakeLock()
            } else {
                releaseWakeLock()
            }
        }

        binding.temperatureSlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                prefs.edit().putFloat("temperature_limit", value).apply()
                updateTemperatureText(value.toInt())
            }
        }

        binding.memorySlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                prefs.edit().putFloat("memory_limit", value).apply()
                updateMemoryText(value.toInt())
            }
        }

        binding.storageSlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                prefs.edit().putFloat("storage_limit", value).apply()
                updateStorageText(value.toInt())
            }
        }

        binding.loadZipButton.setOnClickListener {
            openZipPicker()
        }
    }

    private fun updateTemperatureText(value: Int) {
        binding.temperatureValue.text = "$value°C"
    }

    private fun updateMemoryText(value: Int) {
        binding.memoryValue.text = "$value MB"
    }

    private fun updateStorageText(value: Int) {
        binding.storageValue.text = "$value GB"
    }

    private fun acquireWakeLock() {
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "TerminalApp::WakeLock"
        )
        wakeLock.acquire(10 * 60 * 1000L) // 10 minutes
        Toast.makeText(context, "Wake Lock acquired", Toast.LENGTH_SHORT).show()
    }

    private fun releaseWakeLock() {
        Toast.makeText(context, "Wake Lock released", Toast.LENGTH_SHORT).show()
    }

    private fun openZipPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
        }
        zipPicker.launch(intent)
    }

    private fun extractZipToHome(uri: Uri) {
        val homeDir = File("/data/data/com.terminal.app/files/home")
        if (!homeDir.exists()) {
            homeDir.mkdirs()
        }

        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipInputStream ->
                    var entry = zipInputStream.nextEntry
                    while (entry != null) {
                        val file = File(homeDir, entry.name)

                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { outputStream ->
                                zipInputStream.copyTo(outputStream)
                            }
                        }

                        zipInputStream.closeEntry()
                        entry = zipInputStream.nextEntry
                    }
                }
            }

            val fileName = uri.lastPathSegment ?: "unknown.zip"
            prefs.edit().putString("zip_loaded", fileName).apply()
            binding.zipStatus.text = "Extracted: $fileName"
            Toast.makeText(context, "ZIP extracted successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Error extracting ZIP: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

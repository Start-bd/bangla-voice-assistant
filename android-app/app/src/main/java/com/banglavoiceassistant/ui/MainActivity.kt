package com.banglavoiceassistant.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.banglavoiceassistant.R
import com.banglavoiceassistant.data.RetrofitClient
import com.banglavoiceassistant.service.FloatingBubbleService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var startBubbleButton: Button
    private lateinit var stopBubbleButton: Button
    private lateinit var testBackendButton: Button
    private lateinit var settingsButton: Button
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            checkOverlayPermissionAndStart()
        } else {
            Toast.makeText(this, "Permissions required for voice assistant", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
    }
    
    private fun initViews() {
        startBubbleButton = findViewById(R.id.start_bubble_button)
        stopBubbleButton = findViewById(R.id.stop_bubble_button)
        testBackendButton = findViewById(R.id.test_backend_button)
        settingsButton = findViewById(R.id.settings_button)
    }
    
    private fun setupClickListeners() {
        startBubbleButton.setOnClickListener {
            requestPermissionsAndStart()
        }
        
        stopBubbleButton.setOnClickListener {
            stopBubbleService()
        }
        
        testBackendButton.setOnClickListener {
            testBackendConnection()
        }
        
        settingsButton.setOnClickListener {
            openSettings()
        }
    }
    
    private fun requestPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Add any Android 13+ specific permissions if needed
        }
        
        permissionLauncher.launch(permissions.toTypedArray())
    }
    
    private fun checkOverlayPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                AlertDialog.Builder(this)
                    .setTitle("Overlay Permission Required")
                    .setMessage("This app needs permission to draw over other apps to show the floating bubble.")
                    .setPositiveButton("Grant") { _, _ ->
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                startBubbleService()
            }
        } else {
            startBubbleService()
        }
    }
    
    private fun startBubbleService() {
        val intent = Intent(this, FloatingBubbleService::class.java).apply {
            action = FloatingBubbleService.ACTION_SHOW_BUBBLE
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        Toast.makeText(this, "Voice assistant started!", Toast.LENGTH_SHORT).show()
        finish() // Close activity so user can see the bubble
    }
    
    private fun stopBubbleService() {
        val intent = Intent(this, FloatingBubbleService::class.java).apply {
            action = FloatingBubbleService.ACTION_STOP_SERVICE
        }
        stopService(intent)
        Toast.makeText(this, "Voice assistant stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun testBackendConnection() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.healthCheck()
                if (response.isSuccessful) {
                    val body = response.body()
                    val status = body?.get("status") as? String ?: "unknown"
                    Toast.makeText(this@MainActivity, "Backend: $status", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Backend error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun openSettings() {
        // TODO: Implement settings activity
        Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startBubbleService()
                } else {
                    Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check if service is running and update UI
        updateButtonStates()
    }
    
    private fun updateButtonStates() {
        // TODO: Check if service is running
    }
    
    private fun isServiceRunning(): Boolean {
        // TODO: Implement service running check
        return false
    }
}

private const val REQUEST_OVERLAY_PERMISSION = 1001

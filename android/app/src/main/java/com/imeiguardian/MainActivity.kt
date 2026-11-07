package com.imeiguardian

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            ContextCompat.startForegroundService(this, Intent(this, LocationService::class.java))
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("IMEI Guardian", style = MaterialTheme.typography.headlineLarge)
                    Text("Consent-based location sharing", style = MaterialTheme.typography.titleMedium)
                    Text("Sign in and link this phone to your owner account before enabling location updates. Location sharing requires your permission.")
                    Button(onClick = { requestLocation() }) { Text("Allow location and start service") }
                    OutlinedButton(onClick = { stopService(Intent(this@MainActivity, LocationService::class.java)) }) { Text("Stop location sharing") }
                    Text("This starter build demonstrates runtime permission flow and a visible foreground notification. Complete authenticated account linking and API upload before using it as a tracker.")
                }
            }
        }
    }
    private fun requestLocation() {
        val p = mutableListOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) p += Manifest.permission.POST_NOTIFICATIONS
        permissions.launch(p.toTypedArray())
    }
}
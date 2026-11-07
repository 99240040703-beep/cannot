package com.imeiguardian

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val http = OkHttpClient()
    private val jsonType = "application/json".toMediaType()
    private var pendingStart = false
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted && pendingStart) startServiceNow()
        pendingStart = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("guardian", MODE_PRIVATE)
        setContent {
            var name by remember { mutableStateOf("") }
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var phoneName by remember { mutableStateOf("My Android phone") }
            var imei by remember { mutableStateOf("") }
            var token by remember { mutableStateOf(prefs.getString("token", "") ?: "") }
            var deviceId by remember { mutableStateOf(prefs.getString("deviceId", "") ?: "") }
            var status by remember { mutableStateOf(if (token.isNotBlank()) "Signed in. Register or link this phone." else "Create an account or sign in.") }
            var busy by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            MaterialTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("IMEI Guardian", style = MaterialTheme.typography.headlineLarge)
                    Text("Consent-based lost phone recovery", style = MaterialTheme.typography.titleMedium)
                    Text(status, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your name (for new account)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password (10+ characters)") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !busy, onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    val body = JSONObject().put("name", name).put("email", email).put("password", password)
                                    val data = withContext(Dispatchers.IO) { callApi("/api/auth/register", body, null) }
                                    token = data.getString("token"); prefs.edit().putString("token", token).apply()
                                    status = "Account created and signed in."
                                } catch (e: Exception) { status = e.message ?: "Registration failed." }
                                busy = false
                            }
                        }) { Text("Create account") }
                        OutlinedButton(enabled = !busy, onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    val body = JSONObject().put("email", email).put("password", password)
                                    val data = withContext(Dispatchers.IO) { callApi("/api/auth/login", body, null) }
                                    token = data.getString("token"); prefs.edit().putString("token", token).apply()
                                    status = "Signed in successfully."
                                } catch (e: Exception) { status = e.message ?: "Sign-in failed." }
                                busy = false
                            }
                        }) { Text("Sign in") }
                    }
                    Divider()
                    Text("Register this phone", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(value = phoneName, onValueChange = { phoneName = it }, label = { Text("Device name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = imei, onValueChange = { imei = it.filter(Char::isDigit).take(15) }, label = { Text("15-digit IMEI (enter manually)") }, modifier = Modifier.fillMaxWidth())
                    Button(enabled = !busy && token.isNotBlank() && imei.length == 15, onClick = {
                        busy = true
                        scope.launch {
                            try {
                                val body = JSONObject().put("name", phoneName).put("imei", imei)
                                val data = withContext(Dispatchers.IO) { callApi("/api/devices", body, token) }
                                deviceId = data.getJSONObject("device").getString("id")
                                prefs.edit().putString("deviceId", deviceId).apply()
                                status = "Phone linked to your account. You can start visible location sharing."
                            } catch (e: Exception) { status = e.message ?: "Device registration failed." }
                            busy = false
                        }
                    }) { Text("Register and link phone") }
                    Text("Linked device: " + if (deviceId.isBlank()) "None" else deviceId.take(8) + "…")
                    Button(enabled = token.isNotBlank() && deviceId.isNotBlank(), onClick = { pendingStart = true; requestLocationPermissions() }) { Text("Start location sharing") }
                    OutlinedButton(onClick = { stopService(Intent(this@MainActivity, LocationService::class.java)); status = "Location sharing stopped." }) { Text("Stop location sharing") }
                    Text("Location updates are sent to your account over HTTPS while the service is active. Android may require you to allow background location separately in Settings. A persistent notification remains visible.")
                    Text("IMEI does not locate phones by itself. This app cannot send updates when the phone is powered off or has no network.")
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        val p = mutableListOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) p += Manifest.permission.POST_NOTIFICATIONS
        permissions.launch(p.toTypedArray())
    }

    private fun startServiceNow() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) ContextCompat.startForegroundService(this, Intent(this, LocationService::class.java))
    }

    private fun callApi(path: String, body: JSONObject, bearer: String?): JSONObject {
        val request = Request.Builder().url(BuildConfig.API_BASE_URL.trimEnd('/') + path)
            .post(body.toString().toRequestBody(jsonType))
            .apply { if (!bearer.isNullOrBlank()) header("Authorization", "Bearer " + bearer) }.build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException(JSONObject(text.ifBlank { "{}" }).optString("error", "API request failed: " + response.code))
            return JSONObject(text)
        }
    }
}
package com.imeiguardian

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.concurrent.thread

class LocationService : Service() {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val http = OkHttpClient()
    private val jsonType = "application/json".toMediaType()
    private val prefs by lazy { getSharedPreferences("guardian", MODE_PRIVATE) }

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val token = prefs.getString("token", "").orEmpty()
            val id = prefs.getString("deviceId", "").orEmpty()
            if (token.isBlank() || id.isBlank()) return
            val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
            val battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
            val payload = JSONObject().put("latitude", location.latitude).put("longitude", location.longitude)
                .put("accuracy", location.accuracy.toDouble()).put("battery", battery).put("source", "android-companion")
            thread {
                post("/api/devices/" + id + "/location", payload, token)
                post("/api/devices/" + id + "/heartbeat", JSONObject().put("battery", battery), token)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("tracking", "Location sharing", NotificationManager.IMPORTANCE_LOW))
        val notification = NotificationCompat.Builder(this, "tracking")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("IMEI Guardian location sharing is active")
            .setContentText("Your device is sharing location with your own account. Tap Stop in the app to end sharing.")
            .setOngoing(true).build()
        startForeground(1001, notification)
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) { stopSelf(); return }
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5 * 60 * 1000L)
            .setMinUpdateIntervalMillis(2 * 60 * 1000L).build()
        try { fused.requestLocationUpdates(request, callback, mainLooper) } catch (_: SecurityException) { stopSelf() }
    }

    private fun post(path: String, payload: JSONObject, token: String) {
        try {
            val request = Request.Builder().url(BuildConfig.API_BASE_URL.trimEnd('/') + path)
                .header("Authorization", "Bearer " + token)
                .post(payload.toString().toRequestBody(jsonType)).build()
            http.newCall(request).execute().close()
        } catch (_: Exception) {
            // No location is fabricated or queued as current if the network is unavailable.
        }
    }

    override fun onDestroy() { fused.removeLocationUpdates(callback); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
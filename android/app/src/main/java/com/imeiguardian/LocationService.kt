package com.imeiguardian

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult

/**
 * Visible foreground location service starter.
 * Before production use, add account sign-in, device linking and a revocable device credential,
 * then POST each permitted location to /api/devices/{id}/location over HTTPS.
 * Never hide the persistent notification or collect location before consent.
 */
class LocationService : Service() {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            // TODO: Upload result.lastLocation with authenticated device credentials.
            // Do not log GPS coordinates or store them unencrypted on the device.
        }
    }
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("tracking", "Location sharing", NotificationManager.IMPORTANCE_LOW))
        val notification = NotificationCompat.Builder(this, "tracking")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("IMEI Guardian location sharing is active")
            .setContentText("Your device is sharing location with your own account.")
            .setOngoing(true).build()
        startForeground(1001, notification)
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15 * 60 * 1000L)
            .setMinUpdateIntervalMillis(5 * 60 * 1000L).build()
        try { fused.requestLocationUpdates(request, callback, mainLooper) } catch (_: SecurityException) { stopSelf() }
    }
    override fun onDestroy() { fused.removeLocationUpdates(callback); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
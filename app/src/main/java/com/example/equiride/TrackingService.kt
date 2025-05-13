package com.example.equiride

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.equiride.AppDatabase
import com.example.equiride.Ride
import com.google.android.gms.location.*

class TrackingService : Service() {

    companion object {
        private const val CHANNEL_ID = "equiride_tracking"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START       = "com.example.equiride.ACTION_START_TRACKING"
        const val ACTION_STOP        = "com.example.equiride.ACTION_STOP_TRACKING"
        const val ACTION_LOCATION    = "com.example.equiride.ACTION_LOCATION"
        const val EXTRA_HORSE_ID     = "horseId"
        const val EXTRA_LAT          = "lat"
        const val EXTRA_LON          = "lon"
        const val EXTRA_SPEED        = "speed"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var horseId: Long = 0L
    private val segments = mutableListOf<Pair<Location, Long>>() // (loc, timestamp)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_START -> {
                    horseId = intent.getLongExtra(EXTRA_HORSE_ID, 0L)
                    startForeground(NOTIFICATION_ID, buildNotification())
                    startLocationUpdates()
                }
                ACTION_STOP -> {
                    stopLocationUpdates()
                    saveRide()
                    stopForeground(true)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { loc ->
                    // uložíme bod
                    segments.add(loc to loc.time)
                    // broadcast do Activity
                    Intent(ACTION_LOCATION).also { i ->
                        i.putExtra(EXTRA_LAT, loc.latitude)
                        i.putExtra(EXTRA_LON, loc.longitude)
                        i.putExtra(EXTRA_SPEED, loc.speed.toDouble())
                        sendBroadcast(i)
                    }
                }
            }
        }
    }

    private fun startLocationUpdates() {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateDistanceMeters(1f)
            .build()
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(req, locationCallback, mainLooper)
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun saveRide() {
        if (segments.size < 2) return

        // GeoJSON
        val coords = org.json.JSONArray().also { arr ->
            segments.forEach { (loc, _) ->
                arr.put(org.json.JSONArray().put(loc.longitude).put(loc.latitude))
            }
        }
        val startTime = segments.first().second
        val endTime   = segments.last().second
        val durationSec = (endTime - startTime) / 1000

        // vzdálenost
        var totalDist = 0.0
        for (i in 1 until segments.size) {
            val prev = segments[i-1].first
            val curr = segments[i].first
            totalDist += prev.distanceTo(curr).toDouble()
        }

        // proporce chodu (pokud chceš, můžeš doplnit podle rychlosti)
        val pts = segments.size.toDouble().coerceAtLeast(1.0)
        val walkP   = 0.0
        val trotP   = 0.0
        val gallP   = 0.0

        val ride = Ride(
            horseId       = horseId,
            timestamp     = startTime,
            durationSeconds   = durationSec,
            distance      = totalDist,
            walkPortion   = walkP,
            trotPortion   = trotP,
            gallopPortion = gallP,
            geoJson       = org.json.JSONObject().apply {
                put("type", "LineString")
                put("coordinates", coords)
                put("timestamp", endTime)
            }.toString()
        )
        AppDatabase.get(this).rideDao().insert(ride)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_STOP }
        val pStop = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EquiRide – záznam jízdy")
            .setContentText("Klepni pro ukončení")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pStop)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(CHANNEL_ID, "EquiRide Tracking", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(chan)
        }
    }

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }
}

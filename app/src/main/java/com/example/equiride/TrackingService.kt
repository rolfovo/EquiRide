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
import com.google.android.gms.location.*
import org.json.JSONArray
import org.json.JSONObject

class TrackingService : Service() {

    companion object {
        private const val CHANNEL_ID = "equiride_tracking"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START    = "com.example.equiride.ACTION_START_TRACKING"
        const val ACTION_STOP     = "com.example.equiride.ACTION_STOP_TRACKING"
        const val ACTION_LOCATION = "com.example.equiride.ACTION_LOCATION"
        const val EXTRA_HORSE_ID  = "horseId"
        const val EXTRA_LAT       = "lat"
        const val EXTRA_LON       = "lon"
        const val EXTRA_SPEED     = "speed"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var horseId: Long = 0L
    private lateinit var horse: Horse
    private val segments = mutableListOf<Pair<Location, Long>>()

    override fun onBind(intent: Intent?) = null

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
                    // Načteme si koně z DB
                    horse = AppDatabase.get(this).horseDao().getById(horseId)
                        ?: throw IllegalStateException("Horse not found")
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
                    segments.add(loc to loc.time)
                    Intent(ACTION_LOCATION).apply {
                        putExtra(EXTRA_LAT, loc.latitude)
                        putExtra(EXTRA_LON, loc.longitude)
                        putExtra(EXTRA_SPEED, loc.speed.toDouble())
                        sendBroadcast(this)
                    }
                }
            }
        }
    }

    private fun startLocationUpdates() {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateDistanceMeters(1f)
            .build()
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(req, locationCallback, mainLooper)
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun saveRide() {
        if (segments.size < 2) return

        // 1) GeoJSON
        val coords = JSONArray().also { arr ->
            segments.forEach { (loc, _) ->
                arr.put(JSONArray().put(loc.longitude).put(loc.latitude))
            }
        }

        // časy a trvání
        val startTime = segments.first().second
        val endTime   = segments.last().second
        val durationSec = (endTime - startTime) / 1000

        // vzdálenost
        var totalDist = 0.0
        for (i in 1 until segments.size) {
            val prev = segments[i - 1].first
            val curr = segments[i].first
            totalDist += prev.distanceTo(curr).toDouble()
        }

        // poměry chodu podle rychlostních prahů z horse
        val totalPts = segments.size.toDouble().coerceAtLeast(1.0)
        val standCnt = segments.count {
            (it.first.speed * 3.6) < horse.walkSpeed * 0.8
        }
        val walkCnt = segments.count {
            val k = it.first.speed * 3.6
            k >= horse.walkSpeed * 0.8 && k < horse.walkSpeed
        }
        val trotCnt = segments.count {
            val k = it.first.speed * 3.6
            k >= horse.walkSpeed && k < horse.trotSpeed
        }
        val gallCnt = segments.count {
            (it.first.speed * 3.6) >= horse.trotSpeed
        }

        val ride = Ride(
            horseId        = horseId,
            timestamp      = startTime,
            durationSeconds= durationSec,
            distance       = totalDist,
            walkPortion    = walkCnt  / totalPts,
            trotPortion    = trotCnt  / totalPts,
            gallopPortion  = gallCnt  / totalPts,
            geoJson        = JSONObject().apply {
                put("type", "LineString")
                put("coordinates", coords)
                put("timestamp", endTime)
            }.toString()
        )
        AppDatabase.get(this).rideDao().insert(ride)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, TrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val pStop = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EquiRide – záznam jízdy")
            .setContentText("Klepni pro ukončení")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                pStop
            )
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID,
                "EquiRide Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).let { chan ->
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(chan)
            }
        }
    }

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }
}

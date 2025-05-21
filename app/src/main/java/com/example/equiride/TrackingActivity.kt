package com.example.equiride

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.equiride.databinding.ActivityTrackingBinding
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class TrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrackingBinding
    private var horseId: Long = 0L
    private var rideStartTime: Long = 0L
    private lateinit var horse: Horse
    private val db by lazy { AppDatabase.get(this) }

    private val segments = mutableListOf<Pair<GeoPoint, Double>>()
    private var speedSumKph = 0.0
    private var speedCount = 0

    // pro aktualizaci doby
    private val uiHandler = Handler(Looper.getMainLooper())
    private val durationUpdater = object : Runnable {
        override fun run() {
            updateDuration()
            uiHandler.postDelayed(this, 1000)
        }
    }

    private lateinit var locReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // načtení
        horseId = intent.getLongExtra("horseId", 0L)
        horse   = db.horseDao().getById(horseId)
            ?: throw IllegalStateException("Horse not found")

        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getPreferences(Context.MODE_PRIVATE))

        binding = ActivityTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // spustit měřič času
        rideStartTime = System.currentTimeMillis()
        uiHandler.post(durationUpdater)

        // mapa
        binding.mapview.apply {
            setMultiTouchControls(true)
            controller.setZoom(18.0)
        }
        binding.tvHorseName.text = "Kůň: ${horse.name}"

        // broadcast listener
        locReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == TrackingService.ACTION_LOCATION) {
                    val lat   = intent.getDoubleExtra(TrackingService.EXTRA_LAT, 0.0)
                    val lon   = intent.getDoubleExtra(TrackingService.EXTRA_LON, 0.0)
                    val speed = intent.getDoubleExtra(TrackingService.EXTRA_SPEED, 0.0)
                    onNewLocation(lat, lon, speed)
                }
            }
        }
        registerReceiver(locReceiver, IntentFilter(TrackingService.ACTION_LOCATION))

        // FAB centrování
        binding.fabCenter.setOnClickListener {
            segments.lastOrNull()?.first?.let {
                binding.mapview.controller.animateTo(it)
            }
        }

        // požádat o práva & spustit službu
        val req = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) startTrackingService()
            else Toast.makeText(this, "GPS permission denied", Toast.LENGTH_LONG).show()
        }

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            req.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            startTrackingService()
        }

        binding.btnStop.setOnClickListener {
            stopAndSave()
        }
    }

    private fun startTrackingService() {
        Intent(this, TrackingService::class.java).also {
            it.action = TrackingService.ACTION_START
            it.putExtra(TrackingService.EXTRA_HORSE_ID, horseId)
            ContextCompat.startForegroundService(this, it)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapview.onResume()
    }
    override fun onPause() {
        super.onPause()
        binding.mapview.onPause()
        uiHandler.removeCallbacks(durationUpdater)
    }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(locReceiver)
    }

    private fun onNewLocation(lat: Double, lon: Double, speedMps: Double) {
        val pt = GeoPoint(lat, lon)
        // přidat bod, vykreslit a centrovat
        segments.add(pt to speedMps)
        drawSegments()
        showCurrentMarker(pt)
        binding.mapview.controller.animateTo(pt)

        // aktualizovat statistiky a barevný proužek
        updateStats(speedMps)
    }

    private fun drawSegments() {
        binding.mapview.overlays.removeAll { it is Polyline }
        segments.zipWithNext { (p0,_),(p1,s1) ->
            val kph = s1*3.6
            val col = when {
                kph < horse.walkSpeed*0.8 -> 0xFF888888.toInt()
                kph < horse.walkSpeed      -> 0xFF006400.toInt()
                kph < horse.trotSpeed      -> 0xFFB59F00.toInt()
                else                        -> 0xFF8B0000.toInt()
            }
            Polyline().apply {
                addPoint(p0); addPoint(p1)
                outlinePaint.strokeWidth = 8f
                outlinePaint.color = col
            }.also { binding.mapview.overlays.add(it) }
        }
        binding.mapview.invalidate()
    }

    private var currentMarker: Marker? = null
    private fun showCurrentMarker(p: GeoPoint) {
        currentMarker?.let { binding.mapview.overlays.remove(it) }
        Marker(binding.mapview).apply {
            position = p
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(
                this@TrackingActivity,
                org.osmdroid.library.R.drawable.marker_default
            )
        }.also {
            currentMarker = it
            binding.mapview.overlays.add(it)
        }
        binding.mapview.invalidate()
    }

    private fun updateStats(latestSpeedMps: Double) {
        // 1) Vzdálenost
        var dist = 0.0
        for (i in 1 until segments.size) {
            dist += segments[i - 1].first.distanceToAsDouble(segments[i].first)
        }
        // 2) Rychlosti
        val instKph = latestSpeedMps * 3.6
        speedSumKph += instKph
        speedCount++
        val avgKph = speedSumKph / speedCount
        binding.tvInstantSpeed.text = "Rychlost: ${"%.1f".format(instKph)} km/h"
        binding.tvAverageSpeed.text = "Průměr: ${"%.1f".format(avgKph)} km/h"
        // 3) Chod
        val gait = when {
            instKph < horse.walkSpeed * 0.8 -> "Stojí"
            instKph < horse.walkSpeed       -> "Krok"
            instKph < horse.trotSpeed       -> "Klus"
            else                              -> "Cval"
        }
        binding.tvCurrentGait.text = "Chod: $gait"
        // 4) Text stats
        binding.tvStats.text = "Body: ${segments.size}  Vzdálenost: ${"%.1f".format(dist)} m"
        // 5) Barevný proužek
        val total = segments.size.toFloat().coerceAtLeast(1f)
        listOf(
            binding.barStand  to segments.count { classifySpeed(it.second) == RideType.STAND },
            binding.barWalk   to segments.count { classifySpeed(it.second) == RideType.WALK },
            binding.barTrot   to segments.count { classifySpeed(it.second) == RideType.TROT },
            binding.barGallop to segments.count { classifySpeed(it.second) == RideType.GALLOP }
        ).forEach { (view, cnt) ->
            val lp = view.layoutParams as LinearLayout.LayoutParams
            lp.weight = cnt.toFloat() / total
            view.layoutParams = lp
        }
    }

    private fun updateDuration() {
        val elapsed = System.currentTimeMillis() - rideStartTime
        val m = (elapsed/1000/60).toInt()
        val s = (elapsed/1000%60).toInt()
        binding.tvDuration.text = String.format("Doba: %02d:%02d", m, s)
    }

    private fun stopAndSave() {
        // zastavit službu
        Intent(this, TrackingService::class.java).also {
            it.action = TrackingService.ACTION_STOP
            startService(it)
        }
        // uložit a přejít na statistiky
        val rideEnd = System.currentTimeMillis()
        val durationSec = (rideEnd - rideStartTime)/1000
        var totalDist = 0.0
        for (i in 1 until segments.size) totalDist += segments[i-1].first.distanceToAsDouble(segments[i].first)
        val pts = segments.size.toDouble().coerceAtLeast(1.0)
        val walkP  = segments.count{ classifySpeed(it.second)==RideType.WALK }/pts
        val trotP  = segments.count{ classifySpeed(it.second)==RideType.TROT }/pts
        val gallP  = segments.count{ classifySpeed(it.second)==RideType.GALLOP }/pts
        val standP = segments.count{ classifySpeed(it.second)==RideType.STAND }/pts
        db.rideDao().insert(
            Ride(horseId=horseId, timestamp=rideStartTime,durationSeconds=durationSec,
                distance=totalDist,walkPortion=walkP,trotPortion=trotP,gallopPortion=gallP,
                geoJson=JSONObject().apply{
                    put("type","LineString");put("coordinates",
                    JSONArray().also{arr->segments.forEach{(p,_)->arr.put(JSONArray().put(p.longitude).put(p.latitude))}});
                    put("timestamp",rideEnd)
                }.toString()
            )
        )
        startActivity(Intent(this, StatsActivity::class.java).putExtra("horseId",horseId))
        finish()
    }

    private fun classifySpeed(mps: Double): RideType {
        val kph = mps*3.6
        if(kph<1.0) return RideType.STAND
        return when {
            kph < horse.walkSpeed*0.8 -> RideType.STAND
            kph < horse.walkSpeed      -> RideType.WALK
            kph < horse.trotSpeed      -> RideType.TROT
            else                        -> RideType.GALLOP
        }
    }

    private enum class RideType { STAND, WALK, TROT, GALLOP }
}
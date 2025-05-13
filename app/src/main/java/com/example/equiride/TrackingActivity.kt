package com.example.equiride

import android.Manifest
import android.content.Intent
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.google.android.gms.location.*

class TrackingActivity : AppCompatActivity() {

    /** Jednoduchý Kalmanův filtr pro 1D (latitude nebo longitude) */
    class SimpleKalmanFilter(var q: Double, var r: Double) {
        private var x = 0.0
        private var p = 1.0
        fun init(value: Double) { x = value; p = 1.0 }
        fun filter(meas: Double): Double {
            p += q
            val k = p / (p + r)
            x += k * (meas - x)
            p *= (1 - k)
            return x
        }
    }

    private lateinit var binding: ActivityTrackingBinding
    private var horseId: Long = 0L
    private var rideStartTime: Long = 0L
    private lateinit var horse: Horse
    private val db by lazy { AppDatabase.get(this) }

    private val segments = mutableListOf<Pair<GeoPoint, Double>>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Kalmanovy filtry
    private val latFilter = SimpleKalmanFilter(q = 0.001, r = 5.0)
    private val lonFilter = SimpleKalmanFilter(q = 0.001, r = 5.0)
    private var kalmanInited = false
    private var fixCount = 0

    private val MIN_DIST_THRESHOLD = 0.5
    private val MAX_VALID_SPEED_KPH = 40.0
    private var firstFix = true
    private var currentPosition: GeoPoint? = null

    // Pro výpočet okamžité a průměrné rychlosti
    private var speedSumKph = 0.0
    private var speedCount = 0

    // Handler pro pravidelnou aktualizaci doby
    private val uiHandler = Handler(Looper.getMainLooper())
    private val durationUpdater = object : Runnable {
        override fun run() {
            updateDuration()
            uiHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        horseId = intent.getLongExtra("horseId", 0L)
        horse = db.horseDao().getById(horseId) ?: throw IllegalStateException("Horse not found")

        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))

        binding = ActivityTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rideStartTime = System.currentTimeMillis()
        uiHandler.post(durationUpdater)

        // Inicializace mapy
        binding.mapview.setMultiTouchControls(true)
        binding.mapview.controller.setZoom(18.0)
        binding.tvHorseName.text = "Kůň: ${horse.name}"

        // Středící tlačítko
        binding.fabCenter.setOnClickListener { currentPosition?.let { binding.mapview.controller.animateTo(it) } }

        // GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    fixCount++
                    if (!kalmanInited && fixCount >= 5) {
                        latFilter.init(loc.latitude)
                        lonFilter.init(loc.longitude)
                        kalmanInited = true
                    }
                    if (kalmanInited) {
                        latFilter.r = loc.accuracy.toDouble()
                        lonFilter.r = loc.accuracy.toDouble()
                        val smoothLat = latFilter.filter(loc.latitude)
                        val smoothLon = lonFilter.filter(loc.longitude)
                        onNewLocationFiltered(smoothLat, smoothLon, loc.speed.toDouble())
                    }
                }
            }
        }
        // Žádost o práva
        val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startLocationUpdates() else Toast.makeText(this, "Bez GPS nelze sledovat trasu", Toast.LENGTH_LONG).show()
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else startLocationUpdates()

        binding.btnStop.setOnClickListener { stopAndSave() }
    }

    override fun onResume() {
        super.onResume()
        binding.mapview.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapview.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        uiHandler.removeCallbacks(durationUpdater)
    }

    private fun startLocationUpdates() {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateDistanceMeters(0f).build()
        fusedLocationClient.requestLocationUpdates(req, locationCallback, mainLooper)
    }

    private fun onNewLocationFiltered(lat: Double, lon: Double, speedMps: Double) {
        val pt = GeoPoint(lat, lon)
        currentPosition = pt
        if (segments.isNotEmpty()) {
            val last = segments.last().first
            val d = last.distanceToAsDouble(pt)
            val kph = speedMps * 3.6
            if (kph > MAX_VALID_SPEED_KPH || d < MIN_DIST_THRESHOLD) return
        }
        segments.add(pt to speedMps)
        drawSegments()
        showCurrentMarker(pt)
        if (firstFix) { binding.mapview.controller.animateTo(pt); firstFix = false }
        updateStats(speedMps)
    }

    private fun drawSegments() {
        binding.mapview.overlays.removeAll { it is Polyline }
        segments.zipWithNext { (p0, _), (p1, s1) ->
            val kph = s1 * 3.6
            val color = when {
                kph < horse.walkSpeed * 0.8 -> 0xFF888888.toInt()
                kph < horse.walkSpeed       -> 0xFF006400.toInt()
                kph < horse.trotSpeed       -> 0xFFB59F00.toInt()
                else                         -> 0xFF8B0000.toInt()
            }
            Polyline().apply {
                addPoint(p0); addPoint(p1)
                outlinePaint.strokeWidth = 10f
                outlinePaint.color = color
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
            icon = ContextCompat.getDrawable(this@TrackingActivity, org.osmdroid.library.R.drawable.marker_default)
            title = "Teď tady"
        }.also { currentMarker = it; binding.mapview.overlays.add(it) }
        binding.mapview.invalidate()
    }

    private fun updateStats(latestSpeedMps: Double) {
        // vzdálenost
        var dist = 0.0
        for (i in 1 until segments.size) dist += segments[i - 1].first.distanceToAsDouble(segments[i].first)

        // okamžitá rychlost
        val instKph = latestSpeedMps * 3.6
        speedSumKph += instKph
        speedCount++
        val avgKph = speedSumKph / speedCount
        binding.tvInstantSpeed.text = "Rychlost: ${"%.1f".format(instKph)} km/h"
        binding.tvAverageSpeed.text = "Průměr: ${"%.1f".format(avgKph)} km/h"

        // barevný proužek a chod
        val gait = when {
            instKph < horse.walkSpeed * 0.8 -> "Stojí"
            instKph < horse.walkSpeed       -> "Krok"
            instKph < horse.trotSpeed       -> "Klus"
            else                             -> "Cval"
        }
        binding.tvCurrentGait.text = "Chod: $gait"
        binding.tvStats.text = "Body: ${segments.size}  Vzdálenost: ${"%.1f".format(dist)} m"

        listOf(
            binding.barStand  to segments.count { classifySpeed(it.second) == RideType.STAND },
            binding.barWalk   to segments.count { classifySpeed(it.second) == RideType.WALK },
            binding.barTrot   to segments.count { classifySpeed(it.second) == RideType.TROT },
            binding.barGallop to segments.count { classifySpeed(it.second) == RideType.GALLOP }
        ).forEach { (view, cnt) ->
            val lp = view.layoutParams as LinearLayout.LayoutParams
            lp.weight = cnt.toFloat() / segments.size
            view.layoutParams = lp
        }
    }

    private fun updateDuration() {
        val elapsed = System.currentTimeMillis() - rideStartTime
        val m = (elapsed / 1000 / 60).toInt()
        val s = (elapsed / 1000 % 60).toInt()
        binding.tvDuration.text = String.format("Doba: %02d:%02d", m, s)
    }

    private fun stopAndSave() {
        val rideEnd = System.currentTimeMillis()
        val durationSec = (rideEnd - rideStartTime) / 1000

        // GeoJSON
        val coords = JSONArray().also { arr -> segments.forEach { (p, _) -> arr.put(JSONArray().put(p.longitude).put(p.latitude)) } }
        val geoJson = JSONObject().apply { put("type","LineString"); put("coordinates",coords); put("timestamp",rideEnd) }.toString()

        // vzdálenost
        var totalDist = 0.0; for (i in 1 until segments.size) totalDist += segments[i-1].first.distanceToAsDouble(segments[i].first)

        // podíly
        val pts = segments.size.toDouble().coerceAtLeast(1.0)
        val walkP  = segments.count { classifySpeed(it.second) == RideType.WALK }   / pts
        val trotP  = segments.count { classifySpeed(it.second) == RideType.TROT }   / pts
        val gallP  = segments.count { classifySpeed(it.second) == RideType.GALLOP } / pts
        val standP = segments.count { classifySpeed(it.second) == RideType.STAND }  / pts

        // uložení
        val ride = Ride(
            horseId         = horseId,
            timestamp       = rideStartTime,
            durationSeconds = durationSec,
            distance        = totalDist,
            walkPortion     = walkP,
            trotPortion     = trotP,
            gallopPortion   = gallP,
            geoJson         = geoJson
        )
        db.rideDao().insert(ride)

        Toast.makeText(
            this,
            "Jízda uložena: ${"%.1f".format(totalDist)} m, " +
                    "Trvání: ${String.format("%02d:%02d", durationSec/60, durationSec%60)}", Toast.LENGTH_LONG
        ).show()

        startActivity(Intent(this, StatsActivity::class.java).putExtra("horseId", horseId))
        finish()
    }

    private fun classifySpeed(mps: Double): RideType {
        val kph = mps * 3.6
        if (kph < 1.0) return RideType.STAND
        return when {
            kph < horse.walkSpeed * 0.8 -> RideType.STAND
            kph < horse.walkSpeed       -> RideType.WALK
            kph < horse.trotSpeed       -> RideType.TROT
            else                         -> RideType.GALLOP
        }
    }

    private enum class RideType { STAND, WALK, TROT, GALLOP }
}

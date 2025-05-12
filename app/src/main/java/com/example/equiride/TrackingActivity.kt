package com.example.equiride

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
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
import com.google.android.gms.location.*

class TrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrackingBinding
    private var horseId: Long = 0L
    private var rideStartTime: Long = 0L
    private lateinit var horse: Horse
    private val db by lazy { AppDatabase.get(this) }

    private val segments = mutableListOf<Pair<GeoPoint, Double>>()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startLocationUpdates()
        else Toast.makeText(this, "Bez GPS nebudu sledovat trasu", Toast.LENGTH_LONG).show()
    }

    private val MIN_DIST_THRESHOLD = 5.0  // metry
    private val MAX_VALID_SPEED_KPH = 40.0
    private var firstFix = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Načíst koně
        horseId = intent.getLongExtra("horseId", 0L)
        horse = db.horseDao().getById(horseId)
            ?: throw IllegalStateException("Horse not found")

        // Konfigurace osmdroid
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))

        binding = ActivityTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        rideStartTime = System.currentTimeMillis()

        // Povolit pinch-zoom a panning
        binding.mapview.setMultiTouchControls(true)
        // Výchozí zoom
        binding.mapview.controller.setZoom(18.0)

        // Zobrazit jméno koně
        binding.tvHorseName.text = "Kůň: ${horse.name}"

        // Inicializace location API
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { onNewLocation(it) }
            }
        }

        // Žádost o práva
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            startLocationUpdates()
        }

        // Ukončit záznam
        binding.btnStop.setOnClickListener {
            stopAndSave()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapview.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapview.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun startLocationUpdates() {
        val req = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 1500L)
            .setMinUpdateDistanceMeters(2f)
            .build()
        fusedLocationClient.requestLocationUpdates(req, locationCallback, mainLooper)
    }

    private fun onNewLocation(loc: Location) {
        val pt = GeoPoint(loc.latitude, loc.longitude)
        if (segments.isNotEmpty()) {
            val lastPt = segments.last().first
            val speedKph = loc.speed * 3.6
            if (speedKph > MAX_VALID_SPEED_KPH) return
            val d = lastPt.distanceToAsDouble(pt)
            if (d < MIN_DIST_THRESHOLD) return
        }
        segments.add(pt to loc.speed.toDouble())
        drawSegments()
        showCurrentMarker(pt)
        if (firstFix) {
            binding.mapview.controller.animateTo(pt)
            firstFix = false
        }
        updateStats(loc.speed.toDouble())
        // 0) Doba jízdy
        val elapsedMs = System.currentTimeMillis() - rideStartTime
        val minutes = (elapsedMs / 1000 / 60).toInt()
        val seconds = (elapsedMs / 1000 % 60).toInt()
        binding.tvDuration.text = String.format("Doba: %02d:%02d", minutes, seconds)
    }

    private fun drawSegments() {
        binding.mapview.overlays.removeAll { it is Polyline }
        for (i in 1 until segments.size) {
            val (p0, _) = segments[i - 1]
            val (p1, s1) = segments[i]
            val kph = s1 * 3.6
            val color = when {
                kph < horse.walkSpeed * 0.8 -> 0xFF888888.toInt()
                kph < horse.walkSpeed      -> 0xFF006400.toInt()     // krok (tmavě zelená)
                kph < horse.trotSpeed       -> 0xFFCCCC00.toInt()
                else                        -> 0xFF8B0000.toInt()
            }
            val line = Polyline().apply {
                addPoint(p0); addPoint(p1)
                width = 8f
                outlinePaint.color = color
            }
            binding.mapview.overlays.add(line)
        }
        binding.mapview.invalidate()
    }

    private var currentMarker: Marker? = null

    private fun showCurrentMarker(p: GeoPoint) {
        currentMarker?.let { binding.mapview.overlays.remove(it) }
        currentMarker = Marker(binding.mapview).apply {
            position = p
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(
                this@TrackingActivity,
                org.osmdroid.library.R.drawable.marker_default
            )
            title = "Teď tady"
        }
        binding.mapview.overlays.add(currentMarker)
        binding.mapview.invalidate()
    }

    private fun updateStats(latestSpeed: Double) {
        // Vzdálenost
        var dist = 0.0
        for (i in 1 until segments.size) {
            dist += segments[i - 1].first.distanceToAsDouble(segments[i].first)
        }

        // Počty a procenta chodů
        val total = segments.size.toDouble().coerceAtLeast(1.0)
        val standCount  = segments.count { classifySpeed(it.second) == RideType.STAND }
        val walkCount   = segments.count { classifySpeed(it.second) == RideType.WALK }
        val trotCount   = segments.count { classifySpeed(it.second) == RideType.TROT }
        val gallopCount = segments.count { classifySpeed(it.second) == RideType.GALLOP }

        val standPct  = standCount  / total
        val walkPct   = walkCount   / total
        val trotPct   = trotCount   / total
        val gallopPct = gallopCount / total

        // Texty
        val gait = when (classifySpeed(latestSpeed)) {
            RideType.STAND  -> "Stojí"
            RideType.WALK   -> "Krok"
            RideType.TROT   -> "Klus"
            RideType.GALLOP -> "Cval"
        }
        binding.tvCurrentGait.text = "Chod: $gait"

        binding.tvStats.text = "Body: ${segments.size}  Vzdálenost: ${"%.1f".format(dist)} m"

        // Nastavení váhy barevného proužku
        val lpStand  = binding.barStand.layoutParams  as LinearLayout.LayoutParams
        val lpWalk   = binding.barWalk.layoutParams   as LinearLayout.LayoutParams
        val lpTrot   = binding.barTrot.layoutParams   as LinearLayout.LayoutParams
        val lpGallop = binding.barGallop.layoutParams as LinearLayout.LayoutParams

        lpStand.weight  = standPct.toFloat()
        lpWalk.weight   = walkPct.toFloat()
        lpTrot.weight   = trotPct.toFloat()
        lpGallop.weight = gallopPct.toFloat()

        binding.barStand.layoutParams  = lpStand
        binding.barWalk.layoutParams   = lpWalk
        binding.barTrot.layoutParams   = lpTrot
        binding.barGallop.layoutParams = lpGallop
    }

    private fun classifySpeed(mps: Double): RideType {
        val kph = mps * 3.6
        return when {
            kph < horse.walkSpeed * 0.8 -> RideType.STAND
            kph < horse.walkSpeed       -> RideType.WALK
            kph < horse.trotSpeed       -> RideType.TROT
            else                         -> RideType.GALLOP
        }
    }

    private fun stopAndSave() {
        val coords = JSONArray().also { arr ->
            segments.forEach { (p, _) ->
                arr.put(JSONArray().put(p.longitude).put(p.latitude))
            }
        }
        val geoJson = JSONObject().apply {
            put("type", "LineString")
            put("coordinates", coords)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        var totalDist = 0.0
        for (i in 1 until segments.size) {
            totalDist += segments[i - 1].first.distanceToAsDouble(segments[i].first)
        }

        val totalPts     = segments.size.coerceAtLeast(1).toDouble()
        val walkPortion   = segments.count { classifySpeed(it.second)==RideType.WALK }   / totalPts
        val trotPortion   = segments.count { classifySpeed(it.second)==RideType.TROT }   / totalPts
        val gallopPortion = segments.count { classifySpeed(it.second)==RideType.GALLOP } / totalPts

        val ride = Ride(
            horseId       = horseId,
            distance      = totalDist,
            walkPortion   = walkPortion,
            trotPortion   = trotPortion,
            gallopPortion = gallopPortion,
            geoJson       = geoJson
        )
        db.rideDao().insert(ride)

        Toast.makeText(
            this,
            "Jízda uložena: ${"%.1f".format(totalDist)} m, " +
                    "K:${"%.0f".format(walkPortion*100)}% Kl:${"%.0f".format(trotPortion*100)}% Cv:${"%.0f".format(gallopPortion*100)}%",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private enum class RideType { STAND, WALK, TROT, GALLOP }
}

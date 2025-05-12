package com.example.equiride

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.equiride.databinding.ActivityStatsBinding
import org.json.JSONObject

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private val db by lazy { AppDatabase.get(this) }
    private var horseId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        horseId = intent.getLongExtra("horseId", 0L)
        db.horseDao().getById(horseId)?.let {
            binding.tvHorseName.text = it.name
        }

        // Pozorujeme změny v datech
        db.rideDao().getByHorse(horseId).observe(this, Observer { rides ->
            if (rides.isEmpty()) {
                binding.tvNoData.visibility = View.VISIBLE
                binding.tvNoData.text = "Zatím žádné jízdy."
                binding.tvTotalDistance.text = ""
                binding.tvAvgSpeeds.text = ""
                binding.pieChart.clearData()
                binding.tvHistory.text = ""
            } else {
                binding.tvNoData.visibility = View.GONE

                val totalDistance = rides.sumOf { it.distance }
                binding.tvTotalDistance.text =
                    "Ujetá vzdálenost: %.1f m".format(totalDistance)

                val avgWalk   = rides.map { it.walkPortion * it.distance }.sum() / totalDistance
                val avgTrot   = rides.map { it.trotPortion * it.distance }.sum() / totalDistance
                val avgGallop = rides.map { it.gallopPortion * it.distance }.sum() / totalDistance
                binding.tvAvgSpeeds.text =
                    "Průměr: krok %.1f m, klus %.1f m, cval %.1f m"
                        .format(avgWalk, avgTrot, avgGallop)

                val pieData = mapOf(
                    "Krok"  to rides.sumOf { it.walkPortion },
                    "Klus"  to rides.sumOf { it.trotPortion },
                    "Cval"  to rides.sumOf { it.gallopPortion }
                )
                binding.pieChart.setData(pieData)

                val historyText = rides.joinToString("\n") { ride ->
                    val ts = JSONObject(ride.geoJson).optLong("timestamp", ride.timestamp)
                    val date = java.text.SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date(ts))
                    "• $date: %.1f m".format(ride.distance)
                }
                binding.tvHistory.text = historyText
            }
        })

        // Reset historie
        binding.btnResetStats.setOnClickListener {
            db.rideDao().deleteByHorse(horseId)
            Toast.makeText(this, "Statistiky smazány", Toast.LENGTH_SHORT).show()
        }
    }
}

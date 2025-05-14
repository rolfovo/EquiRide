package com.example.equiride

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.equiride.databinding.ActivityStatsBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.text.SimpleDateFormat
import java.util.*

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private lateinit var adapter: RideListAdapter
    private val dateFmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView + adapter
        adapter = RideListAdapter(dateFmt)
        binding.rvRides.layoutManager = LinearLayoutManager(this)
        binding.rvRides.adapter = adapter

        val horseId = intent.getLongExtra("horseId", 0L)
        binding.btnReset.setOnClickListener {
            Thread {
                AppDatabase.get(this).rideDao().deleteByHorse(horseId)
            }.start()
        }

        // Nasloucháme LiveData
        AppDatabase.get(this)
            .rideDao()
            .getByHorse(horseId)
            .observe(this) { rides ->
                showSummary(rides)
                adapter.submitList(rides.reversed()) // nejnovější nahoře
            }
    }

    private fun showSummary(rides: List<Ride>) {
        // Celkem a počet jízd
        val totalDist = rides.sumOf { it.distance }
        binding.tvTotalRides.text = "Počet jízd: ${rides.size}"
        binding.tvTotalDist.text  = "Celkem: ${"%.1f".format(totalDist)} m"

        // Výpočet průměrných procent
        val cnt = rides.size.coerceAtLeast(1)
        val avgWalk  = (rides.sumOf { it.walkPortion   } / cnt * 100.0).toFloat()
        val avgTrot  = (rides.sumOf { it.trotPortion   } / cnt * 100.0).toFloat()
        val avgGall  = (rides.sumOf { it.gallopPortion } / cnt * 100.0).toFloat()

        // PieEntries musí být Float + String
        val entries = listOf(
            PieEntry(avgWalk, "Krok"),
            PieEntry(avgTrot, "Klus"),
            PieEntry(avgGall, "Cval")
        )
        val ds = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#006400"),
                Color.parseColor("#CCCC00"),
                Color.parseColor("#8B0000")
            )
            valueTextSize  = 12f
            valueTextColor = Color.WHITE
            sliceSpace     = 2f
        }

        binding.pieChart.apply {
            data = PieData(ds)
            description.isEnabled = false
            legend.isEnabled      = false  // vlastní legenda v layoutu
            setUsePercentValues(true)
            invalidate()
        }
    }
}

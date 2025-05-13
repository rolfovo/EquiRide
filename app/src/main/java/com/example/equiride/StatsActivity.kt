package com.example.equiride

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.observe
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

        // RecyclerView
        adapter = RideListAdapter()
        binding.rvRides.layoutManager = LinearLayoutManager(this)
        binding.rvRides.adapter = adapter

        val horseId = intent.getLongExtra("horseId", 0L)
        binding.btnReset.setOnClickListener { resetStats(horseId) }

        // Pozorujeme LiveData
        AppDatabase.get(this)
            .rideDao()
            .getByHorse(horseId)
            .observe(this) { rides ->
                showSummary(rides)
                adapter.submitList(rides.reversed()) // nejnovější nahoře
            }
    }

    private fun showSummary(rides: List<Ride>) {
        // celková délka, počet
        val totalDist = rides.sumOf { it.distance }
        binding.tvTotalRides.text = "Počet jízd: ${rides.size}"
        binding.tvTotalDist.text = "Celkem: ${"%.1f".format(totalDist)} m"

        // průměrné podíly
        val cnt = rides.size.takeIf { it>0 } ?: 1
        val w = rides.sumOf { it.walkPortion }/cnt *100
        val t = rides.sumOf { it.trotPortion }/cnt *100
        val g = rides.sumOf { it.gallopPortion }/cnt *100

        // PieChart
        val entries = listOf(
            PieEntry(w.toFloat(), "Krok"),
            PieEntry(t.toFloat(), "Klus"),
            PieEntry(g.toFloat(), "Cval")
        )
        val ds = PieDataSet(entries, "").apply {
            colors = listOf(Color.parseColor("#006400"), Color.parseColor("#CCCC00"), Color.parseColor("#8B0000"))
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            sliceSpace = 2f
        }
        binding.pieChart.apply {
            data = PieData(ds)
            description.isEnabled = false
            legend.isEnabled = true
            setUsePercentValues(true)
            invalidate()
        }
    }

    private fun resetStats(horseId: Long) {
        Thread {
            AppDatabase.get(this).rideDao().deleteByHorse(horseId)
        }.start()
    }
}

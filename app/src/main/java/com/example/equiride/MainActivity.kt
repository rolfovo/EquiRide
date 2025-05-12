package com.example.equiride

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.equiride.databinding.ActivityMainBinding
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedHorseId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // USER-AGENT pro osmdroid, jinak nenačte dlaždice
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Map initialization
        binding.mapview.apply {
            controller.setZoom(15.0)
            controller.setCenter(mapCenter)
        }

        // Vybrat koně
        binding.btnSelectHorse.setOnClickListener {
            startActivityForResult(
                Intent(this, HorseListActivity::class.java),
                REQUEST_SELECT_HORSE
            )
        }

        // Spustit jízdu
        binding.btnStartRide.setOnClickListener {
            selectedHorseId?.let { id ->
                startActivity(
                    Intent(this, TrackingActivity::class.java)
                        .putExtra("horseId", id)
                )
            }
        }
    }

    // Zadání user lifecycle pro osmdroid
    override fun onResume() {
        super.onResume()
        binding.mapview.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapview.onPause()
    }

    // Výsledek výběru koně
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_HORSE && resultCode == RESULT_OK) {
            selectedHorseId = data?.getLongExtra("horseId", 0L)
            val name = data?.getStringExtra("horseName") ?: "?"
            binding.btnSelectHorse.text = name
            binding.btnStartRide.isEnabled = true
        }
    }

    companion object {
        private const val REQUEST_SELECT_HORSE = 1001
    }
}

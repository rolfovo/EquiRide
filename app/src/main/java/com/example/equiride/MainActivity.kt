package com.example.equiride

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.equiride.databinding.ActivityMainBinding
import org.osmdroid.config.Configuration

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedHorseId: Long = 0L

    // nový launcher pro výběr koně
    private val selectHorseLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                selectedHorseId = data.getLongExtra("horseId", 0L)
                val name = data.getStringExtra("horseName") ?: "?"
                binding.btnSelectHorse.text = name
                binding.btnStartRide.isEnabled = true
            }
        }
    }

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

        // Vybrat koně -> nový launcher
        binding.btnSelectHorse.setOnClickListener {
            selectHorseLauncher.launch(
                Intent(this, HorseListActivity::class.java)
            )
        }

        // Spustit jízdu
        binding.btnStartRide.setOnClickListener {
            Intent(this, TrackingActivity::class.java).also {
                it.putExtra("horseId", selectedHorseId)
                startActivity(it)
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
}

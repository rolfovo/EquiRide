package com.example.equiride

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.equiride.databinding.ActivityAddHorseBinding

class AddHorseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddHorseBinding
    private val horseDao by lazy { AppDatabase.get(this).horseDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHorseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSave.setOnClickListener {
            val name = binding.inputName.text.toString().trim()
            val walk = binding.inputWalk.text.toString().toDoubleOrNull() ?: 0.0
            val trot = binding.inputTrot.text.toString().toDoubleOrNull() ?: 0.0
            val gallop = binding.inputGallop.text.toString().toDoubleOrNull() ?: 0.0

            if (name.isNotEmpty()) {
                horseDao.insert(Horse(name = name, walkSpeed = walk, trotSpeed = trot, gallopSpeed = gallop))
                finish()
            } else {
                Toast.makeText(this, "Jméno je povinné", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
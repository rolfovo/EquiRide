package com.example.equiride

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.equiride.databinding.ActivityHorseListBinding

class HorseListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHorseListBinding
    private val db by lazy { AppDatabase.get(this) }
    private val adapter = HorseAdapter(mutableListOf()) { horse, action ->
        when (action) {
            HorseAdapter.Action.DELETE ->
                db.horseDao().delete(horse)

            HorseAdapter.Action.STATS -> {
                Intent(this, StatsActivity::class.java).apply {
                    putExtra("horseId", horse.id)
                }.also { startActivity(it) }
            }

            HorseAdapter.Action.SELECT -> {
                setResult(
                    RESULT_OK,
                    Intent().apply {
                        putExtra("horseId", horse.id)
                        putExtra("horseName", horse.name)
                    }
                )
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHorseListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        db.horseDao().getAll().observe(this) { horses ->
            adapter.updateList(horses)
        }

        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, AddHorseActivity::class.java))
        }
    }
}
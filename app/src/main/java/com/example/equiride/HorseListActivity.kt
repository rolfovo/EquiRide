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
            HorseAdapter.Action.DELETE -> db.horseDao().delete(horse)
            HorseAdapter.Action.STATS -> {
                val i = Intent(this, StatsActivity::class.java)
                    .putExtra("horseId", horse.id)
                startActivity(i)
            }
            HorseAdapter.Action.SELECT -> {
                setResult(
                    RESULT_OK,
                    Intent()
                        .putExtra("horseId", horse.id)
                        .putExtra("horseName", horse.name)
                )
                finish()
            }
            else -> Unit  // ← tohle musíme doplnit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHorseListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        db.horseDao().getAll().observe(this) {
            adapter.updateList(it)
        }

        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, AddHorseActivity::class.java))
        }
    }
}
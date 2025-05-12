package com.example.equiride

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "horses")
data class Horse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val walkSpeed: Double,
    val trotSpeed: Double,
    val gallopSpeed: Double
)
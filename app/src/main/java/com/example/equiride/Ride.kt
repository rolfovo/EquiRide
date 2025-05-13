package com.example.equiride

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rides",
    foreignKeys = [ForeignKey(
        entity = Horse::class,
        parentColumns = ["id"],
        childColumns = ["horseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("horseId")]  // přidáme index na horseId
)
data class Ride(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val horseId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val distance: Double,
    val walkPortion: Double,
    val trotPortion: Double,
    val gallopPortion: Double,
    val geoJson: String
)

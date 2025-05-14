package com.example.equiride

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RideDao {
    @Query("SELECT * FROM rides WHERE horseId = :horseId")
    fun getByHorse(horseId: Long): LiveData<List<Ride>>

    @Insert
    fun insert(ride: Ride): Long

    @Query("DELETE FROM rides WHERE horseId = :horseId")
    fun deleteByHorse(horseId: Long)
}
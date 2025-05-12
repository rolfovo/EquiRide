package com.example.equiride

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface HorseDao {
    @Query("SELECT * FROM horses")
    fun getAll(): LiveData<List<Horse>>

    @Query("SELECT * FROM horses WHERE id = :id")
    fun getById(id: Long): Horse?

    @Insert fun insert(h: Horse): Long
    @Delete fun delete(h: Horse)
}
package com.example.equiride

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Horse::class, Ride::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun horseDao(): HorseDao
    abstract fun rideDao(): RideDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "equiride.db"
                )
                    .allowMainThreadQueries()  // pro prototyp OK
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

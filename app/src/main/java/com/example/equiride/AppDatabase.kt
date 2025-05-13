package com.example.equiride

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Horse::class, Ride::class],
    version = 2,              // zvýšili jsme na 2
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun horseDao(): HorseDao
    abstract fun rideDao(): RideDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "equiride.db"
            )
                .allowMainThreadQueries()           // pro prototyp OK
                .fallbackToDestructiveMigration()   // smaže a znovu vytvoří DB, když se změní verze
                .build()
    }
}

package com.example.equiride

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Horse::class, Ride::class],
    version = 3,         // zvýšili jsme verzi
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun horseDao(): HorseDao
    abstract fun rideDao(): RideDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "equiride.db"
            )
                // přidáme migraci 2 → 3, která přidá sloupec durationSeconds
                .addMigrations(MIGRATION_2_3)
                .allowMainThreadQueries() // pro prototyp OK
                .build()

        // Migrace z verze 2 na 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // přidáme nový sloupec s defaultní hodnotou 0
                db.execSQL(
                    """
                    ALTER TABLE rides 
                    ADD COLUMN durationSeconds INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
            }
        }
    }
}

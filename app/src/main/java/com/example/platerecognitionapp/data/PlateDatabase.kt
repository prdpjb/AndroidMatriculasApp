package com.example.platerecognitionapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Plate::class, PlateLocation::class], version = 1)
@TypeConverters(LocalDateTimeConverter::class)
abstract class PlateDatabase : RoomDatabase() {
    abstract fun plateDao(): PlateDao

    abstract fun plateLocationDao(): PlateLocationDao

    companion object {
        @Volatile
        private var INSTANCE: PlateDatabase? = null

        fun getDatabase(context: Context): PlateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlateDatabase::class.java,
                    "plate_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

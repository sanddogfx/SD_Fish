package com.sdfish.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CatchEntry::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catchDao(): CatchDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "sdfish.db").build()
    }
}

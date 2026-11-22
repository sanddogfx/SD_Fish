package com.sdfish.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CatchDao {
    @Query("SELECT * FROM catches ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CatchEntry>>

    @Query("SELECT * FROM catches WHERE id = :id")
    suspend fun getById(id: Long): CatchEntry?

    @Upsert
    suspend fun upsert(entry: CatchEntry): Long

    @Delete
    suspend fun delete(entry: CatchEntry)
}

package com.sdfish.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

/**
 * A photo of a fish. A fish can have several; the first one is also kept in
 * [CatchEntry.photoPath] as the picture shown in lists.
 */
@Entity(tableName = "catch_photos", indices = [Index("catchId")])
data class CatchPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val catchId: Long,
    val path: String,
    val position: Int,
)

@Dao
interface CatchPhotoDao {
    @Query("SELECT path FROM catch_photos WHERE catchId = :catchId ORDER BY position")
    suspend fun pathsFor(catchId: Long): List<String>

    @Query("DELETE FROM catch_photos WHERE catchId = :catchId")
    suspend fun deleteFor(catchId: Long)

    @Insert
    suspend fun insertAll(photos: List<CatchPhoto>)

    /** Replaces the photos of a fish with [paths], in this order. */
    @Transaction
    suspend fun replace(catchId: Long, paths: List<String>) {
        deleteFor(catchId)
        insertAll(paths.mapIndexed { i, p -> CatchPhoto(catchId = catchId, path = p, position = i) })
    }
}

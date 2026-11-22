package com.sdfish.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One caught fish. */
@Entity(tableName = "catches")
data class CatchEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** When it was caught, epoch milliseconds. */
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    /** A [Species.id] from [SpeciesCatalog], or free text if the species is not in the list. */
    val species: String,
    val weightKg: Double?,
    val lengthCm: Double?,
    /** Absolute path of the photo in app storage. */
    val photoPath: String?,
    val note: String,
)

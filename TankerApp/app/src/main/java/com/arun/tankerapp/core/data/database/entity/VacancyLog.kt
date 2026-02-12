package com.arun.tankerapp.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vacancies",
    foreignKeys = [
        ForeignKey(
            entity = Apartment::class,
            parentColumns = ["id"],
            childColumns = ["apartment_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["apartment_id"])]
)
data class VacancyLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "apartment_id")
    val apartmentId: Long,

    @ColumnInfo(name = "start_date")
    val startDate: String, // ISO-8601 format: "2026-02-10"

    @ColumnInfo(name = "end_date")
    val endDate: String // ISO-8601 format: "2026-02-15"
)

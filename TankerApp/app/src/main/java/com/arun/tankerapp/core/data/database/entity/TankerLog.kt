package com.arun.tankerapp.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tankers")
data class TankerLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "date")
    val date: String, // ISO-8601 format: "2026-02-10"

    @ColumnInfo(name = "month")
    val month: Int,

    @ColumnInfo(name = "year")
    val year: Int,

    @ColumnInfo(name = "count")
    val count: Int = 1 // Number of tankers received on this date
)

package com.arun.tankerapp.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "billing_cycles")
data class BillingCycle(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startDate: LocalDate, // Start date (exclusive for tanker filtering usually, but we store the bounds here)
    val endDate: LocalDate,   // End date (inclusive)
    val totalTankers: Int     // Snapshot of total tankers
)

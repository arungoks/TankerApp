package com.arun.tankerapp.core.data.model

import com.arun.tankerapp.core.data.database.entity.Apartment

data class ApartmentBill(
    val apartment: Apartment,
    val billableTankers: Int,
    val totalTankersInCycle: Int
)

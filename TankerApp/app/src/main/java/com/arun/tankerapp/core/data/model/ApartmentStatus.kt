package com.arun.tankerapp.core.data.model

import com.arun.tankerapp.core.data.database.entity.Apartment

data class ApartmentStatus(
    val apartment: Apartment,
    val isVacant: Boolean
)

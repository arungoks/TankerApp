package com.arun.tankerapp.core.data.model

import java.time.LocalDate

data class TankerPrice(
    val id: String,
    val effectiveDate: LocalDate,
    val price: Double
)

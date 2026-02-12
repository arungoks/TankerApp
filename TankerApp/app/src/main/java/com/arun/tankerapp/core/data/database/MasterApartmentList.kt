package com.arun.tankerapp.core.data.database

/**
 * Hardcoded master list of 68 apartments as per requirements.
 * Single block, 17 floors, 4 units per floor (01-04).
 * Range: 101 to 1704.
 */
object MasterApartmentList {
    val apartments = (1..17).flatMap { floor ->
        (1..4).map { unit ->
            "${floor * 100 + unit}"
        }
    }
}

package com.arun.tankerapp.core.data.model.firestore

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class ApartmentDocument(
    @DocumentId val id: String? = null, // Document ID will be the apartment number (e.g., "101")
    val number: String = "",
    val ownerId: String = ""
)

data class VacancyDocument(
    @DocumentId val id: String? = null,
    val apartmentId: String = "", // References Apartment Document ID (e.g., "101")
    val startDate: String = "",
    val endDate: String? = null,
    val ownerId: String = ""
)

data class TankerDocument(
    @DocumentId val id: String? = null,
    val date: String = "",
    val count: Int = 0,
    val month: Int = 0,
    val year: Int = 0,
    val ownerId: String = ""
)

data class BillingCycleDocument(
    @DocumentId val id: String? = null,
    val startDate: String = "",
    val endDate: String = "",
    val totalTankers: Int = 0,
    val ownerId: String = ""
)

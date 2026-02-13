package com.arun.tankerapp.core.data.model.bson

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class TankerBson(
    @BsonId
    val id: ObjectId = ObjectId(),
    val date: String,
    val count: Int = 1,
    val supplier: String? = null,
    val ownerId: String
)

data class VacancyBson(
    @BsonId
    val id: ObjectId = ObjectId(),
    val apartmentId: Int, // Referencing by Apartment Number or ID. Since apartments are static, ID/Number map is fine. Using Int ID for now to match Room logic.
    val startDate: String,
    val endDate: String? = null,
    val ownerId: String
)

data class BillingCycleBson(
    @BsonId
    val id: ObjectId = ObjectId(),
    val startDate: String,
    val endDate: String,
    val isActive: Boolean,
    val ownerId: String
)

data class ApartmentBson(
    @BsonId
    val id: ObjectId = ObjectId(),
    val number: String,
    val residents: List<ResidentBson> = emptyList(),
    val ownerId: String
)

data class ResidentBson(
    val name: String,
    val phoneNumber: String
)

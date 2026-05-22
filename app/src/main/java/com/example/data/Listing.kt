package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listings")
data class Listing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val photoUri: String = "",
    val address: String = "",
    val bedrooms: Int = 3,
    val bathrooms: Double = 2.0,
    val squareFeet: Int = 1800,
    val lotSize: Double = 0.25,
    val yearBuilt: Int = 2010,
    val propertyType: String = "Single Family",
    val condition: String = "Good",
    val upgrades: String = "",
    val askingPrice: Double = 0.0,
    val detectedFeatures: String = "", // Comma-separated list of tags
    val suggestedPriceLow: Double = 0.0,
    val suggestedPriceRecommended: Double = 0.0,
    val suggestedPriceHigh: Double = 0.0,
    val confidenceScore: Int = 0,
    val pricingReasoning: String = "",
    val mlsDescription: String = "",
    val socialCaption: String = "",
    val flyerText: String = "",
    val emailBlast: String = "",
    val viewsCount: Int = 0,
    val leadsCount: Int = 0,
    val status: String = "Draft", // Draft, Active, Sold, Closed
    val lastUpdatedDate: Long = System.currentTimeMillis(),
    val compsJson: String = ""
)

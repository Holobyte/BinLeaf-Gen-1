package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leads")
data class Lead(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listingId: Int,
    val buyerName: String,
    val buyerEmail: String = "",
    val buyerPhone: String = "",
    val interestLevel: String = "Medium", // High, Medium, Low
    val preferredShowingTime: String = "",
    val notes: String = "",
    val leadSource: String = "Direct",
    val status: String = "New", // New, Contacted, Showing Scheduled, Offer Pending, Closed, Lost
    val timestamp: Long = System.currentTimeMillis()
)

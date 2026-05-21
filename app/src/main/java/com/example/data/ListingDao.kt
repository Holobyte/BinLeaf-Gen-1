package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {
    @Query("SELECT * FROM listings ORDER BY lastUpdatedDate DESC")
    fun getAllListings(): Flow<List<Listing>>

    @Query("SELECT * FROM listings WHERE id = :id")
    suspend fun getListingById(id: Int): Listing?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: Listing): Long

    @Delete
    suspend fun deleteListing(listing: Listing)

    @Query("SELECT * FROM leads ORDER BY timestamp DESC")
    fun getAllLeads(): Flow<List<Lead>>

    @Query("SELECT * FROM leads WHERE listingId = :listingId ORDER BY timestamp DESC")
    fun getLeadsForListing(listingId: Int): Flow<List<Lead>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: Lead): Long

    @Update
    suspend fun updateLead(lead: Lead)

    @Delete
    suspend fun deleteLead(lead: Lead)

    @Query("UPDATE listings SET status = :status, lastUpdatedDate = :lastUpdated WHERE id = :listingId")
    suspend fun updateListingStatus(listingId: Int, status: String, lastUpdated: Long = System.currentTimeMillis())

    @Query("UPDATE listings SET viewsCount = :views, leadsCount = :leads WHERE id = :listingId")
    suspend fun updateListingStats(listingId: Int, views: Int, leads: Int)
}

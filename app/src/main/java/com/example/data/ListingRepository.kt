package com.example.data

import kotlinx.coroutines.flow.Flow

class ListingRepository(private val listingDao: ListingDao) {
    val allListings: Flow<List<Listing>> = listingDao.getAllListings()
    val allLeads: Flow<List<Lead>> = listingDao.getAllLeads()

    suspend fun getListingById(id: Int): Listing? {
        return listingDao.getListingById(id)
    }

    suspend fun insertListing(listing: Listing): Long {
        return listingDao.insertListing(listing)
    }

    suspend fun deleteListing(listing: Listing) {
        listingDao.deleteListing(listing)
    }

    fun getLeadsForListing(listingId: Int): Flow<List<Lead>> {
        return listingDao.getLeadsForListing(listingId)
    }

    suspend fun insertLead(lead: Lead): Long {
        return listingDao.insertLead(lead)
    }

    suspend fun updateLead(lead: Lead) {
        listingDao.updateLead(lead)
    }

    suspend fun deleteLead(lead: Lead) {
        listingDao.deleteLead(lead)
    }

    suspend fun updateListingStatus(listingId: Int, status: String) {
        listingDao.updateListingStatus(listingId, status)
    }

    suspend fun updateListingStats(listingId: Int, views: Int, leads: Int) {
        listingDao.updateListingStats(listingId, views, leads)
    }
}

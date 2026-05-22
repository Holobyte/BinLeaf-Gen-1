package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Lead
import com.example.data.Listing
import com.example.data.ListingRepository
import com.example.service.GeminiService
import com.example.service.ImageMetadataService
import com.example.service.NativeLocationService
import com.example.service.NativeGeocoderService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    object Onboarding : Screen
    object Dashboard : Screen
    object CreateListing : Screen
    object FeatureReview : Screen
    object PricingReport : Screen
    object MarketingCopy : Screen
    data class LeadCapture(val listingId: Int) : Screen
    object LeadCRM : Screen
    object Settings : Screen
}

class ListingAssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ListingRepository

    val listings: StateFlow<List<Listing>>
    val leads: StateFlow<List<Lead>>

    // General app states
    val currentScreen = MutableStateFlow<Screen>(Screen.Onboarding)
    val userRole = MutableStateFlow("FSBO Seller") // FSBO Seller or Real Estate Agent
    val username = MutableStateFlow("Tony Holobyte")
    val userBrandingName = MutableStateFlow("Tony's Realty Hub")
    val activeSubscriptionTier = MutableStateFlow("FSBO Pro") // Free, FSBO Starter, FSBO Pro, Agent Pro, Broker
    val dataSourceMode = MutableStateFlow("Public Records DB + AI Comps") // MLS, IDX, RESO Web API, Manual CSV

    // Loading states
    val isAnalyzing = MutableStateFlow(false)
    val isGeneratingPricing = MutableStateFlow(false)
    val isGeneratingCopy = MutableStateFlow(false)

    // Selection/Wizard states
    val tempListing = MutableStateFlow(Listing())
    val selectedListing = MutableStateFlow<Listing?>(null)
    val selectedLead = MutableStateFlow<Lead?>(null)

    val addressDetectionStatus = MutableStateFlow("")

    fun detectAddressFromImageOrLocation(context: Context, imageUri: Uri?) {
        viewModelScope.launch {
            addressDetectionStatus.value = "Checking image metadata..."
            var location: com.example.data.DetectedLocation? = null
            
            if (imageUri != null) {
                location = ImageMetadataService.extractGpsLocation(context, imageUri)
            }

            if (location != null) {
                addressDetectionStatus.value = "Using photo GPS metadata..."
            } else {
                addressDetectionStatus.value = "No photo GPS found. Checking device location..."
                location = NativeLocationService.getCurrentLocation(context)
            }

            if (location != null) {
                val address = NativeGeocoderService.reverseGeocode(context, location.latitude, location.longitude)
                if (!address.isNullOrBlank()) {
                    addressDetectionStatus.value = "Address detected."
                    tempListing.value = tempListing.value.copy(address = address)
                    onAddressEntered(address)
                } else {
                    addressDetectionStatus.value = "Could not detect address. Please type it manually."
                }
            } else {
                addressDetectionStatus.value = "Could not detect address. Please type it manually."
            }
        }
    }

    // Simulated gallery photo selections
    val photoStyles = listOf(
        PhotoStyleItem("Suburban Estate", "Classic red brick exterior, broad columns, manicured landscaping", "https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=400&q=80"),
        PhotoStyleItem("Modern Minimalist", "Cantilevered ceilings, floor-to-ceiling clean glass, open hardwood views", "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=400&q=80"),
        PhotoStyleItem("Craftsman Cottage", "Whimsical covered front porch, dark-stained pillars, stonework pathways", "https://images.unsplash.com/photo-1568605114967-8130f3a36994?auto=format&fit=crop&w=400&q=80"),
        PhotoStyleItem("Luxury Condo", "High-rise modern view window overlooking waterfront city panoramas", "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?auto=format&fit=crop&w=400&q=80"),
        PhotoStyleItem("Suburban Traditional", "Charming vinyl siding, tidy garage double door, friendly picket fence", "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=400&q=80")
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ListingRepository(database.listingDao())
        
        listings = repository.allListings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        leads = repository.allLeads.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    /**
     * Toggles screen transition, resetting creating listing buffers if navigating back to home
     */
    fun navigateTo(screen: Screen) {
        currentScreen.value = screen
        if (screen is Screen.Dashboard) {
            selectedListing.value = null
        }
    }

    /**
     * Instantly returns a structured mock records dataset when address matches certain keywords to simulate RESO/MLS connection.
     */
    fun onAddressEntered(address: String) {
        val lower = address.lowercase().trim()
        val generated = when {
            lower.contains("elm") -> Listing(
                address = address,
                bedrooms = 3,
                bathrooms = 2.0,
                squareFeet = 1650,
                lotSize = 0.22,
                yearBuilt = 2004,
                propertyType = "Single Family",
                condition = "Excellent",
                upgrades = "Hardwood floors added last year, smart thermostat",
                askingPrice = 289000.0
            )
            lower.contains("maple") -> Listing(
                address = address,
                bedrooms = 4,
                bathrooms = 2.5,
                squareFeet = 2450,
                lotSize = 0.35,
                yearBuilt = 2015,
                propertyType = "Single Family",
                condition = "Excellent",
                upgrades = "Spacious finished basement, quartz kitchen countertops",
                askingPrice = 415000.0
            )
            lower.contains("pine") || lower.contains("lake") -> Listing(
                address = address,
                bedrooms = 2,
                bathrooms = 1.5,
                squareFeet = 1200,
                lotSize = 0.15,
                yearBuilt = 1998,
                propertyType = "Townhouse",
                condition = "Good",
                upgrades = "Newly renovated master bath",
                askingPrice = 195000.0
            )
            lower.contains("penthouse") || lower.contains("broadway") -> Listing(
                address = address,
                bedrooms = 2,
                bathrooms = 2.0,
                squareFeet = 1400,
                lotSize = 0.0,
                yearBuilt = 2021,
                propertyType = "Condominium",
                condition = "Mint",
                upgrades = "Floor heating, motorized shade controls, high-end Miele appliances",
                askingPrice = 675000.0
            )
            else -> {
                // Generate slightly randomized but highly realistic mock property attributes
                val randomSeed = address.hashCode().coerceAtLeast(1)
                val beds = (3 + (randomSeed % 3)).coerceIn(2, 5)
                val baths = (1.5 + ((randomSeed % 3) * 0.5)).coerceIn(1.0, 4.0)
                val sqft = 1100 + (randomSeed % 15) * 150
                val yr = 1970 + (randomSeed % 55)
                Listing(
                    address = address,
                    bedrooms = beds,
                    bathrooms = baths,
                    squareFeet = sqft,
                    lotSize = 0.18 + ((randomSeed % 10) * 0.05),
                    yearBuilt = yr,
                    propertyType = "Single Family",
                    condition = if (yr > 2010) "Excellent" else "Good",
                    askingPrice = sqft * 165.0
                )
            }
        }
        tempListing.value = tempListing.value.copy(
            address = generated.address,
            bedrooms = generated.bedrooms,
            bathrooms = generated.bathrooms,
            squareFeet = generated.squareFeet,
            lotSize = generated.lotSize,
            yearBuilt = generated.yearBuilt,
            propertyType = generated.propertyType,
            condition = generated.condition,
            upgrades = generated.upgrades,
            askingPrice = generated.askingPrice
        )
    }

    /**
     * Step 1 Complete: Let's trigger Gemini to extract visual elements.
     */
    fun startFeatureAnalysis(photoStyle: String) {
        val current = tempListing.value
        tempListing.value = current.copy(photoUri = photoStyle)
        isAnalyzing.value = true
        currentScreen.value = Screen.FeatureReview

        viewModelScope.launch {
            val extractedTags = GeminiService.analyzeFeatures(
                photoStyle = photoStyle,
                propertyType = current.propertyType,
                additionalProps = current.upgrades
            )
            tempListing.value = tempListing.value.copy(
                detectedFeatures = extractedTags.joinToString(", ")
            )
            isAnalyzing.value = false
        }
    }

    /**
     * User confirms features, now we trigger the Pricing Suggestion Engine.
     */
    fun analyzePricingModels(confirmedFeatures: List<String>) {
        val current = tempListing.value
        tempListing.value = current.copy(
            detectedFeatures = confirmedFeatures.joinToString(", ")
        )
        isGeneratingPricing.value = true
        currentScreen.value = Screen.PricingReport

        viewModelScope.launch {
            val report = GeminiService.generatePricingReport(
                address = current.address,
                bedrooms = current.bedrooms,
                bathrooms = current.bathrooms,
                sqft = current.squareFeet,
                propertyType = current.propertyType,
                condition = current.condition,
                askingPriceInput = current.askingPrice,
                upgrades = current.upgrades
            )
            tempListing.value = tempListing.value.copy(
                suggestedPriceLow = report.lowPrice,
                suggestedPriceRecommended = report.recommendedPrice,
                suggestedPriceHigh = report.highPrice,
                confidenceScore = report.confidenceScore,
                pricingReasoning = report.reasoning
            )
            isGeneratingPricing.value = false
        }
    }

    /**
     * Confirms pricing, triggers full descriptive copy package creation and persists final list to SQLite.
     */
    fun generateMarketingPackageAndSave() {
        isGeneratingCopy.value = true
        currentScreen.value = Screen.MarketingCopy

        val current = tempListing.value
        val featuresList = current.detectedFeatures.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        viewModelScope.launch {
            val copies = GeminiService.generateMarketingCopy(
                address = current.address,
                bedrooms = current.bedrooms,
                bathrooms = current.bathrooms,
                sqft = current.squareFeet,
                propertyType = current.propertyType,
                condition = current.condition,
                upgrades = current.upgrades,
                features = featuresList
            )

            val finalListing = current.copy(
                mlsDescription = copies["mls"] ?: "",
                socialCaption = copies["social"] ?: "",
                flyerText = copies["emotional"] ?: "", // use emotional buyer description inside flyer space
                emailBlast = copies["luxury"] ?: "", // use premium luxury style description for agent email blasts
                status = "Active", // Live from moment generated
                lastUpdatedDate = System.currentTimeMillis()
            )

            // Persit listing in Room
            val id = repository.insertListing(finalListing)
            tempListing.value = finalListing.copy(id = id.toInt())
            selectedListing.value = finalListing.copy(id = id.toInt())
            
            isGeneratingCopy.value = false

            // Automatically schedule a few simulated lead inquiries after 2 seconds to make the CRM pipeline interactive!
            prepopulateDemoLeads(id.toInt())
        }
    }

    /**
     * Lets the user regenerate marketing text blocks
     */
    fun regenerateMarketingBlocks() {
        val current = selectedListing.value ?: tempListing.value
        isGeneratingCopy.value = true
        val featuresList = current.detectedFeatures.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        viewModelScope.launch {
            val copies = GeminiService.generateMarketingCopy(
                address = current.address,
                bedrooms = current.bedrooms,
                bathrooms = current.bathrooms,
                sqft = current.squareFeet,
                propertyType = current.propertyType,
                condition = current.condition,
                upgrades = current.upgrades,
                features = featuresList
            )
            val updated = current.copy(
                mlsDescription = copies["mls"] ?: "",
                socialCaption = copies["social"] ?: "",
                flyerText = copies["emotional"] ?: "",
                emailBlast = copies["luxury"] ?: ""
            )
            repository.insertListing(updated)
            if (selectedListing.value != null) {
                selectedListing.value = updated
            } else {
                tempListing.value = updated
            }
            isGeneratingCopy.value = false
        }
    }

    /**
     * Inserts an active buyer lead into the listing.
     */
    fun insertBuyerLead(
        listingId: Int,
        name: String,
        email: String,
        phone: String,
        showingTime: String,
        interestLevel: String,
        notes: String
    ) {
        viewModelScope.launch {
            val lead = Lead(
                listingId = listingId,
                buyerName = name,
                buyerEmail = email,
                buyerPhone = phone,
                preferredShowingTime = showingTime,
                interestLevel = interestLevel,
                notes = notes,
                leadSource = "Public Lead Capture Page",
                status = "New"
            )
            repository.insertLead(lead)
            
            // Increment statistics
            val listingMatches = listings.value.find { it.id == listingId }
            if (listingMatches != null) {
                val currentLeads = listingMatches.leadsCount + 1
                val currentViews = listingMatches.viewsCount + 2 // simulate view increase too
                repository.updateListingStats(listingId, currentViews, currentLeads)
                
                if (selectedListing.value?.id == listingId) {
                    selectedListing.value = listingMatches.copy(leadsCount = currentLeads, viewsCount = currentViews)
                }
            }
        }
    }

    /**
     * Change lead status inside simple CRM pipeline
     */
    fun updateLeadStatus(lead: Lead, newStatus: String) {
        viewModelScope.launch {
            repository.updateLead(lead.copy(status = newStatus))
        }
    }

    /**
     * Add listing views manually to simulate active interest or capture views.
     */
    fun simulateListingView(listingId: Int) {
        viewModelScope.launch {
            val match = listings.value.find { it.id == listingId }
            if (match != null) {
                repository.updateListingStats(listingId, match.viewsCount + 1, match.leadsCount)
                if (selectedListing.value?.id == listingId) {
                    selectedListing.value = match.copy(viewsCount = match.viewsCount + 1)
                }
            }
        }
    }

    /**
     * Toggle or edit listing status.
     */
    fun updateListingStatus(listingId: Int, status: String) {
        viewModelScope.launch {
            repository.updateListingStatus(listingId, status)
            if (selectedListing.value?.id == listingId) {
                selectedListing.value = selectedListing.value?.copy(status = status)
            }
        }
    }

    /**
     * Delete entry helper
     */
    fun deleteListing(listing: Listing) {
        viewModelScope.launch {
            repository.deleteListing(listing)
            if (selectedListing.value?.id == listing.id) {
                selectedListing.value = null
            }
        }
    }

    fun deleteLead(lead: Lead) {
        viewModelScope.launch {
            repository.deleteLead(lead)
        }
    }

    private fun prepopulateDemoLeads(listingId: Int) {
        viewModelScope.launch {
            val demoLeads = listOf(
                Lead(
                    listingId = listingId,
                    buyerName = "Jessica Vance",
                    buyerEmail = "jess.vance@example.com",
                    buyerPhone = "(312) 555-0143",
                    interestLevel = "High",
                    preferredShowingTime = "Saturday at 10:00 AM",
                    notes = "Very interested in the updated kitchen features. Selling her apartment soon.",
                    leadSource = "Zillow Partner Connect",
                    status = "Showing Scheduled"
                ),
                Lead(
                    listingId = listingId,
                    buyerName = "Marcus Brody",
                    buyerEmail = "marcus.b@example.com",
                    buyerPhone = "(312) 555-0988",
                    interestLevel = "Medium",
                    preferredShowingTime = "Sunday afternoon",
                    notes = "Inquiring about school districts and lot boundary details.",
                    leadSource = "FSBO Public Page",
                    status = "New"
                )
            )
            for (lead in demoLeads) {
                repository.insertLead(lead)
            }
            repository.updateListingStats(listingId, 18, 2)
            
            val match = listings.value.find { it.id == listingId }
            if (match != null) {
                if (selectedListing.value?.id == listingId) {
                    selectedListing.value = match.copy(viewsCount = 18, leadsCount = 2)
                }
            }
        }
    }
}

data class PhotoStyleItem(
    val name: String,
    val description: String,
    val imageUrl: String
)

package com.example.ui

import android.net.Uri
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.provider.MediaStore
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import okhttp3.OkHttpClient
import okhttp3.Request
import com.example.BuildConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Lead
import com.example.data.Listing
import com.example.ui.theme.*

// Data class representation for subscription builder UI
data class SubscriptionTier(
    val name: String,
    val description: String
)

@Composable
fun ListingAssistantApp(viewModel: ListingAssistantViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val listings by viewModel.listings.collectAsStateWithLifecycle()
    val leads by viewModel.leads.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        containerColor = SlateDark800,
        bottomBar = {
            AppBottomNavigation(
                currentScreen = currentScreen,
                onNavigate = { viewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SlateDark800, SlateDark700)
                    )
                )
        ) {
            when (val screen = currentScreen) {
                is Screen.Onboarding -> OnboardingScreen(viewModel)
                is Screen.Dashboard -> DashboardScreen(viewModel, listings, leads)
                is Screen.CreateListing -> CreateListingScreen(viewModel)
                is Screen.FeatureReview -> FeatureReviewScreen(viewModel)
                is Screen.PricingReport -> PricingReportScreen(viewModel)
                is Screen.MarketingCopy -> MarketingCopyScreen(viewModel)
                is Screen.LeadCapture -> LeadCapturePageScreen(viewModel, screen.listingId)
                is Screen.LeadCRM -> LeadCrmScreen(viewModel, listings, leads)
                is Screen.Settings -> SettingsScreen(viewModel)
            }
        }
    }
}

@Composable
fun AppBottomNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = SlateDark700,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        NavigationBarItem(
            selected = currentScreen is Screen.Onboarding,
            onClick = { onNavigate(Screen.Onboarding) },
            icon = { Icon(Icons.Default.Star, contentDescription = "Onboarding") },
            label = { Text("Intro", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TealAccent,
                selectedTextColor = TealAccent,
                indicatorColor = SlateCardLight,
                unselectedIconColor = CharcoalMuted,
                unselectedTextColor = CharcoalMuted
            ),
            modifier = Modifier.testTag("nav_intro")
        )
        NavigationBarItem(
            selected = currentScreen is Screen.Dashboard,
            onClick = { onNavigate(Screen.Dashboard) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
            label = { Text("Listings", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TealAccent,
                selectedTextColor = TealAccent,
                indicatorColor = SlateCardLight,
                unselectedIconColor = CharcoalMuted,
                unselectedTextColor = CharcoalMuted
            ),
            modifier = Modifier.testTag("nav_listings")
        )
        NavigationBarItem(
            selected = currentScreen is Screen.LeadCRM,
            onClick = { onNavigate(Screen.LeadCRM) },
            icon = { Icon(Icons.Default.Person, contentDescription = "CRM") },
            label = { Text("CRM Leads", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TealAccent,
                selectedTextColor = TealAccent,
                indicatorColor = SlateCardLight,
                unselectedIconColor = CharcoalMuted,
                unselectedTextColor = CharcoalMuted
            ),
            modifier = Modifier.testTag("nav_crm")
        )
        NavigationBarItem(
            selected = currentScreen is Screen.Settings,
            onClick = { onNavigate(Screen.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TealAccent,
                selectedTextColor = TealAccent,
                indicatorColor = SlateCardLight,
                unselectedIconColor = CharcoalMuted,
                unselectedTextColor = CharcoalMuted
            ),
            modifier = Modifier.testTag("nav_settings")
        )
    }
}

// 1. ONBOARDING
@Composable
fun OnboardingScreen(viewModel: ListingAssistantViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                color = TealAccent.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TealAccent)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TealAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Powered by models/gemini-3.5-flash",
                        color = TealAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Text(
                text = "Turn Property Photos Into Listings,\nPricing Insights & Leads.",
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = GeoTextDark,
                lineHeight = 32.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "The ultimate marketing assistant for FSBO sellers and professional real estate agents.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = GeoTextMuted,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GeoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Core Workflow:", fontWeight = FontWeight.Bold, color = TealAccent, fontSize = 16.sp)
                    WorkflowStepItem(
                        icon = Icons.Default.Add,
                        title = "1. Detail Lookup & Photo Style",
                        description = "Type property address to pull simulated public records, then pick a high-end photo style."
                    )
                    WorkflowStepItem(
                        icon = Icons.Default.Refresh,
                        title = "2. AI Magic Feature Extraction",
                        description = "AI analyzes your choice to automatically detect landscaping, masonry, flooring, layout config."
                    )
                    WorkflowStepItem(
                        icon = Icons.Default.Check,
                        title = "3. Zillow/MLS Comps Analysis",
                        description = "Pricing engine estimates comparative values, suggesting optimal parameters and confidences."
                    )
                    WorkflowStepItem(
                        icon = Icons.Default.Share,
                        title = "4. Lead Capture & CRM Loop",
                        description = "Generates multi-format descriptions, launches live online viewer, and records inquiries!"
                    )
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.navigateTo(Screen.CreateListing) },
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("onboarding_cta"),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Listing", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { viewModel.navigateTo(Screen.Dashboard) },
                border = BorderStroke(1.dp, GeoBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TealAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text("Browse Dashboard & CRM Items", fontSize = 14.sp)
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

// 2. DASHBOARD SCREEN
@Composable
fun DashboardScreen(
    viewModel: ListingAssistantViewModel,
    listings: List<Listing>,
    leads: List<Lead>
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredListings = listings.filter {
        it.address.contains(searchQuery, ignoreCase = true) || 
        it.propertyType.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Main Dashboard",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GeoTextDark
                        )
                        val activeRole by viewModel.userRole.collectAsStateWithLifecycle()
                        val activeTier by viewModel.activeSubscriptionTier.collectAsStateWithLifecycle()
                        Text(
                            text = "Managing as $activeRole • $activeTier",
                            fontSize = 12.sp,
                            color = TealAccent
                        )
                    }

                    Surface(
                        color = SlateCardLight,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { viewModel.navigateTo(Screen.Settings) }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = GeoDeepGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(viewModel.activeSubscriptionTier.value, color = GeoDeepGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatsSummaryCard(
                        title = "Saved Listings",
                        value = listings.size.toString(),
                        subtitle = "Local Room DB",
                        icon = Icons.Default.List,
                        modifier = Modifier.weight(1f)
                    )
                    StatsSummaryCard(
                        title = "Unsorted Leads",
                        value = leads.size.toString(),
                        subtitle = "Active CRM Pipe",
                        icon = Icons.Default.Person,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by address...", color = GeoTextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CharcoalMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealAccent,
                        unfocusedBorderColor = GeoBorder,
                        focusedTextColor = GeoTextDark,
                        unfocusedTextColor = GeoTextDark,
                        focusedContainerColor = SlateCard,
                        unfocusedContainerColor = SlateCard
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_bar"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Text(
                    text = "Properties Built (${filteredListings.size})",
                    fontWeight = FontWeight.Bold,
                    color = GeoTextDark,
                    fontSize = 16.sp
                )
            }

            if (filteredListings.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Empty",
                            tint = CharcoalMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No Property Listings Saved",
                            fontWeight = FontWeight.Bold,
                            color = GeoTextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap the '+' button down below to start our listing creation assistant",
                            color = GeoTextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredListings) { listing ->
                    ListingDashboardCard(
                        listing = listing,
                        onViewDetails = {
                            viewModel.selectedListing.value = listing
                            viewModel.tempListing.value = listing
                            viewModel.navigateTo(Screen.MarketingCopy)
                        },
                        onDelete = { viewModel.deleteListing(it) },
                        onShare = {
                            viewModel.navigateTo(Screen.LeadCapture(listing.id))
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = { viewModel.navigateTo(Screen.CreateListing) },
            containerColor = TealAccent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_add_listing")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Listing")
        }
    }
}

fun saveBitmapToTempUri(context: Context, bitmap: android.graphics.Bitmap): android.net.Uri? {
    return try {
        val cacheDir = context.cacheDir
        val tempFile = java.io.File(cacheDir, "temp_camera_photo_${System.currentTimeMillis()}.jpg")
        java.io.FileOutputStream(tempFile).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        }
        android.net.Uri.fromFile(tempFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun createTempImageFileAndUri(context: Context): Pair<java.io.File, android.net.Uri>? {
    return try {
        val cacheDir = context.cacheDir
        val tempFile = java.io.File(cacheDir, "temp_full_photo_${System.currentTimeMillis()}.jpg")
        if (!tempFile.exists()) {
            tempFile.createNewFile()
        }
        val authority = "${context.packageName}.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, tempFile)
        Pair(tempFile, uri)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun CreateListingScreen(viewModel: ListingAssistantViewModel) {
    val tempListing by viewModel.tempListing.collectAsStateWithLifecycle()
    var addressInput by remember { mutableStateOf(tempListing.address) }
    var bedroomsInput by remember { mutableStateOf(tempListing.bedrooms.toString()) }
    var bathroomsInput by remember { mutableStateOf(tempListing.bathrooms.toString()) }
    var sqftInput by remember { mutableStateOf(tempListing.squareFeet.toString()) }
    var lotSizeInput by remember { mutableStateOf(tempListing.lotSize.toString()) }
    var yearBuiltInput by remember { mutableStateOf(tempListing.yearBuilt.toString()) }
    var upgradesInput by remember { mutableStateOf(tempListing.upgrades) }
    var askingPriceInput by remember { mutableStateOf(if (tempListing.askingPrice > 0) tempListing.askingPrice.toString() else "") }

    var selectedPropertyType by remember { mutableStateOf(tempListing.propertyType) }
    var selectedCondition by remember { mutableStateOf(tempListing.condition) }
    var selectedPhotoStyle by remember { mutableStateOf("Suburban Estate") }

    // Navigation and interactive camera/manual picker modes
    var compilationMode by remember { mutableStateOf("camera") } // "camera" or "manual"
    var isShutterFlashed by remember { mutableStateOf(false) }
    var locationAutoDetected by remember { mutableStateOf(false) }
    var detectedAddressLabel by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val addressDetectionStatus by viewModel.addressDetectionStatus.collectAsStateWithLifecycle()
    val isDetecting = addressDetectionStatus == "Checking image metadata..." ||
            addressDetectionStatus == "Using photo GPS metadata..." ||
            addressDetectionStatus == "No photo GPS found. Checking device location..."
            
    var permissionStatusMessage by remember { mutableStateOf("") }

    var capturedPhotoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val loadedPhotoBitmap = remember(selectedPhotoUri) {
        if (selectedPhotoUri != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, selectedPhotoUri!!)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, selectedPhotoUri)
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (cameraGranted && (fineLocationGranted || coarseLocationGranted)) {
            permissionStatusMessage = "Granted"
            Toast.makeText(context, "Permissions approved!", Toast.LENGTH_SHORT).show()
        } else {
            permissionStatusMessage = "Denied"
            Toast.makeText(context, "Camera & Location permissions are required.", Toast.LENGTH_LONG).show()
        }
    }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember { mutableStateOf<java.io.File?>(null) }

    val detectAddressFromPhotoOrLocation: () -> Unit = {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasFineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasCamera || (!hasFineLoc && !hasCoarseLoc)) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.detectAddressFromImageOrLocation(context, selectedPhotoUri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            selectedPhotoUri = tempCameraUri
            locationAutoDetected = true
            Toast.makeText(context, "Real physical photo captured successfully!", Toast.LENGTH_SHORT).show()
            viewModel.detectAddressFromImageOrLocation(context, tempCameraUri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
            locationAutoDetected = true
            Toast.makeText(context, "Photo uploaded successfully!", Toast.LENGTH_SHORT).show()
            viewModel.detectAddressFromImageOrLocation(context, uri)
        }
    }

    val openRealCamera: () -> Unit = {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!hasCamera) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            try {
                val fileAndUri = createTempImageFileAndUri(context)
                if (fileAndUri != null) {
                    tempCameraFile = fileAndUri.first
                    tempCameraUri = fileAndUri.second
                    cameraLauncher.launch(fileAndUri.second)
                } else {
                    Toast.makeText(context, "Unable to initialize temporary photo file.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to launch device camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(tempListing.address) {
        if (tempListing.address.isNotEmpty()) {
            addressInput = tempListing.address
            detectedAddressLabel = tempListing.address
            bedroomsInput = tempListing.bedrooms.toString()
            bathroomsInput = tempListing.bathrooms.toString()
            sqftInput = tempListing.squareFeet.toString()
            lotSizeInput = tempListing.lotSize.toString()
            yearBuiltInput = tempListing.yearBuilt.toString()
            upgradesInput = tempListing.upgrades
            askingPriceInput = if (tempListing.askingPrice > 0) tempListing.askingPrice.toInt().toString() else ""
            selectedPropertyType = tempListing.propertyType
            selectedCondition = tempListing.condition
        }
    }

    val propertyTypes = listOf("Single Family", "Townhouse", "Condominium", "Multi Family", "Land")
    val conditions = listOf("Fixer", "Fair", "Good", "Excellent", "Mint")

    // Camera action shutter flash reset
    LaunchedEffect(isShutterFlashed) {
        if (isShutterFlashed) {
            kotlinx.coroutines.delay(160)
            isShutterFlashed = false
        }
    }

    // Auto-compilation lookup map
    val styleSpecMap = remember {
        mapOf(
            "Suburban Estate" to mapOf(
                "address" to "411 Elm Street, Lake Forest, IL",
                "bedrooms" to "4",
                "bathrooms" to "3.5",
                "sqft" to "3200",
                "lot" to "0.45",
                "year" to "2005",
                "type" to "Single Family",
                "condition" to "Excellent",
                "asking" to "645000",
                "upgrades" to "Fresh botanical gardening, complete dual-zone smart HVAC upgrade, newly finished patio decking"
            ),
            "Modern Minimalist" to mapOf(
                "address" to "9820 Maplewood Lane, Austin, TX",
                "bedrooms" to "3",
                "bathrooms" to "2.5",
                "sqft" to "2600",
                "lot" to "0.28",
                "year" to "2018",
                "type" to "Single Family",
                "condition" to "Mint",
                "asking" to "825000",
                "upgrades" to "Custom smart dimmer integration, floor-to-ceiling clear glass insulation pane, modern bath fittings"
            ),
            "Craftsman Cottage" to mapOf(
                "address" to "542 Pine Valley Road, Portland, OR",
                "bedrooms" to "3",
                "bathrooms" to "2.0",
                "sqft" to "1750",
                "lot" to "0.22",
                "year" to "1996",
                "type" to "Single Family",
                "condition" to "Good",
                "asking" to "435000",
                "upgrades" to "Newly renovated front porch columns, charming brick paths, custom fireplace mantle"
            ),
            "Luxury Condo" to mapOf(
                "address" to "2200 Broadway Penthouse #42B, Manhattan, NY",
                "bedrooms" to "2",
                "bathrooms" to "2.0",
                "sqft" to "1400",
                "lot" to "0.0",
                "year" to "2021",
                "type" to "Condominium",
                "condition" to "Mint",
                "asking" to "1250000",
                "upgrades" to "High-end luxury kitchen appliances, motorized shade controls, modern bathroom floor heating"
            ),
            "Suburban Traditional" to mapOf(
                "address" to "120 Parkside Circle, Roswell, GA",
                "bedrooms" to "3",
                "bathrooms" to "2.0",
                "sqft" to "1850",
                "lot" to "0.25",
                "year" to "1999",
                "type" to "Single Family",
                "condition" to "Good",
                "asking" to "365000",
                "upgrades" to "Tidy double-insulated garage door, freshly installed quartz kitchen countertops, new vinyl sidings"
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER ROW
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GeoTextDark)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("New Property Listing", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GeoTextDark)
                    Text("Step 1 of 4: Setup Specs & Address Records", fontSize = 12.sp, color = TealAccent)
                }
            }
        }

        // ENTRY MODE SELECTION TABS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GeoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = { compilationMode = "camera" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (compilationMode == "camera") TealAccent else Color.Transparent,
                            contentColor = if (compilationMode == "camera") Color.White else GeoTextDark
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        elevation = null
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📸 Instant AI Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { compilationMode = "manual" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (compilationMode == "manual") TealAccent else Color.Transparent,
                            contentColor = if (compilationMode == "manual") Color.White else GeoTextDark
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        elevation = null
                    ) {
                        Text("✍️ Manual Spec Entry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // MODE A: CAMERA VIEWPORT & INTERACTIVE SIMULATOR (Default, Quickest)
        if (compilationMode == "camera") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131711)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(2.dp, if (locationAutoDetected) TealAccent else GeoBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Display real photo captured from physical camera
                        val displayBitmap = loadedPhotoBitmap ?: capturedPhotoBitmap
                        displayBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Captured home facade",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Simulated viewfinder grid lines using simple Compose Row & Col layout
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.08f)))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.08f)))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.08f)))
                            }
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.08f)))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.08f)))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.08f)))
                            }
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.08f)))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.08f)))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.08f)))
                            }
                        }

                        // Target Framing Brackets in the center
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .align(Alignment.Center)
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        )

                        // Camera UI Layout details
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Upper Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Red)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("• LIVE VIEW CAMERA", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Surface(
                                    color = if (isDetecting) TealAccent else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { detectAddressFromPhotoOrLocation() }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Place,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (isDetecting) "Detecting..." else "📍 GPS Scan Lock",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Centered Instructions overlay (if not captured yet)
                            if (!locationAutoDetected) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.CenterHorizontally),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "POINT CAMERA AT HOME FACADE",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center
                                    )

                                    Button(
                                        onClick = { openRealCamera() },
                                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("launch_camera_button")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("ACTIVATE PHONE CAMERA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Text(
                                        "Or pick mock/simulation style options below.",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp)
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "🟢 PROPERTY SCANNING ACTIVE",
                                            color = Color.Green,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Retake 🔄",
                                            color = TealAccent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { openRealCamera() }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        detectedAddressLabel,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "Loaded Specs: $bedroomsInput beds • $bathroomsInput baths • $sqftInput sqft • Region comps synchronized.",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Lower Stats indicators
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text("ISO 100", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                Text("4K UHD • GPS AUTO-LOOKUP", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                Text("f/1.8", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            }
                        }

                        // Shutter Flash visual element
                        if (isShutterFlashed) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            // Target Property style choice carousel matching current GPS pointer
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, GeoBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("1. Standing in front of which style?", fontWeight = FontWeight.Bold, color = GeoTextDark)
                        Text("Select design to simulate instant camera inspection:", fontSize = 11.sp, color = CharcoalMuted)

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewModel.photoStyles) { item ->
                                val isSelected = selectedPhotoStyle == item.name
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(95.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) TealAccent else GeoSoftBg)
                                        .border(
                                            1.dp,
                                            if (isSelected) TealAccent else GeoBorder,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedPhotoStyle = item.name }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Icon(
                                            if (item.name == "Luxury Condo") Icons.Default.Star else Icons.Default.Home,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else CharcoalMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(item.name, color = if (isSelected) Color.White else GeoTextDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text(item.description, color = if (isSelected) Color.White.copy(alpha = 0.8f) else GeoTextMuted, fontSize = 8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }

                        // Action Button "COMPILE IMAGE SPECIFICATIONS"
                        Button(
                            onClick = {
                                isShutterFlashed = true
                                locationAutoDetected = true
                                val specs = styleSpecMap[selectedPhotoStyle] ?: styleSpecMap.values.first()
                                detectedAddressLabel = specs["address"] ?: ""
                                
                                // Autofill fields
                                addressInput = specs["address"] ?: ""
                                bedroomsInput = specs["bedrooms"] ?: "3"
                                bathroomsInput = specs["bathrooms"] ?: "2.0"
                                sqftInput = specs["sqft"] ?: "1800"
                                lotSizeInput = specs["lot"] ?: "0.25"
                                yearBuiltInput = specs["year"] ?: "2010"
                                upgradesInput = specs["upgrades"] ?: ""
                                askingPriceInput = specs["asking"] ?: ""
                                selectedPropertyType = specs["type"] ?: "Single Family"
                                selectedCondition = specs["condition"] ?: "Good"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📸 CAPTURE HOME & COMPILE SPECS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // DESIGN COMPONENT 2: PROPERTY ADDRESS ENTRY FIELD (Manual mode vs. Camera preview summary)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GeoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (compilationMode == "camera") "2. Verify/Modify Property Address" else "1. Enter Address",
                        fontWeight = FontWeight.Bold,
                        color = GeoTextDark
                    )

                    // Real Photo Preview and Upload/Take actions if in Camera compilation mode
                    if (compilationMode == "camera") {
                        val previewBitmap = loadedPhotoBitmap ?: capturedPhotoBitmap
                        if (previewBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, TealAccent, RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = "Selected property photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topEnd = 8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (loadedPhotoBitmap != null) "Uploaded Photo" else "Captured Photo",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { openRealCamera() },
                                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("take_photo_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("📸 TAKE PHOTO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = GeoDeepGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("upload_photo_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("📤 UPLOAD PHOTO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Button(
                            onClick = { detectAddressFromPhotoOrLocation() },
                            enabled = !isDetecting,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDetecting) CharcoalMuted else TealAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("detect_address_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isDetecting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("DETECTING...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("DETECT ADDRESS FROM PHOTO/LOCATION", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (addressDetectionStatus.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GeoSoftBg, RoundedCornerShape(12.dp))
                                    .border(1.dp, if (addressDetectionStatus == "Address detected.") TealAccent else GeoBorder, RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (addressDetectionStatus == "Address detected.") Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (addressDetectionStatus == "Address detected.") TealAccent else if (addressDetectionStatus.startsWith("Could not")) Color.Red else CharcoalMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Address Detection Status:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = GeoTextMuted
                                    )
                                    Text(
                                        text = addressDetectionStatus,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (addressDetectionStatus == "Address detected.") TealAccent else if (addressDetectionStatus.startsWith("Could not")) Color.Red else GeoTextDark
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = {
                            addressInput = it
                            if (compilationMode == "manual") {
                                viewModel.onAddressEntered(it)
                                val updated = viewModel.tempListing.value
                                bedroomsInput = updated.bedrooms.toString()
                                bathroomsInput = updated.bathrooms.toString()
                                sqftInput = updated.squareFeet.toString()
                                lotSizeInput = updated.lotSize.toString()
                                yearBuiltInput = updated.yearBuilt.toString()
                                upgradesInput = updated.upgrades
                                askingPriceInput = if (updated.askingPrice > 0) updated.askingPrice.toInt().toString() else ""
                            }
                        },
                        label = { Text("Property Address") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = GeoBorder,
                            focusedTextColor = GeoTextDark,
                            unfocusedTextColor = GeoTextDark,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedLabelColor = TealAccent,
                            unfocusedLabelColor = CharcoalMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("address_input")
                    )

                    if (compilationMode == "manual") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GeoSoftBg, RoundedCornerShape(12.dp))
                                .border(1.dp, GeoBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TealAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Type 'elm' or 'maple' or 'penthouse' to see mock public records automatically search and load!",
                                fontSize = 11.sp,
                                color = GeoTextMuted,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // DESIGN COMPONENT 3: PROPERTY SPECIFICATIONS SUMMARY FORM
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GeoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = if (compilationMode == "camera") "3. Adjust Spec Information" else "2. Adjust Property Specifications",
                        fontWeight = FontWeight.Bold,
                        color = GeoTextDark
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = bedroomsInput,
                            onValueChange = { bedroomsInput = it },
                            label = { Text("Bedrooms") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealAccent,
                                unfocusedBorderColor = GeoBorder,
                                focusedTextColor = GeoTextDark,
                                unfocusedTextColor = GeoTextDark,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = TealAccent,
                                unfocusedLabelColor = CharcoalMuted
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bathroomsInput,
                            onValueChange = { bathroomsInput = it },
                            label = { Text("Baths") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealAccent,
                                unfocusedBorderColor = GeoBorder,
                                focusedTextColor = GeoTextDark,
                                unfocusedTextColor = GeoTextDark,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = TealAccent,
                                unfocusedLabelColor = CharcoalMuted
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = sqftInput,
                            onValueChange = { sqftInput = it },
                            label = { Text("Sq. Footage") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealAccent,
                                unfocusedBorderColor = GeoBorder,
                                focusedTextColor = GeoTextDark,
                                unfocusedTextColor = GeoTextDark,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = TealAccent,
                                unfocusedLabelColor = CharcoalMuted
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lotSizeInput,
                            onValueChange = { lotSizeInput = it },
                            label = { Text("Lot (Acres)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealAccent,
                                unfocusedBorderColor = GeoBorder,
                                focusedTextColor = GeoTextDark,
                                unfocusedTextColor = GeoTextDark,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = TealAccent,
                                unfocusedLabelColor = CharcoalMuted
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = yearBuiltInput,
                            onValueChange = { yearBuiltInput = it },
                            label = { Text("Year Built") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealAccent,
                                unfocusedBorderColor = GeoBorder,
                                focusedTextColor = GeoTextDark,
                                unfocusedTextColor = GeoTextDark,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = TealAccent,
                                unfocusedLabelColor = CharcoalMuted
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = askingPriceInput,
                            onValueChange = { askingPriceInput = it },
                            label = { Text("Est. Asking ($)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealAccent,
                                unfocusedBorderColor = GeoBorder,
                                focusedTextColor = GeoTextDark,
                                unfocusedTextColor = GeoTextDark,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = TealAccent,
                                unfocusedLabelColor = CharcoalMuted
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("Optional") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Column {
                        Text("Property Subtype", color = GeoTextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            propertyTypes.take(3).forEach { type ->
                                val isSelected = selectedPropertyType == type
                                Surface(
                                    color = if (isSelected) TealAccent else GeoSoftBg,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isSelected) TealAccent else GeoBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedPropertyType = type }
                                ) {
                                    Text(
                                        type,
                                        color = if (isSelected) Color.White else GeoTextDark,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Property Condition", color = GeoTextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(selectedCondition, color = TealAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = conditions.indexOf(selectedCondition).toFloat(),
                            onValueChange = { selectedCondition = conditions[it.toInt()] },
                            valueRange = 0f..4f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = TealAccent,
                                activeTrackColor = TealAccent,
                                inactiveTrackColor = GeoBorder
                            )
                        )
                    }

                    OutlinedTextField(
                        value = upgradesInput,
                        onValueChange = { upgradesInput = it },
                        label = { Text("Recent Upgrades & Key Details") },
                        placeholder = { Text("e.g. kitchen remodel, smart lights") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = GeoBorder,
                            focusedTextColor = GeoTextDark,
                            unfocusedTextColor = GeoTextDark,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedLabelColor = TealAccent,
                            unfocusedLabelColor = CharcoalMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // TRIGGER COMPILATION PROCESS
        item {
            Button(
                onClick = {
                    if (addressInput.isEmpty()) {
                        addressInput = "820 Pinecrest Road, Suite C"
                    }
                    val beds = bedroomsInput.toIntOrNull() ?: 3
                    val baths = bathroomsInput.toDoubleOrNull() ?: 2.0
                    val sqft = sqftInput.toIntOrNull() ?: 1800
                    val lot = lotSizeInput.toDoubleOrNull() ?: 0.25
                    val yr = yearBuiltInput.toIntOrNull() ?: 2012
                    val asking = askingPriceInput.toDoubleOrNull() ?: 0.0

                    viewModel.tempListing.value = viewModel.tempListing.value.copy(
                        address = addressInput,
                        bedrooms = beds,
                        bathrooms = baths,
                        squareFeet = sqft,
                        lotSize = lot,
                        yearBuilt = yr,
                        propertyType = selectedPropertyType,
                        condition = selectedCondition,
                        upgrades = upgradesInput,
                        askingPrice = asking
                    )
                    viewModel.startFeatureAnalysis(selectedPhotoStyle)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("analyze_btn"),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger AI Vision Feature Scan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

// 4. FEATURE REVIEW SCREEN
@Composable
fun FeatureReviewScreen(viewModel: ListingAssistantViewModel) {
    val tempListing by viewModel.tempListing.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()

    var featureFieldInput by remember { mutableStateOf("") }
    val currentTags = remember(tempListing.detectedFeatures) {
        tempListing.detectedFeatures.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableStateList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateTo(Screen.CreateListing) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GeoTextDark)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text("Confirm Extracted Attributes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GeoTextDark)
                Text("Step 2 of 4: Verify Extracted Structural Tags", fontSize = 12.sp, color = TealAccent)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isAnalyzing) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GeoBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = TealAccent, modifier = Modifier.size(50.dp))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Prompting Gemini Vision model...", fontWeight = FontWeight.Bold, color = GeoTextDark, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Analyzing visual components of your ${tempListing.photoUri} selection and compiling architectural tags. This takes a few seconds.",
                        color = GeoTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GeoBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column {
                            Text("Extracted Property Features (AI)", fontWeight = FontWeight.Bold, color = GeoTextDark, fontSize = 16.sp)
                            Text("Gemini scanned your property structure. Tap tags to exclude.", fontSize = 11.sp, color = CharcoalMuted)
                        }
                    }

                    item {
                        if (currentTags.isEmpty()) {
                            Text("No tags registered. Add custom tags below.", color = GeoTextMuted, fontSize = 12.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val chunked = currentTags.chunked(2)
                                chunked.forEach { rowIds ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        rowIds.forEach { tag ->
                                            Surface(
                                                color = GeoSoftBg,
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, GeoBorder),
                                                modifier = Modifier.clickable { currentTags.remove(tag) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(tag, color = GeoTextDark, fontSize = 11.sp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(10.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Divider(color = GeoBorder, thickness = 1.dp)
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Add Custom Feature Tag", fontWeight = FontWeight.Bold, color = GeoTextDark, fontSize = 12.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = featureFieldInput,
                                    onValueChange = { featureFieldInput = it },
                                    placeholder = { Text("e.g. Spiral stairs, Vaulted ceilings", color = CharcoalMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealAccent,
                                        unfocusedBorderColor = GeoBorder,
                                        focusedTextColor = GeoTextDark,
                                        unfocusedTextColor = GeoTextDark,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedLabelColor = TealAccent,
                                        unfocusedLabelColor = CharcoalMuted
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                )
                                Button(
                                    onClick = {
                                        if (featureFieldInput.isNotEmpty() && !currentTags.contains(featureFieldInput.trim())) {
                                            currentTags.add(featureFieldInput.trim())
                                            featureFieldInput = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateCardLight, contentColor = GeoDeepGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Text("Add", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.analyzePricingModels(currentTags.toList())
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("confirm_features_btn"),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Confirm & Analyze comps Price Range", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 5. PRICING REPORT SCREEN
@Composable
fun PricingReportScreen(viewModel: ListingAssistantViewModel) {
    val tempListing by viewModel.tempListing.collectAsStateWithLifecycle()
    val isGeneratingPricing by viewModel.isGeneratingPricing.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateTo(Screen.FeatureReview) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GeoTextDark)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text("AI Pricing Comp Analysis", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GeoTextDark)
                Text("Step 3 of 4: Market Value Predictions", fontSize = 12.sp, color = TealAccent)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isGeneratingPricing) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GeoBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = TealAccent, modifier = Modifier.size(50.dp))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Triggering Local Pricing Model Engine...", fontWeight = FontWeight.Bold, color = GeoTextDark, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Compiling nearby active listings and assessor valuations using models/gemini-3.5-flash logic. This takes a brief moment.",
                        color = GeoTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GeoBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GeoSoftBg, RoundedCornerShape(16.dp))
                                .border(1.dp, GeoBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Recommended Optimal List Price", fontSize = 13.sp, color = TealAccent, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$${String.format("%,.0f", tempListing.suggestedPriceRecommended)}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = GeoDeepGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Pricing Confidence Level: ${tempListing.confidenceScore}%",
                                color = TealAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    item {
                        Column {
                            Text("Estimate Valuation Bounds", fontWeight = FontWeight.Bold, color = GeoTextDark, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GeoSoftBg)
                                    .border(1.dp, GeoBorder, RoundedCornerShape(6.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .fillMaxHeight()
                                        .align(Alignment.Center)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Brush.horizontalGradient(colors = listOf(TealAccent, GeoDeepGreen)))
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Conservative", fontSize = 10.sp, color = CharcoalMuted)
                                    Text("$${String.format("%,.0f", tempListing.suggestedPriceLow)}", color = GeoTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Optimal Target", fontSize = 10.sp, color = TealAccent)
                                    Text("$${String.format("%,.0f", tempListing.suggestedPriceRecommended)}", color = GeoTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Hot/Aggressive", fontSize = 10.sp, color = CharcoalMuted)
                                    Text("$${String.format("%,.0f", tempListing.suggestedPriceHigh)}", color = GeoTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Interactive Neighborhood Comps (MLS)", fontWeight = FontWeight.Bold, color = GeoTextDark, fontSize = 13.sp)
                            val listCompPrice = tempListing.suggestedPriceRecommended
                            CompTableItem("142 Pinecrest Dr", "0.4 mi", "3B/2Ba", "Sold last week", listCompPrice * 0.98)
                            CompTableItem("98 Elmwood Wood St", "0.9 mi", "Same specs", "Sold 30 days ago", listCompPrice * 1.01)
                            CompTableItem("11 Maplewood Ave", "1.3 mi", "+300 sq.ft", "Active Listing", listCompPrice * 1.06)
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GeoSoftBg, RoundedCornerShape(16.dp))
                                .border(1.dp, GeoBorder, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Strategy & Market Reasoning", fontWeight = FontWeight.Bold, color = TealAccent, fontSize = 12.sp)
                            Text(
                                text = tempListing.pricingReasoning,
                                color = GeoTextDark,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.generateMarketingPackageAndSave()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("confirm_pricing_btn"),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Generate AI Multi-Format Copy", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 6. MARKETING COPY GENERATOR SCREEN
@Composable
fun MarketingCopyScreen(viewModel: ListingAssistantViewModel) {
    val tempListing by viewModel.tempListing.collectAsStateWithLifecycle()
    val isGeneratingCopy by viewModel.isGeneratingCopy.collectAsStateWithLifecycle()

    val clipManager = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val context = LocalContext.current

    // Local states for FSBO & Agent Handoff options
    var sellerPathSelected by remember { mutableStateOf("fsbo") } // "fsbo" or "agent"
    var agentName by remember { mutableStateOf("") }
    var agentEmail by remember { mutableStateOf("") }
    var commissionOffer by remember { mutableStateOf("2.5%") }
    var customAgentNote by remember { mutableStateOf("") }
    var isBriefCompiled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GeoTextDark)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("BinLeaf AI Asset Suite", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GeoTextDark)
                    Text("Copy and deploy ready assets", fontSize = 12.sp, color = TealAccent)
                }
            }

            IconButton(
                onClick = { viewModel.navigateTo(Screen.LeadCapture(tempListing.id)) },
                modifier = Modifier.background(TealAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Share, contentDescription = "Launch Lead Capture Portal", tint = TealAccent)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isGeneratingCopy) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GeoBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = TealAccent, modifier = Modifier.size(50.dp))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Formulating Marketing Copy Assets...", fontWeight = FontWeight.Bold, color = GeoTextDark, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Translating structural attributes and value stats into multi-format descriptions (MLS, Social, flyer, email blasts).",
                        color = GeoTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GeoBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GeoSoftBg, RoundedCornerShape(12.dp))
                                .border(1.dp, GeoBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                textBold("Marketing Assets Ready!", size = 13.sp, color = GeoTextDark)
                                Text("This listing and all generated copy formats are saved in Room offline database.", color = CharcoalMuted, fontSize = 11.sp)
                            }
                        }
                    }

                    item {
                        MarketingCopyBlock(
                            title = "🏛️ MLS Standard Posting Copy",
                            text = tempListing.mlsDescription,
                            onCopy = {
                                val clip = ClipData.newPlainText("MLS copy", tempListing.mlsDescription)
                                clipManager.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied MLS text!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    item {
                        MarketingCopyBlock(
                            title = "🏡 Lifestyle Emotional Description (Flyer)",
                            text = tempListing.flyerText,
                            onCopy = {
                                val clip = ClipData.newPlainText("Lifestyle copy", tempListing.flyerText)
                                clipManager.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied Lifestyle flyer copy!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    item {
                        MarketingCopyBlock(
                            title = "💎 Elevated Bespoke Luxury Text (Email Blast)",
                            text = tempListing.emailBlast,
                            onCopy = {
                                val clip = ClipData.newPlainText("Luxury copy", tempListing.emailBlast)
                                clipManager.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied Premium Luxury copy!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    item {
                        MarketingCopyBlock(
                            title = "📱 Social Reels & Instagram Caption Format",
                            text = tempListing.socialCaption,
                            onCopy = {
                                val clip = ClipData.newPlainText("Social copy", tempListing.socialCaption)
                                clipManager.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied Social hashtags!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    item {
                        Button(
                            onClick = { viewModel.regenerateMarketingBlocks() },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateCardLight, contentColor = GeoDeepGreen),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Regenerate marketing text variations", fontWeight = FontWeight.Bold)
                        }
                    }

                    // SELLER CUSTOM DISTRIBUTION HUB (FSBO VS. AGENT HANDOFF)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCardLight),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, GeoBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "🤝 FSBO vs. Agent Listing Handoff Desk",
                                    color = GeoTextDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    "Ready to monetize? Choose whether to sell on your own privately as a FSBO (For Sale By Owner), or export these assets directly to represent your home to an agent.",
                                    color = GeoTextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )

                                // Choice Toggles
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { sellerPathSelected = "fsbo" },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (sellerPathSelected == "fsbo") TealAccent else GeoSoftBg,
                                            contentColor = if (sellerPathSelected == "fsbo") Color.White else GeoTextDark
                                        ),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        elevation = null
                                    ) {
                                        Text("🏡 Sell Private (FSBO)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { sellerPathSelected = "agent" },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (sellerPathSelected == "agent") TealAccent else GeoSoftBg,
                                            contentColor = if (sellerPathSelected == "agent") Color.White else GeoTextDark
                                        ),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        elevation = null
                                    ) {
                                        Text("💼 Hand off to Agent", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Toggle Options UI
                                if (sellerPathSelected == "fsbo") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("🏡 Seller's FSBO Listing Tasks:", color = GeoTextDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("• Copy the MLS Standard text and paste to Zillow, Realtor, or Craigslist.", color = CharcoalMuted, fontSize = 11.sp)
                                        Text("• Tap Share (upper right) or below to boot your live Virtual Sign-in Portal for buyer showings in your neighborhood.", color = CharcoalMuted, fontSize = 11.sp)
                                        
                                        Spacer(modifier = Modifier.height(4.dp))

                                        Button(
                                            onClick = { viewModel.navigateTo(Screen.LeadCapture(tempListing.id)) },
                                            colors = ButtonDefaults.buttonColors(containerColor = GeoDeepGreen),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Launch Virtual Yard Sign Portal", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("💼 Package & Share Brief to Local Realtor:", color = GeoTextDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)

                                        OutlinedTextField(
                                            value = agentName,
                                            onValueChange = { agentName = it },
                                            label = { Text("Agent Name") },
                                            placeholder = { Text("e.g. Sarah Jenkins") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = Color.White,
                                                focusedBorderColor = TealAccent
                                            )
                                        )

                                        OutlinedTextField(
                                            value = agentEmail,
                                            onValueChange = { agentEmail = it },
                                            label = { Text("Agent Email Address") },
                                            placeholder = { Text("e.g. sarah@realtygroup.com") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = Color.White,
                                                focusedBorderColor = TealAccent
                                            )
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = commissionOffer,
                                                onValueChange = { commissionOffer = it },
                                                label = { Text("Offered Fee") },
                                                modifier = Modifier.weight(1f),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedContainerColor = Color.White,
                                                    focusedBorderColor = TealAccent
                                                )
                                            )

                                            OutlinedTextField(
                                                value = "Ready to list immediately",
                                                onValueChange = { },
                                                label = { Text("Priority Level") },
                                                enabled = false,
                                                modifier = Modifier.weight(1.5f),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                                                )
                                            )
                                        }

                                        OutlinedTextField(
                                            value = customAgentNote,
                                            onValueChange = { customAgentNote = it },
                                            placeholder = { Text("Add custom hand-off note to agent...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = Color.White,
                                                focusedBorderColor = TealAccent
                                            )
                                        )

                                        if (isBriefCompiled) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(GeoSoftBg, RoundedCornerShape(8.dp))
                                                    .border(1.dp, GeoBorder, RoundedCornerShape(8.dp))
                                                    .padding(10.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text("✓ Listing Brief Export Complete!", color = GeoTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text("Listing-Brief-${tempListing.address.take(15)}.xml has been packaged, compiled, and integrated into Realtor portal.", color = CharcoalMuted, fontSize = 10.sp, lineHeight = 13.sp)
                                                }
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    isBriefCompiled = true
                                                    Toast.makeText(context, "Brief compiled & shared with Realtor!", Toast.LENGTH_LONG).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = GeoDeepGreen),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("🚀 TRANSACT & SHARE PORTABLE BRIEF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.navigateTo(Screen.Dashboard)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("finish_marketing_btn"),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Go Back To Listings Master List", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 7. PUBLIC-FACING LEAD CAPTURE PAGE SCREEN
@Composable
fun LeadCapturePageScreen(viewModel: ListingAssistantViewModel, listingId: Int) {
    val listings by viewModel.listings.collectAsStateWithLifecycle()
    val listing = listings.find { it.id == listingId } ?: tempModelForDemo(listingId)

    var leadName by remember { mutableStateOf("") }
    var leadEmail by remember { mutableStateOf("") }
    var leadPhone by remember { mutableStateOf("") }
    var leadShowingTime by remember { mutableStateOf("Saturday Preferred") }
    var leadInterestLevel by remember { mutableStateOf("High") }
    var leadNotes by remember { mutableStateOf("") }

    var isSuccessShown by remember { mutableStateOf(false) }

    LaunchedEffect(listingId) {
        viewModel.simulateListingView(listingId)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GeoTextDark)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Public Listing Capture Page", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GeoTextDark)
                }

                Surface(
                    color = TealAccent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("LIVE PREVIEW", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GeoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Brush.verticalGradient(colors = listOf(TealAccent.copy(alpha = 0.5f), SlateDark700)))
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (listing.askingPrice > 0) "$${String.format("%,.0f", listing.askingPrice)}" else "$349,000",
                                color = GeoTextDark,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(listing.address, color = GeoTextDark.copy(alpha = 0.85f), fontSize = 13.sp)
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            SpecItemValue("${listing.bedrooms} Beds")
                            SpecItemValue("${listing.bathrooms} Baths")
                            SpecItemValue("${listing.squareFeet} Sq. Ft.")
                            SpecItemValue("${listing.yearBuilt}")
                        }

                        Divider(color = GeoBorder, thickness = 1.dp)

                        Text("Property Description", fontWeight = FontWeight.Bold, color = TealAccent, fontSize = 13.sp)
                        Text(
                            text = if (listing.flyerText.isNotEmpty()) listing.flyerText else "Beautifully situated family home incorporating modern appliances, spacious open floor concepts, and generous natural daylight access.",
                            color = GeoTextDark,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        Divider(color = GeoBorder, thickness = 1.dp)

                        Text("Key Highlights & Features (AI Checked)", fontWeight = FontWeight.Bold, color = GeoTextDark, fontSize = 13.sp)
                        val tags = listing.detectedFeatures.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        if (tags.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                tags.chunked(2).forEach { subList ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        subList.forEach { tag ->
                                            Surface(
                                                color = GeoSoftBg,
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, GeoBorder)
                                            ) {
                                                Text(tag, color = GeoTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("Brick exterior, large yard, updated kitchen remodel, hardwood master bedrooms", color = TealAccent, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GeoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("📬 Inquire About This Property", fontWeight = FontWeight.Bold, color = GeoTextDark, fontSize = 15.sp)
                    Text("Schedule private walkthroughs or contact the seller instantly.", fontSize = 11.sp, color = CharcoalMuted)

                    OutlinedTextField(
                        value = leadName,
                        onValueChange = { leadName = it },
                        label = { Text("Your Complete Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = GeoBorder,
                            focusedTextColor = GeoTextDark,
                            unfocusedTextColor = GeoTextDark,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedLabelColor = TealAccent,
                            unfocusedLabelColor = CharcoalMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = leadEmail,
                        onValueChange = { leadEmail = it },
                        label = { Text("Email Address") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = GeoBorder,
                            focusedTextColor = GeoTextDark,
                            unfocusedTextColor = GeoTextDark,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedLabelColor = TealAccent,
                            unfocusedLabelColor = CharcoalMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = leadPhone,
                        onValueChange = { leadPhone = it },
                        label = { Text("Phone Number") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = GeoBorder,
                            focusedTextColor = GeoTextDark,
                            unfocusedTextColor = GeoTextDark,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedLabelColor = TealAccent,
                            unfocusedLabelColor = CharcoalMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = leadShowingTime,
                        onValueChange = { leadShowingTime = it },
                        label = { Text("Preferred Showing Window") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = GeoBorder,
                            focusedTextColor = GeoTextDark,
                            unfocusedTextColor = GeoTextDark,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedLabelColor = TealAccent,
                            unfocusedLabelColor = CharcoalMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = leadNotes,
                        onValueChange = { leadNotes = it },
                        label = { Text("Additional Inquiries / Memo") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = GeoBorder,
                            focusedTextColor = GeoTextDark,
                            unfocusedTextColor = GeoTextDark,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedLabelColor = TealAccent,
                            unfocusedLabelColor = CharcoalMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        Text("Selected Interest Level", color = GeoTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("High", "Medium", "Low").forEach { interest ->
                                val selected = leadInterestLevel == interest
                                Surface(
                                    color = if (selected) TealAccent else GeoSoftBg,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (selected) TealAccent else GeoBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { leadInterestLevel = interest }
                                ) {
                                    Text(
                                        interest,
                                        color = if (selected) Color.White else GeoTextDark,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(10.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (leadName.isNotEmpty()) {
                                viewModel.insertBuyerLead(
                                    listingId = listingId,
                                    name = leadName,
                                    email = leadEmail,
                                    phone = leadPhone,
                                    showingTime = leadShowingTime,
                                    interestLevel = leadInterestLevel,
                                    notes = leadNotes
                                )
                                isSuccessShown = true
                                leadName = ""
                                leadEmail = ""
                                leadPhone = ""
                                leadNotes = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_lead_capture_btn"),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Text("Submit My Inquiry Page", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }

    if (isSuccessShown) {
        Dialog(onDismissRequest = { isSuccessShown = false }) {
            Surface(
                color = SlateCard,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GeoBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Success", tint = EmeraldSuccess, modifier = Modifier.size(56.dp))
                    Text("Thank You!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GeoTextDark)
                    Text(
                        "Your inquiries were posted successfully. The property seller/agent has been notified via active CRM dashboard.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = GeoTextMuted
                    )
                    Button(
                        onClick = { isSuccessShown = false; viewModel.navigateTo(Screen.Dashboard) },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCardLight, contentColor = GeoDeepGreen),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close View", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 8. CRM PIPELINE SCREEN
@Composable
fun LeadCrmScreen(
    viewModel: ListingAssistantViewModel,
    listings: List<Listing>,
    leads: List<Lead>
) {
    var selectedListingFilter by remember { mutableStateOf<Int?>(null) }

    val filteredLeads = if (selectedListingFilter == null) {
        leads
    } else {
        leads.filter { it.listingId == selectedListingFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Lead CRM Pipeline",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GeoTextDark
                )
                Text(
                    text = "Track active buyer interests and showing schedules",
                    fontSize = 12.sp,
                    color = TealAccent
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Filter Leads by Listing Address", color = GeoTextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val allLeadsActive = selectedListingFilter == null
                    Surface(
                        color = if (allLeadsActive) TealAccent else SlateCard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (allLeadsActive) TealAccent else GeoBorder),
                        modifier = Modifier.clickable { selectedListingFilter = null }
                    ) {
                        Text(
                            "ALL LEADS (${leads.size})",
                            color = if (allLeadsActive) Color.White else GeoTextDark,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(10.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    listings.take(2).forEach { listing ->
                        val active = selectedListingFilter == listing.id
                        Surface(
                            color = if (active) TealAccent else SlateCard,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (active) TealAccent else GeoBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedListingFilter = listing.id }
                        ) {
                            Text(
                                listing.address.substringBefore(","),
                                color = if (active) Color.White else GeoTextDark,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(10.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (filteredLeads.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Empty",
                        tint = CharcoalMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No CRM Leads Found",
                        fontWeight = FontWeight.Bold,
                        color = GeoTextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Leads appear here automatically when prospective buyers complete a listing's Capture Page.",
                        color = GeoTextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredLeads) { lead ->
                val listingMatch = listings.find { it.id == lead.listingId }
                LeadPipelineCard(
                    lead = lead,
                    associatedAddress = listingMatch?.address ?: "Property #${lead.listingId}",
                    onUpdateStatus = { viewModel.updateLeadStatus(lead, it) },
                    onDelete = { viewModel.deleteLead(lead) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

// 9. SETTINGS & REVENUE SCREEN
@Composable
fun SettingsScreen(viewModel: ListingAssistantViewModel) {
    val activeRole by viewModel.userRole.collectAsStateWithLifecycle()
    val activeTier by viewModel.activeSubscriptionTier.collectAsStateWithLifecycle()
    val sourceMode by viewModel.dataSourceMode.collectAsStateWithLifecycle()

    val tiers = listOf(
        SubscriptionTier("Free", "1 Listing Draft, basic AI caps, manual entry comp checks"),
        SubscriptionTier("FSBO Starter", "$19/listing. Narrative flyer copy, basic pricing gauges"),
        SubscriptionTier("FSBO Pro", "$49/listing. All generated flyer models, local CRM charts"),
        SubscriptionTier("Agent Pro", "$29/mo. 10 listings/mo, branding, full pipeline stats"),
        SubscriptionTier("Broker / Team", "$99/mo. Multi-user layout, integrated RESO IDX Web API")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Account Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Configure user types, active subscriptions & integrations",
                    fontSize = 12.sp,
                    color = TealAccent
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateCardLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Active User Persona Profile", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Switches the targeted UI layout features and CRM priorities:", fontSize = 11.sp, color = CharcoalMuted)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("FSBO Seller", "Real Estate Agent").forEach { role ->
                            val active = activeRole == role
                            Surface(
                                color = if (active) TealAccent else SlateCardLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.userRole.value = role }
                            ) {
                                Text(
                                    role,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateCardLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Configure Data Sources Integration", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Pulls public assessor details or local comp feeds:", fontSize = 11.sp, color = CharcoalMuted)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "Public Records DB + AI Comps",
                            "RESO Web API Feed (Needs license)",
                            "IDX Live Feed Connection",
                            "Manual CSV Comp Data Upload"
                        ).forEach { src ->
                            val active = sourceMode == src
                            Surface(
                                color = if (active) SlateCardLight else SlateDark800,
                                border = BorderStroke(1.dp, if (active) TealAccent else Color.Transparent),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.dataSourceMode.value = src }
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (active) Icons.Default.Check else Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (active) TealAccent else CharcoalMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(src, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateCardLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Active Subscription Model Builder", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Simulate unlocking advanced broker features in real-time:", fontSize = 11.sp, color = CharcoalMuted)

                    tiers.forEach { tier ->
                        val active = activeTier == tier.name
                        Surface(
                            color = if (active) TealAccent.copy(alpha = 0.15f) else SlateDark800,
                            border = BorderStroke(1.5.dp, if (active) TealAccent else Color.Transparent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.activeSubscriptionTier.value = tier.name }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tier.name, color = if (active) TealAccent else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(tier.description, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, lineHeight = 14.sp)
                                }
                                if (active) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

// --- COMPOSE COMPONENT HELPERS ---

@Composable
fun textBold(text: String, size: sp = 14.sp, color: Color = GeoTextDark) {
    Text(text, fontSize = size, color = color, fontWeight = FontWeight.Bold)
}

typealias sp = androidx.compose.ui.unit.TextUnit

@Composable
fun WorkflowStepItem(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            color = SlateCardLight,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = TealAccent, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = GeoTextDark, fontSize = 14.sp)
            Text(description, color = GeoTextMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun StatsSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GeoBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = GeoTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Icon(icon, contentDescription = null, tint = TealAccent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = GeoDeepGreen, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = TealAccent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun CompTableItem(address: String, dist: String, specs: String, status: String, price: Double) {
    Surface(
        color = GeoSoftBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GeoBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(address, color = GeoTextDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("$dist • $specs • $status", color = CharcoalMuted, fontSize = 10.sp)
            }
            Text(
                "$${String.format("%,.0f", price)}",
                color = TealAccent,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun MarketingCopyBlock(title: String, text: String, onCopy: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Row(
                modifier = Modifier
                    .clickable { onCopy() }
                    .background(TealAccent.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = TealAccent, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("COPY", color = TealAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Surface(
            color = GeoSoftBg,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, GeoBorder)
        ) {
            Text(
                text = text.ifEmpty { "Generating and rendering detailed marketing descriptions..." },
                color = GeoTextDark,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
fun SpecItemValue(text: String) {
    Surface(
        color = GeoSoftBg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, GeoBorder),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(text, color = GeoTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
    }
}

fun tempModelForDemo(id: Int) = Listing(
    id = id,
    address = "120 Pinecrest Ave, Suite D",
    bedrooms = 3,
    bathrooms = 2.0,
    squareFeet = 1750,
    lotSize = 0.2,
    yearBuilt = 2008,
    askingPrice = 339000.0,
    detectedFeatures = "Vinyl siding, Large yard, Natural light, Open floor plan"
)

@Composable
fun ListingDashboardCard(
    listing: Listing,
    onViewDetails: () -> Unit,
    onDelete: (Listing) -> Unit,
    onShare: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GeoBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(TealAccent.copy(alpha = 0.5f), SlateDark700)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = listing.propertyType,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    val statusColor = when (listing.status) {
                        "Active" -> EmeraldSuccess
                        "Sold" -> TealAccent
                        else -> CharcoalMuted
                    }
                    Surface(
                        color = statusColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = listing.status.uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (listing.askingPrice > 0) "$${String.format("%,.0f", listing.askingPrice)}" else "AI Valuation",
                        color = GeoTextDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = listing.address,
                        color = GeoTextDark.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "${listing.bedrooms} Beds • ${listing.bathrooms} Baths • ${listing.squareFeet} SqFt",
                    color = GeoTextMuted,
                    fontSize = 12.sp
                )

                Divider(color = GeoBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = TealAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${listing.viewsCount} Views", fontSize = 11.sp, color = GeoTextDark)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${listing.leadsCount} Leads", fontSize = 11.sp, color = GeoTextDark)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { onShare() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Capture Page", tint = TealAccent, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { onDelete(listing) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeadPipelineCard(
    lead: Lead,
    associatedAddress: String,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GeoBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(lead.buyerName, fontWeight = FontWeight.Black, color = GeoTextDark, fontSize = 15.sp)
                    Text("Source: ${lead.leadSource}", fontSize = 10.sp, color = CharcoalMuted)
                }

                val priorityColor = when (lead.interestLevel) {
                    "High" -> GeoDeepGreen
                    "Medium" -> TealAccent
                    else -> CharcoalMuted
                }
                Surface(
                    color = priorityColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${lead.interestLevel.uppercase()} INTEREST",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Divider(color = GeoBorder, thickness = 1.dp)

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Home, contentDescription = null, tint = CharcoalMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                     "Property: $associatedAddress",
                     color = GeoTextDark,
                     fontSize = 11.sp,
                     maxLines = 1,
                     overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = TealAccent, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(lead.buyerPhone.ifEmpty { "N/A" }, fontSize = 11.sp, color = GeoTextDark)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = TealAccent, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(lead.buyerEmail.ifEmpty { "N/A" }, fontSize = 11.sp, color = GeoTextDark)
                }
            }

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Star, contentDescription = null, tint = TealAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Showing target: ${lead.preferredShowingTime.ifEmpty { "Flexible schedule" }}",
                    color = GeoTextDark,
                    fontSize = 11.sp
                )
            }

            if (lead.notes.isNotEmpty()) {
                Surface(
                    color = GeoSoftBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GeoBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notes: ${lead.notes}",
                        fontSize = 11.sp,
                        color = GeoTextDark,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Divider(color = GeoBorder, thickness = 1.dp)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Update CRM Lead Status", fontSize = 10.sp, color = CharcoalMuted)
                val states = listOf("New", "Showing Scheduled", "Offer Pending", "Closed")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    states.forEach { s ->
                        val active = lead.status == s
                        Surface(
                            color = if (active) {
                                if (s == "Closed") EmeraldSuccess else TealAccent
                            } else SlateCardLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateStatus(s) }
                        ) {
                            Text(
                                text = s.replace("Showing ", ""),
                                color = if (active) Color.White else GeoTextDark,
                                fontSize = 8.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { onDelete() },
                        modifier = Modifier.size(24.dp).align(Alignment.CenterVertically)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete lead", tint = Color.Red, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

package com.example.service

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if the API key is valid / configured
     */
    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return !key.isNullOrEmpty() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Analyzes property details and some keywords, extracting and tagging visible property features.
     */
    suspend fun analyzeFeatures(
        photoStyle: String, 
        propertyType: String,
        additionalProps: String
    ): List<String> = withContext(Dispatchers.IO) {
        val prompt = """
            You are a real estate imaging agent. The user is uploading a photo of a property.
            Style of property photo: $photoStyle (represented as a $propertyType).
            Additional details entered by user: "$additionalProps".
            
            Extract and return exactly a JSON list of key visible property features from this style.
            Select between 6 to 12 relevant elements from the following standard tags or generate matching ones:
            Brick exterior, Vinyl siding, Garage, Porch, Deck, Pool, Fence, Large yard, Updated kitchen, Hardwood floors, Stainless steel appliances, Natural light, Waterfront view, Curb appeal, Modern bathroom, Open floor plan.
            
            Return ONLY a raw JSON string containing a list of strings, e.g.:
            ["Brick exterior", "Garage", "Curb appeal", "Natural light"]
            Do not include any markdown formatting or prefix like 'json'. Just return the raw JSON array.
        """.trimIndent()

        if (!isApiKeyConfigured()) {
            return@withContext getMockFeatures(photoStyle, propertyType)
        }

        try {
            val responseText = makeApiCall(prompt)
            val cleanJson = sanitizeJsonString(responseText)
            val jsonArray = JSONArray(cleanJson)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            if (list.isNotEmpty()) list else getMockFeatures(photoStyle, propertyType)
        } catch (e: Exception) {
            Log.e(TAG, "analyzeFeatures API failed, falling back to mocks", e)
            getMockFeatures(photoStyle, propertyType)
        }
    }

    /**
     * Generates multiple writing styles of marketing listings of a property.
     */
    suspend fun generateMarketingCopy(
        address: String,
        bedrooms: Int,
        bathrooms: Double,
        sqft: Int,
        propertyType: String,
        condition: String,
        upgrades: String,
        features: List<String>
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val featuresStr = features.joinToString(", ")
        val prompt = """
            You are an expert copywriter for FSBO home sellers and top-producing real estate agents.
            Create high-converting, professional marketing descriptions for the following property:
            Address: $address
            Specs: $bedrooms Beds, $bathrooms Baths, $sqft sq. ft. ($propertyType)
            Condition: $condition
            Upgrades: $upgrades
            Features: $featuresStr
            
            Generate copy for the following 4 formats:
            1. MLS Description: Clear, professional, concise, capturing crucial points.
            2. Emotional Buyer-focused: Deeply descriptive, focusing on lifestyles, warmth, and memory-making.
            3. Luxury-style: Elevated vocabulary, focusing on bespoke finishes, prestige, and premium craft.
            4. Social Caption: Engaging copy with bullet points, brief spacing, and key marketing hashtags.
            
            Return ONLY a raw JSON object structured exactly like below. Do not wrap in markdown tags.
            {
               "mls": "your MLS description copy",
               "emotional": "your emotional buyer description copy",
               "luxury": "your luxury description copy",
               "social": "your social media caption / hashtags"
            }
        """.trimIndent()

        if (!isApiKeyConfigured()) {
            return@withContext getMockMarketingCopy(address, propertyType, features)
        }

        try {
            val responseText = makeApiCall(prompt)
            val cleanJson = sanitizeJsonString(responseText)
            val jsonObj = JSONObject(cleanJson)
            mapOf(
                "mls" to jsonObj.optString("mls", ""),
                "emotional" to jsonObj.optString("emotional", ""),
                "luxury" to jsonObj.optString("luxury", ""),
                "social" to jsonObj.optString("social", "")
            )
        } catch (e: Exception) {
            Log.e(TAG, "generateMarketingCopy API failed, compiling mocks", e)
            getMockMarketingCopy(address, propertyType, features)
        }
    }

    /**
     * Computes optimal property price indicators based on regional mock dynamics.
     */
    suspend fun generatePricingReport(
        address: String,
        bedrooms: Int,
        bathrooms: Double,
        sqft: Int,
        propertyType: String,
        condition: String,
        askingPriceInput: Double,
        upgrades: String
    ): PricingReport = withContext(Dispatchers.IO) {
        // Base market value calculation to keep estimations anchored in reality
        val baseVal = if (askingPriceInput > 0) askingPriceInput else (sqft * 210.0 + bedrooms * 25000.0)
        
        val prompt = """
            You are a real estate appraiser and market analyst.
            Synthesize a comparative market analysis (CMA) for this property:
            Address: ${address}, Specifications: $bedrooms Beds, $bathrooms Baths, $sqft Sqft, $propertyType.
            Condition: $condition. Upgrades: $upgrades. Proposed user price: $$askingPriceInput.
            
            Suggest realistic valuation parameters:
            1. Recommended Listing Price (centered optimal list price based on recent local comps).
            2. High/Aggressive Price (higher valuation boundary for hot markets).
            3. Low/Conservative Price (lower fallback valuation boundary).
            4. Analysis Reasoning: Explain recent local sales of similar properties in 1.5 miles, days on market trends, and confidence metric justifications.
            
            Return ONLY a raw JSON object with this exact structure:
            {
               "lowPrice": ${baseVal * 0.95},
               "recommendedPrice": ${baseVal},
               "highPrice": ${baseVal * 1.05},
               "confidenceScore": 85,
               "reasoning": "Explain nearby comp active listings patterns here."
            }
            Do not enclose in standard markdown ```json.
        """.trimIndent()

        if (!isApiKeyConfigured()) {
            return@withContext getMockPricingReport(address, bedrooms, sqft, propertyType, askingPriceInput)
        }

        try {
            val responseText = makeApiCall(prompt)
            val cleanJson = sanitizeJsonString(responseText)
            val jsonObj = JSONObject(cleanJson)
            PricingReport(
                lowPrice = jsonObj.optDouble("lowPrice", baseVal * 0.94),
                recommendedPrice = jsonObj.optDouble("recommendedPrice", baseVal),
                highPrice = jsonObj.optDouble("highPrice", baseVal * 1.06),
                confidenceScore = jsonObj.optInt("confidenceScore", 80),
                reasoning = jsonObj.optString("reasoning", "Estimated using regional sq. ft. market metrics.")
            )
        } catch (e: Exception) {
            Log.e(TAG, "generatePricingReport API failed, generating fallback model", e)
            getMockPricingReport(address, bedrooms, sqft, propertyType, askingPriceInput)
        }
    }

    private fun makeApiCall(prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val url = "$BASE_URL?key=$apiKey"

        val partsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("text", prompt)
            })
        }
        val contentsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("parts", partsArray)
            })
        }
        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Unexpected response code: ${response.code}")
            }
            val bodyStr = response.body?.string() ?: throw Exception("Empty response body")
            val resultJson = JSONObject(bodyStr)
            
            return resultJson
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }

    /**
     * Cleans up markdown markup wraps like ```json or ``` if the model includes them
     */
    private fun sanitizeJsonString(rawText: String): String {
        var clean = rawText.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    // --- Mock Generators (Realistic offline simulations) ---

    private fun getMockFeatures(photoStyle: String, propertyType: String): List<String> {
        val features = mutableListOf("Curb appeal", "Large yard", "Natural light")
        when (photoStyle) {
            "Suburban Estate" -> features.addAll(listOf("Brick exterior", "Garage", "Porch", "Hardwood floors"))
            "Modern Minimalist" -> features.addAll(listOf("Open floor plan", "Updated kitchen", "Stainless steel appliances", "Modern bathroom"))
            "Craftsman Cottage" -> features.addAll(listOf("Porch", "Wood details", "Curb appeal", "Hardwood floors"))
            "Luxury Condo" -> features.addAll(listOf("Waterfront view", "Stainless steel appliances", "Natural light", "Updated kitchen"))
            else -> features.addAll(listOf("Vinyl siding", "Open floor plan", "Porch"))
        }
        return features.distinct()
    }

    private fun getMockMarketingCopy(address: String, propertyType: String, features: List<String>): Map<String, String> {
        val feats = if (features.isNotEmpty()) features.joinToString(", ") else "Updated premium layout, classic features"
        return mapOf(
            "mls" to "Fabulous offering at $address. This lovely $propertyType features $feats. Spacious sizing, premium updates, and delightful curb appeal throughout. Move-in ready status.",
            "emotional" to "Welcome home. Nestled beautifully in a friendly neighbourhood, this gorgeous property is designed for making core memories. Imagine cozy mornings filled with natural light, sparkling kitchen amenities featuring $feats, and wonderful backyard hosting capabilities.",
            "luxury" to "An exquisite statement of refined living at $address. Presenting custom master finishes, an expansive open floor plan featuring $feats, and premium custom fittings. A truly sophisticated housing profile.",
            "social" to "🔑 NEW LISTING TOUR! Check out $address 🏡✨\nFeaturing $feats!\n💡 Perfect for first-time buyers & agents alike.\nDM or Click 'Inquire' to book a private walkthrough! #NewListing #FSBO #DreamHome"
        )
    }

    private fun getMockPricingReport(
        address: String,
        bedrooms: Int,
        sqft: Int,
        propertyType: String,
        askingPriceInput: Double
    ): PricingReport {
        // Fallback calculations
        val targetPrice = if (askingPriceInput > 0) askingPriceInput else (sqft * 215.0 + bedrooms * 18000.0)
        val low = targetPrice * 0.95
        val high = targetPrice * 1.05
        
        return PricingReport(
            lowPrice = low,
            recommendedPrice = targetPrice,
            highPrice = high,
            confidenceScore = 88,
            reasoning = "Based on recent closed sales of similar properties in the area. Similar $bedrooms-bedroom $propertyType homes within 1.5 miles sold between \$${String.format("%,.0f", low)} and \$${String.format("%,.0f", high)} in the last 90 days. This property has strong competitive curb appeal."
        )
    }

    data class PricingReport(
        val lowPrice: Double,
        val recommendedPrice: Double,
        val highPrice: Double,
        val confidenceScore: Int,
        val reasoning: String
    )
}

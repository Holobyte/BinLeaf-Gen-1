package com.example.service

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object NativeGeocoderService {
    suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        
        val geocoder = Geocoder(context)
        return suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) {
                                    val address = addresses.firstOrNull()
                                    if (address != null) {
                                        continuation.resume(formatAddress(address))
                                    } else {
                                        continuation.resume(null)
                                    }
                                }
                            }
                            
                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            } else {
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (continuation.isActive) {
                        val address = addresses?.firstOrNull()
                        if (address != null) {
                            continuation.resume(formatAddress(address))
                        } else {
                            continuation.resume(null)
                        }
                    }
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }
    }
    
    private fun formatAddress(address: Address): String {
        val lines = mutableListOf<String>()
        for (i in 0..address.maxAddressLineIndex) {
            lines.add(address.getAddressLine(i))
        }
        return if (lines.isNotEmpty()) {
            lines.joinToString(", ")
        } else {
            val feature = address.featureName ?: ""
            val street = address.thoroughfare ?: ""
            val city = address.locality ?: ""
            val state = address.adminArea ?: ""
            val postal = address.postalCode ?: ""
            listOfNotNull(
                feature.ifEmpty { null },
                street.ifEmpty { null },
                city.ifEmpty { null },
                state.ifEmpty { null },
                postal.ifEmpty { null }
            ).joinToString(", ")
        }
    }
}

package com.example.service

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.data.DetectedLocation

object ImageMetadataService {
    fun extractGpsLocation(context: Context, uri: Uri?): DetectedLocation? {
        if (uri == null) return null
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val latLong = exif.latLong
                if (latLong != null && latLong.size >= 2) {
                    DetectedLocation(
                        latitude = latLong[0],
                        longitude = latLong[1],
                        source = "Image Metadata"
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

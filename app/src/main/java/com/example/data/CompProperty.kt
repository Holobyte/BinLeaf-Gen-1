package com.example.data

import org.json.JSONArray
import org.json.JSONObject

data class CompProperty(
    val address: String,
    val soldOrListPrice: Double,
    val soldOrListDate: String,
    val bedrooms: Int,
    val bathrooms: Double,
    val squareFeet: Int,
    val distanceMiles: Double,
    val condition: String,
    val notes: String
) {
    val pricePerSqFt: Double
        get() = if (squareFeet > 0) soldOrListPrice / squareFeet else 0.0

    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("address", address)
        obj.put("soldOrListPrice", soldOrListPrice)
        obj.put("soldOrListDate", soldOrListDate)
        obj.put("bedrooms", bedrooms)
        obj.put("bathrooms", bathrooms)
        obj.put("squareFeet", squareFeet)
        obj.put("distanceMiles", distanceMiles)
        obj.put("condition", condition)
        obj.put("notes", notes)
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): CompProperty {
            return CompProperty(
                address = obj.optString("address", ""),
                soldOrListPrice = obj.optDouble("soldOrListPrice", 0.0),
                soldOrListDate = obj.optString("soldOrListDate", ""),
                bedrooms = obj.optInt("bedrooms", 3),
                bathrooms = obj.optDouble("bathrooms", 2.0),
                squareFeet = obj.optInt("squareFeet", 1800),
                distanceMiles = obj.optDouble("distanceMiles", 0.5),
                condition = obj.optString("condition", "Good"),
                notes = obj.optString("notes", "")
            )
        }

        fun listToJsonString(list: List<CompProperty>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJsonObject()) }
            return arr.toString()
        }

        fun listFromJsonString(jsonStr: String): List<CompProperty> {
            val list = mutableListOf<CompProperty>()
            if (jsonStr.isBlank()) return list
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(fromJsonObject(obj))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return list
        }
    }
}

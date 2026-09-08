package com.example

object BarcodeParser {
    
    data class ParsedBarcode(
        val stockCode: String = "",
        val stockName: String = "",
        val unit: String = "",
        val quantity: Int = 0,
        val brand: String = "",
        val barcode: String = "",
        val location: String = "",
        val stockLocation: String = "",
        val description: String = "",
        val isFullData: Boolean = false
    )

    fun parse(rawText: String): ParsedBarcode {
        if (rawText.contains("|")) {
            val parts = rawText.split("|")
            return ParsedBarcode(
                stockCode = parts.getOrNull(0) ?: "",
                stockName = parts.getOrNull(1) ?: "",
                unit = parts.getOrNull(2) ?: "",
                quantity = parts.getOrNull(3)?.toIntOrNull() ?: 0,
                brand = parts.getOrNull(4) ?: "",
                barcode = parts.getOrNull(5) ?: "",
                location = parts.getOrNull(6) ?: "",
                stockLocation = parts.getOrNull(7) ?: "",
                description = parts.getOrNull(8) ?: "",
                isFullData = true
            )
        } else {
            // It's just a simple barcode string
            return ParsedBarcode(
                barcode = rawText.trim(),
                isFullData = false
            )
        }
    }
}

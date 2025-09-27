package com.example.barcodescanner.model

data class StockItem(
    val id: Long = 0,
    val barcode: String,
    val brand: String,
    val purchasePrice: Double,
    val stockDate: Long,
    val quantity: Int = 1,
    val productType: String = ""
)
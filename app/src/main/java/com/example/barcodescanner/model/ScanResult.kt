package com.example.barcodescanner.model

data class ScanResult(
    val id: Long = 0,
    val barcode: String,
    val brand: String,
    val salePrice: Double,
    val scanDate: Long
)
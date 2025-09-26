package com.example.barcodescanner.model

data class ExportData(
    val version: String = "1.0",
    val exportDate: Long = System.currentTimeMillis(),
    val stockItems: List<StockItem> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val scanResults: List<ScanResult> = emptyList()
)
package com.example.barcodescanner.model

data class Notification(
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: String, // "LOW_STOCK", "INFO", "WARNING"
    val relatedBarcode: String? = null,
    val isRead: Boolean = false,
    val createdDate: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_LOW_STOCK = "LOW_STOCK"
        const val TYPE_INFO = "INFO"
        const val TYPE_WARNING = "WARNING"
    }
}
package com.example.barcodescanner.model

data class Transaction(
    val id: Long = 0,
    val brand: String,
    val productType: String, // cam, çerçeve, lens
    val transactionType: String, // satış, stok girdisi
    val amount: Double, // alış fiyatı veya satış fiyatı
    val barcode: String,
    val transactionDate: Long,
    val quantity: Int = 1,
    val purchasePrice: Double = 0.0,
    val date: Long = transactionDate
) {
    companion object {
        const val TYPE_SALE = "Satış"
        const val TYPE_STOCK_ENTRY = "Stok Girdisi"
        
        const val PRODUCT_TYPE_GLASS = "Cam"
        const val PRODUCT_TYPE_FRAME = "Çerçeve"
        const val PRODUCT_TYPE_LENS = "Lens"
    }
}
package com.example.barcodescanner.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.barcodescanner.database.DatabaseHelper
import com.example.barcodescanner.model.StockItem

class StockItemRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)
    private val notificationRepository = NotificationRepository(context)

    fun insertStockItem(stockItem: StockItem): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_STOCK_BARCODE, stockItem.barcode)
            put(DatabaseHelper.COLUMN_STOCK_BRAND, stockItem.brand)
            put(DatabaseHelper.COLUMN_STOCK_PURCHASE_PRICE, stockItem.purchasePrice)
            put(DatabaseHelper.COLUMN_STOCK_DATE, stockItem.stockDate)
            put(DatabaseHelper.COLUMN_STOCK_QUANTITY, stockItem.quantity)
        }
        
        val id = db.insert(DatabaseHelper.TABLE_STOCK_ITEMS, null, values)
        db.close()
        
        // Yeni eklenen stok adeti 3'e düşükse bildirim oluştur
        if (id != -1L && stockItem.quantity <= 3) {
            notificationRepository.createLowStockNotification(stockItem.brand, stockItem.barcode, stockItem.quantity)
        }
        
        return id
    }

    fun getAllStockItems(): List<StockItem> {
        val stockItems = mutableListOf<StockItem>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_STOCK_ITEMS,
            null,
            null,
            null,
            null,
            null,
            "${DatabaseHelper.COLUMN_STOCK_DATE} DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                val stockItem = StockItem(
                    id = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK_ID)),
                    barcode = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK_BARCODE)),
                    brand = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK_BRAND)),
                    purchasePrice = getDouble(getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK_PURCHASE_PRICE)),
                    stockDate = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK_DATE)),
                    quantity = getInt(getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK_QUANTITY))
                )
                stockItems.add(stockItem)
            }
        }
        cursor.close()
        db.close()
        return stockItems
    }

    fun deleteStockItem(id: Long): Boolean {
        val db = dbHelper.writableDatabase
        val deletedRows = db.delete(
            DatabaseHelper.TABLE_STOCK_ITEMS,
            "${DatabaseHelper.COLUMN_STOCK_ID} = ?",
            arrayOf(id.toString())
        )
        db.close()
        return deletedRows > 0
    }

    fun getStockItemByBarcode(barcode: String): StockItem? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_STOCK_ITEMS,
            null,
            "${DatabaseHelper.COLUMN_STOCK_BARCODE} = ?",
            arrayOf(barcode),
            null,
            null,
            "${DatabaseHelper.COLUMN_STOCK_DATE} DESC",
            "1"
        )

        var stockItem: StockItem? = null
        with(cursor) {
            if (moveToFirst()) {
                stockItem = StockItem(
                    id = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK_ID)),
                    barcode = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK_BARCODE)),
                    brand = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK_BRAND)),
                    purchasePrice = getDouble(getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK_PURCHASE_PRICE)),
                    stockDate = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK_DATE)),
                    quantity = getInt(getColumnIndexOrThrow(DatabaseHelper.COLUMN_STOCK_QUANTITY))
                )
            }
        }
        cursor.close()
        db.close()
        return stockItem
    }

    fun updateStockItem(stockItem: StockItem): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_STOCK_BRAND, stockItem.brand)
            put(DatabaseHelper.COLUMN_STOCK_PURCHASE_PRICE, stockItem.purchasePrice)
            put(DatabaseHelper.COLUMN_STOCK_QUANTITY, stockItem.quantity)
        }
        
        val rowsAffected = db.update(
            DatabaseHelper.TABLE_STOCK_ITEMS,
            values,
            "${DatabaseHelper.COLUMN_STOCK_ID} = ?",
            arrayOf(stockItem.id.toString())
        )
        db.close()
        
        // Stok adeti 3'e düştüyse bildirim oluştur
        if (rowsAffected > 0 && stockItem.quantity <= 3) {
            notificationRepository.createLowStockNotification(stockItem.brand, stockItem.barcode, stockItem.quantity)
        }
        
        return rowsAffected > 0
    }
    
    fun clearAllStockItems() {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_STOCK_ITEMS, null, null)
        db.close()
    }
}
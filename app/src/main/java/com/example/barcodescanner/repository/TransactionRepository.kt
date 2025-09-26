package com.example.barcodescanner.repository

import android.util.Log
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.barcodescanner.database.DatabaseHelper
import com.example.barcodescanner.model.Transaction

class TransactionRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    fun insertTransaction(transaction: Transaction): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_TRANSACTION_BRAND, transaction.brand)
            put(DatabaseHelper.COLUMN_TRANSACTION_PRODUCT_TYPE, transaction.productType)
            put(DatabaseHelper.COLUMN_TRANSACTION_TYPE, transaction.transactionType)
            put(DatabaseHelper.COLUMN_TRANSACTION_AMOUNT, transaction.amount)
            put(DatabaseHelper.COLUMN_TRANSACTION_BARCODE, transaction.barcode)
            put(DatabaseHelper.COLUMN_TRANSACTION_DATE, transaction.transactionDate)
            put(DatabaseHelper.COLUMN_TRANSACTION_QUANTITY, transaction.quantity)
            put(DatabaseHelper.COLUMN_TRANSACTION_PURCHASE_PRICE, transaction.purchasePrice)
        }
        
        val id = db.insert(DatabaseHelper.TABLE_TRANSACTIONS, null, values)
        db.close()
        return id
    }

    fun getRecentTransactions(limit: Int = -1): List<Transaction> {
        return try {
            val transactions = mutableListOf<Transaction>()
            val db = dbHelper.readableDatabase
            val cursor: Cursor = db.query(
                DatabaseHelper.TABLE_TRANSACTIONS,
                null,
                null,
                null,
                null,
                null,
                "${DatabaseHelper.COLUMN_TRANSACTION_DATE} DESC",
                if (limit > 0) limit.toString() else null
            )

            with(cursor) {
                while (moveToNext()) {
                    val transaction = Transaction(
                        id = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_ID)),
                        brand = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_BRAND)),
                        productType = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_PRODUCT_TYPE)),
                        transactionType = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_TYPE)),
                        amount = getDouble(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_AMOUNT)),
                        barcode = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_BARCODE)),
                        transactionDate = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_DATE))
                    )
                    transactions.add(transaction)
                }
            }
            cursor.close()
            db.close()
            android.util.Log.d("TransactionRepository", "Retrieved ${transactions.size} transactions")
            for (transaction in transactions) {
                android.util.Log.d("TransactionRepository", "DB Transaction: ${transaction.brand} - ${transaction.productType}")
            }
            transactions
        } catch (e: Exception) {
            // Tablo yoksa veya hata varsa boş liste döndür
            android.util.Log.e("TransactionRepository", "Error getting recent transactions", e)
            emptyList()
        }
    }

    fun getTotalSales(): Double {
        return try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT SUM(${DatabaseHelper.COLUMN_TRANSACTION_AMOUNT}) FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ?",
                arrayOf(Transaction.TYPE_SALE)
            )
            
            var total = 0.0
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0)
            }
            cursor.close()
            db.close()
            total
        } catch (e: Exception) {
            // Tablo yoksa veya hata varsa 0 döndür
            0.0
        }
    }

    fun getTodaySales(): Double {
        return try {
            val db = dbHelper.readableDatabase
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            val cursor = db.rawQuery(
                "SELECT SUM(${DatabaseHelper.COLUMN_TRANSACTION_AMOUNT}) FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ? AND ${DatabaseHelper.COLUMN_TRANSACTION_DATE} >= ?",
                arrayOf(Transaction.TYPE_SALE, startOfDay.toString())
            )
            
            var total = 0.0
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0)
            }
            cursor.close()
            db.close()
            total
        } catch (e: Exception) {
            0.0
        }
    }

    fun getTodayNetIncome(): Double {
        return try {
            val db = dbHelper.readableDatabase
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            // Bugünkü satışları al
            val salesCursor = db.rawQuery(
                "SELECT SUM(${DatabaseHelper.COLUMN_TRANSACTION_AMOUNT}) FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ? AND ${DatabaseHelper.COLUMN_TRANSACTION_DATE} >= ?",
                arrayOf(Transaction.TYPE_SALE, startOfDay.toString())
            )
            
            var sales = 0.0
            if (salesCursor.moveToFirst()) {
                sales = salesCursor.getDouble(0)
            }
            salesCursor.close()
            
            // Bugünkü stok girişlerini al
            val stockCursor = db.rawQuery(
                "SELECT SUM(${DatabaseHelper.COLUMN_TRANSACTION_AMOUNT}) FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ? AND ${DatabaseHelper.COLUMN_TRANSACTION_DATE} >= ?",
                arrayOf(Transaction.TYPE_STOCK_ENTRY, startOfDay.toString())
            )
            
            var stockEntries = 0.0
            if (stockCursor.moveToFirst()) {
                stockEntries = stockCursor.getDouble(0)
            }
            stockCursor.close()
            db.close()
            
            sales - stockEntries
        } catch (e: Exception) {
            0.0
        }
    }

    fun getTodaySalesCount(): Int {
        return try {
            val db = dbHelper.readableDatabase
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ? AND ${DatabaseHelper.COLUMN_TRANSACTION_DATE} >= ?",
                arrayOf(Transaction.TYPE_SALE, startOfDay.toString())
            )
            
            var count = 0
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
            db.close()
            count
        } catch (e: Exception) {
            0
        }
    }

    fun getTotalStockValue(): Double {
        return try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT SUM(${DatabaseHelper.COLUMN_TRANSACTION_AMOUNT}) FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ?",
                arrayOf(Transaction.TYPE_STOCK_ENTRY)
            )
            
            var total = 0.0
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0)
            }
            cursor.close()
            db.close()
            total
        } catch (e: Exception) {
            // Tablo yoksa veya hata varsa 0 döndür
            0.0
        }
    }

    fun getTotalSalesCount(): Int {
        return try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ?",
                arrayOf(Transaction.TYPE_SALE)
            )
            
            var count = 0
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
            db.close()
            count
        } catch (e: Exception) {
            // Tablo yoksa veya hata varsa 0 döndür
            0
        }
    }

    fun getNetIncome(): Double {
        return try {
            val db = dbHelper.readableDatabase
            
            // Satış işlemlerini getir
            val salesCursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COLUMN_TRANSACTION_BARCODE}, ${DatabaseHelper.COLUMN_TRANSACTION_AMOUNT} FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ?",
                arrayOf(Transaction.TYPE_SALE)
            )
            
            var netIncome = 0.0
            
            while (salesCursor.moveToNext()) {
                val barcode = salesCursor.getString(0)
                val salePrice = salesCursor.getDouble(1)
                
                // Bu barkod için stok girişindeki alım fiyatını bul
                val stockCursor = db.rawQuery(
                    "SELECT ${DatabaseHelper.COLUMN_TRANSACTION_AMOUNT} FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ? AND ${DatabaseHelper.COLUMN_TRANSACTION_BARCODE} = ? ORDER BY ${DatabaseHelper.COLUMN_TRANSACTION_DATE} DESC LIMIT 1",
                    arrayOf(Transaction.TYPE_STOCK_ENTRY, barcode)
                )
                
                if (stockCursor.moveToFirst()) {
                    val purchasePrice = stockCursor.getDouble(0)
                    netIncome += (salePrice - purchasePrice)
                } else {
                    // Eğer stok girişi bulunamazsa, sadece satış fiyatını ekle
                    netIncome += salePrice
                }
                stockCursor.close()
            }
            
            salesCursor.close()
            db.close()
            netIncome
        } catch (e: Exception) {
            // Hata durumunda 0 döndür
            0.0
        }
    }

    fun getDailyNetIncome(): Double {
        return try {
            val db = dbHelper.readableDatabase
            val startOfDay = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))
            
            // Bugünkü satış işlemlerini getir
            val salesCursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COLUMN_TRANSACTION_BARCODE}, ${DatabaseHelper.COLUMN_TRANSACTION_AMOUNT} FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ? AND ${DatabaseHelper.COLUMN_TRANSACTION_DATE} >= ?",
                arrayOf(Transaction.TYPE_SALE, startOfDay.toString())
            )
            
            var netIncome = 0.0
            
            while (salesCursor.moveToNext()) {
                val barcode = salesCursor.getString(0)
                val salePrice = salesCursor.getDouble(1)
                
                // Bu barkod için stok girişindeki alım fiyatını bul
                val stockCursor = db.rawQuery(
                    "SELECT ${DatabaseHelper.COLUMN_TRANSACTION_AMOUNT} FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ? AND ${DatabaseHelper.COLUMN_TRANSACTION_BARCODE} = ? ORDER BY ${DatabaseHelper.COLUMN_TRANSACTION_DATE} DESC LIMIT 1",
                    arrayOf(Transaction.TYPE_STOCK_ENTRY, barcode)
                )
                
                if (stockCursor.moveToFirst()) {
                    val purchasePrice = stockCursor.getDouble(0)
                    netIncome += (salePrice - purchasePrice)
                } else {
                    // Eğer stok girişi bulunamazsa, sadece satış fiyatını ekle
                    netIncome += salePrice
                }
                stockCursor.close()
            }
            
            salesCursor.close()
            db.close()
            netIncome
        } catch (e: Exception) {
            // Hata durumunda 0 döndür
            0.0
        }
    }

    fun getProductTypeByBarcode(barcode: String): String? {
        return try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COLUMN_TRANSACTION_PRODUCT_TYPE} FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_BARCODE} = ? ORDER BY ${DatabaseHelper.COLUMN_TRANSACTION_DATE} DESC LIMIT 1",
                arrayOf(barcode)
            )
            
            var productType: String? = null
            if (cursor.moveToFirst()) {
                productType = cursor.getString(0)
            }
            cursor.close()
            db.close()
            productType
        } catch (e: Exception) {
            null
        }
    }

    fun getAllTransactions(): List<Transaction> {
        return try {
            val transactions = mutableListOf<Transaction>()
            val db = dbHelper.readableDatabase
            val cursor: Cursor = db.query(
                DatabaseHelper.TABLE_TRANSACTIONS,
                null,
                null,
                null,
                null,
                null,
                "${DatabaseHelper.COLUMN_TRANSACTION_DATE} DESC"
            )

            with(cursor) {
                while (moveToNext()) {
                    val transaction = Transaction(
                        id = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_ID)),
                        brand = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_BRAND)),
                        productType = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_PRODUCT_TYPE)),
                        transactionType = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_TYPE)),
                        amount = getDouble(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_AMOUNT)),
                        barcode = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_BARCODE)),
                        transactionDate = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_DATE))
                    )
                    transactions.add(transaction)
                }
            }
            cursor.close()
            db.close()
            transactions
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getStockCountByProductType(productType: String): Int {
        return try {
            val db = dbHelper.readableDatabase
            
            // Stok girişlerini say
            val stockInCursor = db.rawQuery(
                "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ? AND ${DatabaseHelper.COLUMN_TRANSACTION_PRODUCT_TYPE} = ?",
                arrayOf(Transaction.TYPE_STOCK_ENTRY, productType)
            )
            
            var stockIn = 0
            if (stockInCursor.moveToFirst()) {
                stockIn = stockInCursor.getInt(0)
            }
            stockInCursor.close()
            
            // Satışları say
            val salesCursor = db.rawQuery(
                "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ? AND ${DatabaseHelper.COLUMN_TRANSACTION_PRODUCT_TYPE} = ?",
                arrayOf(Transaction.TYPE_SALE, productType)
            )
            
            var sales = 0
            if (salesCursor.moveToFirst()) {
                sales = salesCursor.getInt(0)
            }
            salesCursor.close()
            db.close()
            
            // Net stok = Giriş - Satış
            stockIn - sales
        } catch (e: Exception) {
            0
        }
    }

    fun getTotalStockCount(): Int {
        val glassCount = getStockCountByProductType(Transaction.PRODUCT_TYPE_GLASS)
        val frameCount = getStockCountByProductType(Transaction.PRODUCT_TYPE_FRAME)
        val lensCount = getStockCountByProductType(Transaction.PRODUCT_TYPE_LENS)
        return glassCount + frameCount + lensCount
    }

    fun getTransactionByBarcode(barcode: String): Transaction? {
        return try {
            val db = dbHelper.readableDatabase
            val cursor: Cursor = db.query(
                DatabaseHelper.TABLE_TRANSACTIONS,
                null,
                "${DatabaseHelper.COLUMN_TRANSACTION_BARCODE} = ?",
                arrayOf(barcode),
                null,
                null,
                "${DatabaseHelper.COLUMN_TRANSACTION_DATE} DESC",
                "1"
            )

            var transaction: Transaction? = null
            with(cursor) {
                if (moveToFirst()) {
                    transaction = Transaction(
                        id = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_ID)),
                        brand = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_BRAND)),
                        productType = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_PRODUCT_TYPE)),
                        transactionType = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_TYPE)),
                        amount = getDouble(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_AMOUNT)),
                        barcode = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_BARCODE)),
                        transactionDate = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_DATE)),
                        quantity = if (getColumnIndex(DatabaseHelper.COLUMN_TRANSACTION_QUANTITY) != -1) getInt(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_QUANTITY)) else 1,
                        purchasePrice = if (getColumnIndex(DatabaseHelper.COLUMN_TRANSACTION_PURCHASE_PRICE) != -1) getDouble(getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_PURCHASE_PRICE)) else 0.0
                    )
                }
            }
            cursor.close()
            db.close()
            transaction
        } catch (e: Exception) {
            Log.e("TransactionRepository", "Error getting transaction by barcode", e)
            null
        }
    }

    fun updateTransaction(transaction: Transaction): Boolean {
        return try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_TRANSACTION_BRAND, transaction.brand)
                put(DatabaseHelper.COLUMN_TRANSACTION_PRODUCT_TYPE, transaction.productType)
                put(DatabaseHelper.COLUMN_TRANSACTION_AMOUNT, transaction.amount)
                put(DatabaseHelper.COLUMN_TRANSACTION_PURCHASE_PRICE, transaction.purchasePrice)
                put(DatabaseHelper.COLUMN_TRANSACTION_DATE, transaction.date)
                put(DatabaseHelper.COLUMN_TRANSACTION_QUANTITY, transaction.quantity)
            }
            
            val rowsAffected = db.update(
                DatabaseHelper.TABLE_TRANSACTIONS,
                values,
                "${DatabaseHelper.COLUMN_TRANSACTION_ID} = ?",
                arrayOf(transaction.id.toString())
            )
            db.close()
            rowsAffected > 0
        } catch (e: Exception) {
            Log.e("TransactionRepository", "Error updating transaction", e)
            false
        }
    }

    fun getProfitForDateRange(startTime: Long, endTime: Long): Double {
        return try {
            val db = dbHelper.readableDatabase
            
            // Belirtilen tarih aralığındaki satışları al
            val salesCursor = db.rawQuery(
                "SELECT SUM(${DatabaseHelper.COLUMN_TRANSACTION_AMOUNT}) FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ? AND ${DatabaseHelper.COLUMN_TRANSACTION_DATE} >= ? AND ${DatabaseHelper.COLUMN_TRANSACTION_DATE} < ?",
                arrayOf(Transaction.TYPE_SALE, startTime.toString(), endTime.toString())
            )
            
            var sales = 0.0
            if (salesCursor.moveToFirst()) {
                sales = salesCursor.getDouble(0)
            }
            salesCursor.close()
            
            // Belirtilen tarih aralığındaki stok girişlerini al
            val stockCursor = db.rawQuery(
                "SELECT SUM(${DatabaseHelper.COLUMN_TRANSACTION_AMOUNT}) FROM ${DatabaseHelper.TABLE_TRANSACTIONS} WHERE ${DatabaseHelper.COLUMN_TRANSACTION_TYPE} = ? AND ${DatabaseHelper.COLUMN_TRANSACTION_DATE} >= ? AND ${DatabaseHelper.COLUMN_TRANSACTION_DATE} < ?",
                arrayOf(Transaction.TYPE_STOCK_ENTRY, startTime.toString(), endTime.toString())
            )
            
            var stockEntries = 0.0
            if (stockCursor.moveToFirst()) {
                stockEntries = stockCursor.getDouble(0)
            }
            stockCursor.close()
            db.close()
            
            sales - stockEntries
        } catch (e: Exception) {
            Log.e("TransactionRepository", "Error getting profit for date range", e)
            0.0
        }
    }
    
    fun clearAllTransactions() {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_TRANSACTIONS, null, null)
        db.close()
    }
}
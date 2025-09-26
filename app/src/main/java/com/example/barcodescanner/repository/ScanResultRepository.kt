package com.example.barcodescanner.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.barcodescanner.database.DatabaseHelper
import com.example.barcodescanner.model.ScanResult

class ScanResultRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    fun insertScanResult(scanResult: ScanResult): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_SCAN_BARCODE, scanResult.barcode)
            put(DatabaseHelper.COLUMN_SCAN_BRAND, scanResult.brand)
            put(DatabaseHelper.COLUMN_SCAN_SALE_PRICE, scanResult.salePrice)
            put(DatabaseHelper.COLUMN_SCAN_DATE, scanResult.scanDate)
        }
        
        val id = db.insert(DatabaseHelper.TABLE_SCAN_RESULTS, null, values)
        db.close()
        return id
    }

    fun getAllScanResults(): List<ScanResult> {
        val scanResults = mutableListOf<ScanResult>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_SCAN_RESULTS,
            null,
            null,
            null,
            null,
            null,
            "${DatabaseHelper.COLUMN_SCAN_DATE} DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                val scanResult = ScanResult(
                    id = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_SCAN_ID)),
                    barcode = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_SCAN_BARCODE)),
                    brand = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_SCAN_BRAND)),
                    salePrice = getDouble(getColumnIndexOrThrow(DatabaseHelper.COLUMN_SCAN_SALE_PRICE)),
                    scanDate = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_SCAN_DATE))
                )
                scanResults.add(scanResult)
            }
        }
        cursor.close()
        db.close()
        return scanResults
    }

    fun deleteScanResult(id: Long): Boolean {
        val db = dbHelper.writableDatabase
        val deletedRows = db.delete(
            DatabaseHelper.TABLE_SCAN_RESULTS,
            "${DatabaseHelper.COLUMN_SCAN_ID} = ?",
            arrayOf(id.toString())
        )
        db.close()
        return deletedRows > 0
    }

    fun getScanResultByBarcode(barcode: String): ScanResult? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_SCAN_RESULTS,
            null,
            "${DatabaseHelper.COLUMN_SCAN_BARCODE} = ?",
            arrayOf(barcode),
            null,
            null,
            "${DatabaseHelper.COLUMN_SCAN_DATE} DESC",
            "1"
        )

        var scanResult: ScanResult? = null
        with(cursor) {
            if (moveToFirst()) {
                scanResult = ScanResult(
                    id = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_SCAN_ID)),
                    barcode = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_SCAN_BARCODE)),
                    brand = getString(getColumnIndexOrThrow(DatabaseHelper.COLUMN_SCAN_BRAND)),
                    salePrice = getDouble(getColumnIndexOrThrow(DatabaseHelper.COLUMN_SCAN_SALE_PRICE)),
                    scanDate = getLong(getColumnIndexOrThrow(DatabaseHelper.COLUMN_SCAN_DATE))
                )
            }
        }
        cursor.close()
        db.close()
        return scanResult
    }
    
    fun clearAllScanResults() {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_SCAN_RESULTS, null, null)
        db.close()
    }
}